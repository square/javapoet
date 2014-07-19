package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
import dagger.internal.codegen.writer.Writable.Context;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.tools.JavaFileObject;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Writes a single compilation unit.
 */
public final class JavaWriter {
  public static JavaWriter inPackage(String packageName) {
    return new JavaWriter(packageName);
  }

  public static JavaWriter inPackage(Package enclosingPackage) {
    return new JavaWriter(enclosingPackage.getName());
  }

  public static JavaWriter inPackage(PackageElement packageElement) {
    return new JavaWriter(packageElement.getQualifiedName().toString());
  }

  private final String packageName;
  // TODO(gak): disallow multiple types in a file
  private final List<TypeWriter> typeWriters;
  private final List<ClassName> explicitImports;

  private JavaWriter(String packageName) {
    this.packageName = packageName;
    this.typeWriters = Lists.newArrayList();
    this.explicitImports = Lists.newArrayList();
  }

  public JavaWriter addImport(Class<?> importedClass) {
    explicitImports.add(ClassName.fromClass(importedClass));
    return this;
  }

  public ClassWriter addClass(String simpleName) {
    Set<Modifier> modifiers = ImmutableSet.<Modifier>of();
    checkNotNull(modifiers);
    checkNotNull(simpleName);
    checkArgument(!modifiers.contains(PROTECTED));
    checkArgument(!modifiers.contains(PRIVATE));
    checkArgument(!modifiers.contains(STATIC));
    checkNotNull(Optional.<Class<?>>absent());
    checkNotNull(ImmutableSet.<Class<?>>of());
    ClassWriter classWriter = new ClassWriter(ClassName.create(packageName, simpleName));
    typeWriters.add(classWriter);
    return classWriter;
  }

  public InterfaceWriter addInterface(String simpleName) {
    InterfaceWriter writer = new InterfaceWriter(ClassName.create(packageName, simpleName));
    typeWriters.add(writer);
    return writer;
  }

  static ImmutableSet<ClassName> collectReferencedClasses(
      Iterable<? extends HasClassReferences> iterable) {
    return FluentIterable.from(iterable)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }

  public Appendable write(Appendable appendable) throws IOException {
    appendable.append("package ").append(packageName).append(';').append("\n\n");

    // write imports
    ImmutableSet<ClassName> classNames = FluentIterable.from(typeWriters)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
    BiMap<String, ClassName> importedClassIndex = HashBiMap.create();
    // TODO(gak): check for collisions with types declared in this compilation unit too
    ImmutableSortedSet<ClassName> importCandidates = ImmutableSortedSet.<ClassName>naturalOrder()
        .addAll(explicitImports)
        .addAll(classNames)
        .build();
    ImmutableSet<ClassName> typeNames = FluentIterable.from(typeWriters)
        .transform(new Function<TypeWriter, ClassName>() {
          @Override public ClassName apply(TypeWriter input) {
            return input.name;
          }
        })
        .toSet();
    for (ClassName className : importCandidates) {
      if (!(className.packageName().equals(packageName)
              && !className.enclosingClassName().isPresent())
          && !(className.packageName().equals("java.lang")
              && className.enclosingSimpleNames().isEmpty())
          && !typeNames.contains(className.topLevelClassName())) {
        Optional<ClassName> importCandidate = Optional.of(className);
        while (importCandidate.isPresent()
            && importedClassIndex.containsKey(importCandidate.get().simpleName())) {
          importCandidate = importCandidate.get().enclosingClassName();
        }
        if (importCandidate.isPresent()) {
          appendable.append("import ").append(importCandidate.get().canonicalName()).append(";\n");
          importedClassIndex.put(importCandidate.get().simpleName(), importCandidate.get());
        }
      }
    }

    appendable.append('\n');

    CompilationUnitContext context =
        new CompilationUnitContext(packageName, ImmutableSet.copyOf(importedClassIndex.values()));

    // write types
    for (TypeWriter typeWriter : typeWriters) {
      typeWriter.write(appendable, context.createSubcontext(typeNames)).append('\n');
    }
    return appendable;
  }

  public void file(Filer filer, Iterable<? extends Element> originatingElements)
      throws IOException {
    JavaFileObject sourceFile = filer.createSourceFile(
        Iterables.getOnlyElement(typeWriters).name.canonicalName(),
        Iterables.toArray(originatingElements, Element.class));
    Closer closer = Closer.create();
    try {
      write(closer.register(sourceFile.openWriter()));
    } catch (Exception e) {
      try {
        sourceFile.delete();
      } catch (Exception e2) {
        // couldn't delete the file
      }
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  @Override
  public String toString() {
    try {
      return write(new StringBuilder()).toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }


  static final class CompilationUnitContext implements Writable.Context {
    private final String packageName;
    private final ImmutableSortedSet<ClassName> visibleClasses;

    CompilationUnitContext(String packageName, Set<ClassName> visibleClasses) {
      this.packageName = packageName;
      this.visibleClasses =
          ImmutableSortedSet.copyOf(Ordering.natural().reverse(), visibleClasses);
    }

    @Override
    public Context createSubcontext(Set<ClassName> newTypes) {
      return new CompilationUnitContext(packageName, Sets.union(visibleClasses, newTypes));
    }

    @Override
    public String sourceReferenceForClassName(ClassName className) {
      if (isImported(className)) {
        return className.simpleName();
      }
      Optional<ClassName> enclosingClassName = className.enclosingClassName();
      while (enclosingClassName.isPresent()) {
        if (isImported(enclosingClassName.get())) {
          return enclosingClassName.get().simpleName()
              + className.canonicalName()
                  .substring(enclosingClassName.get().canonicalName().length());
        }
        enclosingClassName = enclosingClassName.get().enclosingClassName();
      }
      return className.canonicalName();
    }

    private boolean isImported(ClassName className) {
      return (packageName.equals(className.packageName())
              && !className.enclosingClassName().isPresent()) // need to account for scope & hiding
          || visibleClasses.contains(className)
          || (className.packageName().equals("java.lang")
              && className.enclosingSimpleNames().isEmpty());
    }

    private static final String JAVA_IDENTIFIER_REGEX =
        "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    @Override
    public String compressTypesWithin(String snippet) {
      // TODO(gak): deal with string literals
      for (ClassName importedClass : visibleClasses) {
        snippet = snippet.replace(importedClass.canonicalName(), importedClass.simpleName());
      }
      Pattern samePackagePattern = Pattern.compile(
          packageName.replace(".", "\\.") + "\\.(" + JAVA_IDENTIFIER_REGEX + ")([^\\.])");
      Matcher matcher = samePackagePattern.matcher(snippet);
      StringBuffer buffer = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(buffer, "$1$2");
      }
      matcher.appendTail(buffer);
      return buffer.toString();
    }
  }
}
