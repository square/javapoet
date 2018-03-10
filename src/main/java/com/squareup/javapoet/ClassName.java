/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

/** A fully-qualified class name for top-level and member classes. */
public abstract class ClassName extends TypeName implements Comparable<ClassName> {
  final String simpleName;
  String canonicalName;

  /** A fully-qualified class name for top-level classes. */
  private static final class TopLevelClassName extends ClassName {
    final String packageName;

    private TopLevelClassName(String packageName, String simpleName) {
      this(packageName, simpleName, new ArrayList<>());
    }

    private TopLevelClassName(
        String packageName, String simpleName, List<AnnotationSpec> annotations) {
      super(simpleName, annotations);
      this.packageName = packageName == null ? "" : packageName;
      this.canonicalName = isDefaultPackage(packageName)
          ? simpleName : String.join(".", Arrays.asList(packageName, simpleName));
      checkArgument(
          isDefaultPackage(simpleName) || SourceVersion.isName(simpleName),
          "part '%s' is keyword", simpleName);
    }

    @Override public TopLevelClassName annotated(List<AnnotationSpec> annotations) {
      return new TopLevelClassName(packageName, simpleName, concatAnnotations(annotations));
    }

    @Override public TopLevelClassName withoutAnnotations() {
      return new TopLevelClassName(packageName, simpleName);
    }

    public String packageName() {
      return packageName;
    }

    @Override
    public ClassName enclosingClassName() {
      return null;
    }

    @Override
    public TopLevelClassName topLevelClassName() {
      return this;
    }

    @Override
    public String reflectionName() {
      return isDefaultPackage(packageName)
          ? simpleName
          : String.join(".", Arrays.asList(packageName, simpleName));
    }

    @Override
    public List<String> simpleNames() {
      return Arrays.asList(simpleName);
    }

    @Override
    public ClassName peerClass(String name) {
      return new TopLevelClassName(packageName, name);
    }

    @Override
    ClassName prefixWithAtMostOneAnnotatedClass() {
      return this;
    }

    @Override
    boolean hasAnnotatedEnclosingClass() {
      return false;
    }

    @Override
    CodeWriter emitWithoutPrefix(CodeWriter out, ClassName unannotatedPrefix) {
      return out;
    }
  }

  /** A fully-qualified class name for nested classes. */
  private static final class NestedClassName extends ClassName {
    /** From top to bottom. This will be ["java.util", "Map", "Entry"] for {@link Map.Entry}. */
    final ClassName enclosingClassName;

    private NestedClassName(ClassName enclosingClassName, String simpleName) {
      this(enclosingClassName, simpleName, new ArrayList<>());
    }

    private NestedClassName(
        ClassName enclosingClassName, String simpleName, List<AnnotationSpec> annotations) {
      super(simpleName, annotations);
      this.enclosingClassName = enclosingClassName;
      this.canonicalName =
          String.join(".", Arrays.asList(enclosingClassName.canonicalName, simpleName));
    }

    @Override public NestedClassName annotated(List<AnnotationSpec> annotations) {
      return new NestedClassName(enclosingClassName, simpleName, concatAnnotations(annotations));
    }

    @Override public NestedClassName withoutAnnotations() {
      return new NestedClassName(enclosingClassName.withoutAnnotations(), simpleName);
    }

    /** Returns the package name, like {@code "java.util"} for {@code Map.Entry}. */
    public String packageName() {
      return enclosingClassName.packageName();
    }

    @Override
    public ClassName enclosingClassName() {
      return enclosingClassName;
    }

    @Override
    public ClassName topLevelClassName() {
      return enclosingClassName.topLevelClassName();
    }

    @Override
    public String reflectionName() {
      return enclosingClassName.reflectionName() + "$" + simpleName;
    }

    @Override
    public List<String> simpleNames() {
      List<String> simpleNames = new ArrayList<>(enclosingClassName().simpleNames());
      simpleNames.add(simpleName);
      return simpleNames;
    }

