package com.squareup.javawriter;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class AnonymousClassWriter implements Writable, HasClassReferences {
  public static AnonymousClassWriter forClassName(ClassName name) {
    return new AnonymousClassWriter(name);
  }

  public static AnonymousClassWriter forParameterizedTypeName(ParameterizedTypeName name) {
    return new AnonymousClassWriter(name);
  }

  private final TypeName supertypeOrImplementedInterface;
  private Optional<Snippet> constructorArguments;
  private final List<MethodWriter> methodWriters;
  private final Map<String, FieldWriter> fieldWriters;
  // TODO support nested types (currently, nested types must be fully-qualifiedly named)

  AnonymousClassWriter(TypeName supertypeOrImplementedInterface) {
    this.supertypeOrImplementedInterface = supertypeOrImplementedInterface;
    this.constructorArguments = Optional.absent();
    this.methodWriters = Lists.newArrayList();
    this.fieldWriters = Maps.newLinkedHashMap();
  }

  public void setConstructorArguments(Snippet parameters) {
    constructorArguments = Optional.of(parameters);
  }

  public void setConstructorArguments(String parameters, Object... args) {
    setConstructorArguments(Snippet.format(parameters, args));
  }

  public MethodWriter addMethod(TypeWriter returnType, String name) {
    MethodWriter methodWriter = new MethodWriter(returnType.name, name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(TypeMirror returnType, String name) {
    MethodWriter methodWriter =
        new MethodWriter(TypeNames.forTypeMirror(returnType), name);
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
        new MethodWriter(TypeNames.forClass(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public FieldWriter addField(Class<?> type, String name) {
    return addField(TypeNames.forClass(type), name);
  }

  public FieldWriter addField(TypeElement type, String name) {
    return addField(ClassName.fromTypeElement(type), name);
  }

  public FieldWriter addField(TypeName type, String name) {
    String candidateName = name;
    int differentiator = 1;
    while (fieldWriters.containsKey(candidateName)) {
      candidateName = name + differentiator;
      differentiator++;
    }
    FieldWriter fieldWriter = new FieldWriter(type, candidateName);
    fieldWriters.put(candidateName, fieldWriter);
    return fieldWriter;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(ImmutableList.of(supertypeOrImplementedInterface), methodWriters,
            fieldWriters.values());
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    appendable.append("new ");
    supertypeOrImplementedInterface.write(appendable, context);
    appendable.append('(');
    if (constructorArguments.isPresent()) {
      constructorArguments.get().write(appendable, context);
    }
    appendable.append(") {");
    if (!fieldWriters.isEmpty()) {
      appendable.append('\n');
    }
    for (VariableWriter fieldWriter : fieldWriters.values()) {
      fieldWriter.write(new IndentingAppendable(appendable), context).append("\n");
    }
    for (MethodWriter methodWriter : methodWriters) {
      appendable.append('\n');
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append("}");
    return appendable;
  }
}
