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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public final class InterfaceWriter extends TypeWriter {
  public static InterfaceWriter forClassName(ClassName name) {
    checkArgument(name.enclosingSimpleNames().isEmpty(), "%s must be top-level type.", name);
    return new InterfaceWriter(name);
  }

  private final List<TypeVariableName> typeVariables;
  InterfaceWriter(ClassName name) {
    super(name);
    this.typeVariables = Lists.newArrayList();
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    context = context.createSubcontext(FluentIterable.from(nestedTypeWriters)
        .transform(new Function<TypeWriter, ClassName>() {
          @Override public ClassName apply(TypeWriter input) {
            return input.name;
          }
        })
        .toSet());
    writeAnnotations(appendable, context);
    writeModifiers(appendable).append("interface ").append(name.simpleName());
    Writables.Joiner.on(", ").wrap("<", "> ").appendTo(appendable, context, typeVariables);
    Writables.Joiner.on(", ").prefix(" extends ").appendTo(appendable, context, implementedTypes);
    appendable.append(" {");
    for (MethodWriter methodWriter : methodWriters) {
      appendable.append('\n');
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      appendable.append('\n');
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append("}\n");
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(nestedTypeWriters, methodWriters, implementedTypes, typeVariables,
            annotations);
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
