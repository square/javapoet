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
import javax.lang.model.element.TypeElement;

import static com.google.common.base.Preconditions.checkArgument;

public final class ConstructorWriter extends Modifiable implements Writable {
  private final List<TypeVariableName> typeVariables;
  private final String name;
  private final Map<String, VariableWriter> parameterWriters;
  private final BlockWriter body;

  ConstructorWriter(String name) {
    this.typeVariables = Lists.newArrayList();
    this.name = name;
    this.parameterWriters = Maps.newLinkedHashMap();
    this.body = new BlockWriter();
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  public VariableWriter addParameter(Class<?> type, String name) {
    return addParameter(TypeNames.forClass(type), name);
  }

  public VariableWriter addParameter(TypeElement type, String name) {
    return addParameter(ClassName.fromTypeElement(type), name);
  }

  public VariableWriter addParameter(TypeWriter type, String name) {
    return addParameter(type.name, name);
  }

  public VariableWriter addParameter(TypeName type, String name) {
    VariableWriter parameterWriter = new VariableWriter(type, name);
    parameterWriters.put(name, parameterWriter);
    return parameterWriter;
  }

  public BlockWriter body() {
    return body;
  }

  private VariableWriter addParameter(ClassName type, String name) {
    checkArgument(!parameterWriters.containsKey(name));
    VariableWriter parameterWriter = new VariableWriter(type, name);
    parameterWriters.put(name, parameterWriter);
    return parameterWriter;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), typeVariables, parameterWriters.values(),
            ImmutableList.of(body));
    return FluentIterable.from(concat)
            .transformAndConcat(GET_REFERENCED_CLASSES)
            .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    writeModifiers(appendable);
    Writables.Joiner.on(", ").wrap("<", "> ").appendTo(appendable, context, typeVariables);
    appendable.append(name).append('(');
    Writables.Joiner.on(", ").appendTo(appendable, context, parameterWriters.values());
    appendable.append(") {\n");
    if (!body.isEmpty()) {
      body.write(new IndentingAppendable(appendable), context).append('\n');
    }
    return appendable.append("}\n");
  }
}
