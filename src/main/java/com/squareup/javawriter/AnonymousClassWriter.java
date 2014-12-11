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
import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class AnonymousClassWriter extends TypeWriter {
  public static AnonymousClassWriter forClassName(ClassName name) {
    return new AnonymousClassWriter(name);
  }

  private Optional<Snippet> arguments;
  private final List<TypeName> typeVariables;

  AnonymousClassWriter(ClassName className) {
    super(className);
    arguments = Optional.absent();
    typeVariables = Lists.newArrayList();
  }

  public void setConstructorArguments(Snippet parameters) {
    arguments = Optional.of(parameters);
  }

  public void setConstructorArguments(String parameters, Object... args) {
    setConstructorArguments(Snippet.format(parameters, args));
  }

  public void addTypeVariable(TypeName typeName) {
    typeVariables.add(typeName);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    context = createSubcontext(context);
    appendable.append("new ").append(name.simpleName());
    Writables.Joiner.on(", ")
        .wrap("<", ">")
        .appendTo(appendable, context, typeVariables)
        .append('(');
    if (arguments.isPresent()) {
      arguments.get().write(appendable, context);
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
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      appendable.append('\n');
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append('}');
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), arguments.asSet(), typeVariables);
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }
}
