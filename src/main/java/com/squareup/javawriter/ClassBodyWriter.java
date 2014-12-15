/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.javawriter;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.google.common.base.Preconditions.checkState;

final class ClassBodyWriter implements Writable, HasClassReferences {

  static ClassBodyWriter forAnonymousType() {
    return new ClassBodyWriter(Optional.<ClassName>absent());
  }

  static ClassBodyWriter forNamedType(ClassName name) {
    return new ClassBodyWriter(Optional.of(name));
  }

  private final Optional<ClassName> name;
  private final Map<String, FieldWriter> fieldWriters;
  private final List<ConstructorWriter> constructorWriters;
  private final List<MethodWriter> methodWriters;
  final List<TypeWriter> nestedTypeWriters;

  private ClassBodyWriter(Optional<ClassName> name) {
    this.name = name;
    this.fieldWriters = Maps.newLinkedHashMap();
    this.constructorWriters = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    this.nestedTypeWriters = Lists.newArrayList();
  }

  public boolean isEmpty() {
    return fieldWriters.isEmpty() && constructorWriters.isEmpty() && methodWriters.isEmpty()
        && nestedTypeWriters.isEmpty();
  }

  public ConstructorWriter addConstructor() {
    checkState(name.isPresent(), "Cannot add a constructor to an anonymous type");
    ConstructorWriter constructorWriter = new ConstructorWriter(name.get().simpleName());
    constructorWriters.add(constructorWriter);
    return constructorWriter;
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

  public ClassWriter addNestedClass(String name) {
    // TODO support nested types in anonymous types
    // (currently, nested types must be fully-qualifiedly named)
    checkState(this.name.isPresent(), "Nested types not yet supported in anonymous types");
    ClassWriter innerClassWriter = new ClassWriter(this.name.get().nestedClassNamed(name));
    nestedTypeWriters.add(innerClassWriter);
    return innerClassWriter;
  }

  public InterfaceWriter addNestedInterface(String name) {
    // TODO support nested types in anonymous types
    // (currently, nested types must be fully-qualifiedly named)
    checkState(this.name.isPresent(), "Nested types not yet supported in anonymous types");
    InterfaceWriter innerInterfaceWriter =
        new InterfaceWriter(this.name.get().nestedClassNamed(name));
    nestedTypeWriters.add(innerInterfaceWriter);
    return innerInterfaceWriter;
  }

  public EnumWriter addNestedEnum(String name) {
    // TODO support nested types in anonymous types
    // (currently, nested types must be fully-qualifiedly named)
    checkState(this.name.isPresent(), "Nested types not yet supported in anonymous types");
    EnumWriter innerEnumWriter = new EnumWriter(this.name.get().nestedClassNamed(name));
    nestedTypeWriters.add(innerEnumWriter);
    return innerEnumWriter;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(fieldWriters.values(), constructorWriters, methodWriters,
            nestedTypeWriters);
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    if (!fieldWriters.isEmpty()) {
      appendable.append('\n');
    }
    for (VariableWriter fieldWriter : fieldWriters.values()) {
      fieldWriter.write(new IndentingAppendable(appendable), context).append('\n');
    }
    for (ConstructorWriter constructorWriter : constructorWriters) {
      appendable.append('\n');
      constructorWriter.write(new IndentingAppendable(appendable), context);
    }
    for (MethodWriter methodWriter : methodWriters) {
      appendable.append('\n');
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      appendable.append('\n');
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    return appendable;
  }
}
