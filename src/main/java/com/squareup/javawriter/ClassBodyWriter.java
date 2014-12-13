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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javawriter.Writable.Context;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class ClassBodyWriter implements HasClassReferences {
  private final Map<String, FieldWriter> fieldWriters;
  private final List<MethodWriter> methodWriters;

  ClassBodyWriter() {
    this.fieldWriters = Maps.newLinkedHashMap();
    this.methodWriters = Lists.newArrayList();
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
        Iterables.concat(fieldWriters.values(), methodWriters);
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  public Appendable writeFields(Appendable appendable, Context context) throws IOException {
    if (!fieldWriters.isEmpty()) {
      appendable.append('\n');
    }
    for (VariableWriter fieldWriter : fieldWriters.values()) {
      fieldWriter.write(new IndentingAppendable(appendable), context).append('\n');
    }
    return appendable;
  }

  public Appendable writeMethods(Appendable appendable, Context context) throws IOException {
    for (MethodWriter methodWriter : methodWriters) {
      appendable.append('\n');
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    return appendable;
  }
}
