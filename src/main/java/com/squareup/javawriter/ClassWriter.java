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

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.squareup.javawriter.Writables.writeToString;

public final class ClassWriter extends TypeWriter {
  public static ClassWriter forClassName(ClassName name) {
    checkArgument(name.enclosingSimpleNames().isEmpty(), "%s must be top-level type.", name);
    return new ClassWriter(name);
  }

  private Optional<TypeName> supertype;
  private final List<TypeVariableName> typeVariables;

  ClassWriter(ClassName className) {
    super(className);
    this.supertype = Optional.absent();
    this.typeVariables = Lists.newArrayList();
  }

  public void setSupertype(TypeName typeName) {
    if (supertype.isPresent()) {
      throw new IllegalStateException("Supertype already set to " + writeToString(supertype.get()));
    }
    supertype = Optional.of(typeName);
  }

  public void setSupertype(TypeElement typeElement) {
    setSupertype(ClassName.fromTypeElement(typeElement));
  }

  public ConstructorWriter addConstructor() {
    return body.addConstructor();
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    context = createSubcontext(context);
    writeAnnotations(appendable, context);
    writeModifiers(appendable).append("class ").append(name.simpleName());
    Writables.Joiner.on(", ").wrap("<", "> ").appendTo(appendable, context, typeVariables);
    if (supertype.isPresent()) {
      appendable.append(" extends ");
      supertype.get().write(appendable, context);
    }
    Writables.Joiner.on(", ").prefix(" implements ")
        .appendTo(appendable, context, implementedTypes);
    appendable.append(" {");
    body.write(appendable, context);
    appendable.append("}\n");
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), supertype.asSet(), typeVariables);
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }
}