    @Override
    public ClassName peerClass(String name) {
      return enclosingClassName.nestedClass(name);
    }

    @Override
    ClassName prefixWithAtMostOneAnnotatedClass() {
      if (hasAnnotatedEnclosingClass()) {
        enclosingClassName.prefixWithAtMostOneAnnotatedClass();
      }

      return this;
    }

    @Override
    CodeWriter emitWithoutPrefix(
        CodeWriter out, ClassName unannotatedPrefix) throws IOException {

      if (unannotatedPrefix.equals(this)) {
        return out;
      }

      enclosingClassName.emitWithoutPrefix(out, unannotatedPrefix);
      out.emit(".");
      if (isAnnotated()) {
        out.emit(" ");
        emitAnnotations(out);
      }
      return out.emit(simpleName);
    }

    @Override
    boolean hasAnnotatedEnclosingClass() {
      return enclosingClassName.isAnnotated() || enclosingClassName.hasAnnotatedEnclosingClass();
    }
  }

  public static final ClassName OBJECT = ClassName.get(Object.class);

  private ClassName(String simpleName, List<AnnotationSpec> annotations) {
    super(annotations);
    checkArgument(SourceVersion.isName(simpleName), "part '%s' is keyword", simpleName);
    this.simpleName = simpleName;
  }


  /** Returns the package name, like {@code "java.util"} for {@code Map.Entry}. */
  public abstract String packageName();

  /**
   * Returns the enclosing class, like {@link Map} for {@code Map.Entry}. Returns null if this class
   * is not nested in another class.
   */
  public abstract ClassName enclosingClassName();

  /**
   * Returns the top class in this nesting group. Equivalent to chained calls to {@link
   * #enclosingClassName()} until the result's enclosing class is null.
   */
  public abstract ClassName topLevelClassName();

  /**
   * Return the binary name of a class.
   */
  public abstract String reflectionName();

  public abstract ClassName withoutAnnotations();

  /**
   * Returns a new {@link ClassName} instance for the specified {@code name} as nested inside this
   * class.
   */
  public ClassName nestedClass(String name) {
    return new NestedClassName(this, name);
  }

  @Override
  public ClassName annotated(List<AnnotationSpec> annotations) {
    return (ClassName) super.annotated(annotations);
  }

  public abstract List<String> simpleNames();

  /**
   * Returns a class that shares the same enclosing package or class. If this class is enclosed by
   * another class, this is equivalent to {@code enclosingClassName().nestedClass(name)}. Otherwise
   * it is equivalent to {@code get(packageName(), name)}.
   */
  public abstract ClassName peerClass(String name);

  abstract ClassName prefixWithAtMostOneAnnotatedClass();

  abstract boolean hasAnnotatedEnclosingClass();

  abstract CodeWriter emitWithoutPrefix(
      CodeWriter out, ClassName unannotatedPrefix) throws IOException;

  /** Returns the simple name of this class, like {@code "Entry"} for {@link Map.Entry}. */
  public String simpleName() {
    return simpleName;
  }

  public static ClassName get(Class<?> clazz) {
    checkNotNull(clazz, "clazz == null");
    checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
    checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
    checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");

    String anonymousSuffix = "";
    while (clazz.isAnonymousClass()) {
      int lastDollar = clazz.getName().lastIndexOf('$');
      anonymousSuffix = clazz.getName().substring(lastDollar) + anonymousSuffix;
      clazz = clazz.getEnclosingClass();
    }
    String name = clazz.getSimpleName() + anonymousSuffix;

    if (clazz.getEnclosingClass() == null) {
      // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
      int lastDot = clazz.getName().lastIndexOf('.');
      String packageName = (lastDot != -1)  ? clazz.getName().substring(0, lastDot) : null;
      return new TopLevelClassName(packageName, name);
    }

    return ClassName.get(clazz.getEnclosingClass()).nestedClass(name);
  }

