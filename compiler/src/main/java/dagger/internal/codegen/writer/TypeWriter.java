package dagger.internal.codegen.writer;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Only named types. Doesn't cover anonymous inner classes.
 */
public abstract class TypeWriter /* ha ha */ extends Modifiable
    implements Writable, HasTypeName, HasClassReferences {
  final ClassName name;
  Optional<TypeName> supertype;
  final List<TypeName> implementedTypes;
  final List<MethodWriter> methodWriters;
  final List<TypeWriter> nestedTypeWriters;
  final Map<String, FieldWriter> fieldWriters;

  TypeWriter(ClassName name) {
    this.name = name;
    this.supertype = Optional.absent();
    this.implementedTypes = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    this.nestedTypeWriters = Lists.newArrayList();
    this.fieldWriters = Maps.newLinkedHashMap();
  }

  @Override
  public ClassName name() {
    return name;
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
        new MethodWriter(ClassName.fromClass(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public ClassWriter addNestedClass(String name) {
    ClassWriter innerClassWriter = new ClassWriter(this.name.nestedClassNamed(name));
    nestedTypeWriters.add(innerClassWriter);
    return innerClassWriter;
  }

  public void addImplementedType(TypeName typeReference) {
    implementedTypes.add(typeReference);
  }

  public void addImplementedType(TypeElement typeElement) {
    implementedTypes.add(ClassName.fromTypeElement(typeElement));
  }

  public FieldWriter addField(Class<?> type, String name) {
    return addField(ClassName.fromClass(type), name);
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
}
