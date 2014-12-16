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
package com.squareup.javawriter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.google.common.base.Preconditions.checkArgument;

public final class MethodWriter extends Modifiable implements Writable {
  private final List<TypeVariableName> typeVariables;
  private final TypeName returnType;
  private final String name;
  private final Map<String, ParameterWriter> parameterWriters;
  private final List<ClassName> throwsTypes;
  private BlockWriter body;

  MethodWriter(TypeName returnType, String name) {
    this.typeVariables = Lists.newArrayList();
    this.returnType = returnType;
    this.name = name;
    this.parameterWriters = Maps.newLinkedHashMap();
    this.throwsTypes = Lists.newArrayList();
    this.body = new BlockWriter();
  }

  public String name() {
    return name;
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  public ParameterWriter addParameter(Class<?> type, String name) {
    return addParameter(TypeNames.forClass(type), name);
  }

  public ParameterWriter addParameter(TypeElement type, String name) {
    return addParameter(ClassName.fromTypeElement(type), name);
  }

  public ParameterWriter addParameter(TypeWriter type, String name) {
    return addParameter(type.name, name);
  }

  public ParameterWriter addParameter(TypeName type, String name) {
    checkArgument(!parameterWriters.containsKey(name));
    ParameterWriter parameterWriter = new ParameterWriter(type, name);
    parameterWriters.put(name, parameterWriter);
    return parameterWriter;
  }

  public void addThrowsType(Class<?> clazz) {
    addThrowsType(ClassName.fromClass(clazz));
  }

  public void addThrowsType(ClassName throwsType) {
    throwsTypes.add(throwsType);
  }

  public BlockWriter body() {
    return body;
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    writeAnnotations(appendable, context, '\n');
    writeModifiers(appendable);
    Writables.Joiner.on(", ").wrap("<", "> ").appendTo(appendable, context, typeVariables);
    returnType.write(appendable, context);
    appendable.append(' ').append(name).append('(');
    Writables.Joiner.on(", ").appendTo(appendable, context, parameterWriters.values());
    appendable.append(")");
    Writables.Joiner.on(", ").prefix(" throws ").appendTo(appendable, context, throwsTypes);
    if (modifiers.contains(Modifier.ABSTRACT)) {
      appendable.append(";\n");
    } else {
      appendable.append(" {\n");
      if (!body.isEmpty()) {
        body.write(new IndentingAppendable(appendable), context).append('\n');
      }
      appendable.append("}\n");
    }
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), typeVariables,
            ImmutableList.of(returnType, body), parameterWriters.values(), throwsTypes);
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }
}