  /**
   * Returns a new {@link ClassName} instance for the given fully-qualified class name string. This
   * method assumes that the input is ASCII and follows typical Java style (lowercase package
   * names, UpperCamelCase class names) and may produce incorrect results or throw
   * {@link IllegalArgumentException} otherwise. For that reason, {@link #get(Class)} and
   * {@link #get(Class)} should be preferred as they can correctly create {@link ClassName}
   * instances without such restrictions.
   */
  public static ClassName bestGuess(String classNameString) {
    List<String> names = new ArrayList<>();

    // Add the package name, like "java.util.concurrent", or "" for no package.
    int p = 0;
    while (p < classNameString.length() && Character.isLowerCase(classNameString.codePointAt(p))) {
      p = classNameString.indexOf('.', p) + 1;
      checkArgument(p != 0, "couldn't make a guess for %s", classNameString);
    }
    String packageName = p == 0 ? null : classNameString.substring(0, p - 1);
    String[] classNames = classNameString.substring(p).split("\\.", -1);

    checkArgument(classNames.length >= 1, "couldn't make a guess for %s", classNameString);

    String simpleName = classNames[0];
    checkArgument(!simpleName.isEmpty() && Character.isUpperCase(simpleName.codePointAt(0)),
        "couldn't make a guess for %s", classNameString);
    ClassName className = new TopLevelClassName(packageName, simpleName);

    // Add the class names, like "Map" and "Entry".
    for (String part : Arrays.asList(classNames).subList(1, classNames.length)) {
      checkArgument(!part.isEmpty() && Character.isUpperCase(part.codePointAt(0)),
          "couldn't make a guess for %s", classNameString);
      className = className.nestedClass(part);
    }

    return className;
  }

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * {@code "java.util"} and simple names {@code "Map"}, {@code "Entry"} yields {@link Map.Entry}.
   */
  public static ClassName get(String packageName, String simpleName, String... simpleNames) {
    ClassName className = new TopLevelClassName(packageName, simpleName);
    for (String name : simpleNames) {
      className = className.nestedClass(name);
    }
    return className;
  }

  /** Returns the class name for {@code element}. */
  public static ClassName get(TypeElement element) {
    checkNotNull(element, "element == null");
    checkArgument(element.getNestingKind() == TOP_LEVEL || element.getNestingKind() == MEMBER,
        "unexpected type nesting");
    String simpleName = element.getSimpleName().toString();

    if (isClassOrInterface(element.getEnclosingElement())) {
      return ClassName.get((TypeElement) element.getEnclosingElement()).nestedClass(simpleName);
    }

    String packageName = getPackage(element.getEnclosingElement()).getQualifiedName().toString();
    return new TopLevelClassName(packageName, simpleName);
  }

  private static boolean isClassOrInterface(Element e) {
    return e.getKind().isClass() || e.getKind().isInterface();
  }

  private static PackageElement getPackage(Element type) {
    while (type.getKind() != ElementKind.PACKAGE) {
      type = type.getEnclosingElement();
    }
    return (PackageElement) type;
  }

  @Override public int compareTo(ClassName o) {
    return reflectionName().compareTo(o.reflectionName());
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    ClassName prefix = prefixWithAtMostOneAnnotatedClass();
    String unqualifiedName = out.lookupName(prefix);
    if (prefix.isAnnotated())  {
      int dot = unqualifiedName.lastIndexOf(".");
      out.emitAndIndent(unqualifiedName.substring(0, dot + 1));
      if (dot != -1) {
        out.emit(" ");
      }
      prefix.emitAnnotations(out);
      out.emit(unqualifiedName.substring(dot + 1));
    } else {
        out.emitAndIndent(unqualifiedName);
    }
    return emitWithoutPrefix(out, prefix);
  }

  private static boolean isDefaultPackage(String packageName) {
    return packageName == null || packageName.isEmpty();
  }
}
