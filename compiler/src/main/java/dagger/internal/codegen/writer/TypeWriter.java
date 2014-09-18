package dagger.internal.codegen.writer;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
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

  TypeWriter(ClassName name) {
    this.name = name;
    this.supertype = Optional.absent();
    this.implementedTypes = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    nestedTypeWriters = Lists.newArrayList();
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
}
