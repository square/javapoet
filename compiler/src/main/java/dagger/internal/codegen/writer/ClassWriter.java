package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

public final class ClassWriter extends TypeWriter {
  private final List<TypeWriter> nestedTypeWriters;
  private final List<FieldWriter> fieldWriters;
  private final List<ConstructorWriter> constructorWriters;
  private final List<MethodWriter> methodWriters;
  private final List<TypeVariableName> typeVariables;

  ClassWriter(ClassName className) {
    super(className);
    this.nestedTypeWriters = Lists.newArrayList();
    this.fieldWriters = Lists.newArrayList();
    this.constructorWriters = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    this.typeVariables = Lists.newArrayList();
  }

  public void addImplementedType(TypeName typeReference) {
    implementedTypes.add(typeReference);
  }

  public void addImplementedType(TypeElement typeElement) {
    implementedTypes.add(ClassName.fromTypeElement(typeElement));
  }

  public FieldWriter addField(Class<?> type, String name) {
    FieldWriter fieldWriter = new FieldWriter(ClassName.fromClass(type), name);
    fieldWriters.add(fieldWriter);
    return fieldWriter;
  }

  public FieldWriter addField(TypeElement type, String name) {
    FieldWriter fieldWriter = new FieldWriter(ClassName.fromTypeElement(type), name);
    fieldWriters.add(fieldWriter);
    return fieldWriter;
  }

  public FieldWriter addField(TypeName type, String name) {
    FieldWriter fieldWriter = new FieldWriter(type, name);
    fieldWriters.add(fieldWriter);
    return fieldWriter;
  }

  public ConstructorWriter addConstructor() {
    ConstructorWriter constructorWriter = new ConstructorWriter(name.simpleName());
    constructorWriters.add(constructorWriter);
    return constructorWriter;
  }

  public ClassWriter addNestedClass(String name) {
    ClassWriter innerClassWriter = new ClassWriter(this.name.nestedClassNamed(name));
    nestedTypeWriters.add(innerClassWriter);
    return innerClassWriter;
  }

  public MethodWriter addMethod(TypeWriter returnType, String name) {
    MethodWriter methodWriter = new MethodWriter(returnType.name, name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(TypeMirror returnType, String name) {
    MethodWriter methodWriter =
        new MethodWriter(TypeReferences.forTypeMirror(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(TypeName returnType, String name) {
    MethodWriter methodWriter = new MethodWriter(returnType, name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(Class<?> returnType, String name) {
    MethodWriter methodWriter =
        new MethodWriter(ClassName.fromClass(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    writeAnnotations(appendable, context);
    writeModifiers(appendable).append("class ").append(name.simpleName());
    if (!typeVariables.isEmpty()) {
      appendable.append('<');
      Joiner.on(", ").appendTo(appendable, typeVariables);
      appendable.append('>');
    }
    if (supertype.isPresent()) {
      appendable.append(" extends ");
      supertype.get().write(appendable, context);
    }
    Iterator<TypeName> implementedTypesIterator = implementedTypes.iterator();
    if (implementedTypesIterator.hasNext()) {
      appendable.append(" implements ");
      implementedTypesIterator.next().write(appendable, context);
      while (implementedTypesIterator.hasNext()) {
        appendable.append(", ");
        implementedTypesIterator.next().write(appendable, context);
      }
    }
    appendable.append(" {\n");
    for (VariableWriter fieldWriter : fieldWriters) {
      fieldWriter.write(new IndentingAppendable(appendable), context).append("\n");
    }
    appendable.append('\n');
    for (ConstructorWriter constructorWriter : constructorWriters) {
      if (!isDefaultConstructor(constructorWriter)) {
        constructorWriter.write(new IndentingAppendable(appendable), context);
      }
    }
    appendable.append('\n');
    for (MethodWriter methodWriter : methodWriters) {
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append('\n');
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append("}\n");
    return appendable;
  }

  private static final Set<Modifier> VISIBILIY_MODIFIERS =
      Sets.immutableEnumSet(PUBLIC, PROTECTED, PRIVATE);

  private boolean isDefaultConstructor(ConstructorWriter constructorWriter) {
    return Sets.intersection(VISIBILIY_MODIFIERS, modifiers)
        .equals(Sets.intersection(VISIBILIY_MODIFIERS, constructorWriter.modifiers))
        && constructorWriter.body().isEmpty();
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(nestedTypeWriters, fieldWriters, constructorWriters, methodWriters,
            implementedTypes, supertype.asSet(), annotations);
    return FluentIterable.from(concat)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }
}
