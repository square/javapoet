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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public final class AnonymousClassWriter implements Writable, HasClassReferences {
  public static AnonymousClassWriter forClassName(ClassName name) {
    return new AnonymousClassWriter(name);
  }

  public static AnonymousClassWriter forParameterizedTypeName(ParameterizedTypeName name) {
    return new AnonymousClassWriter(name);
  }

  private final TypeName supertypeOrImplementedInterface;
  private Optional<Snippet> constructorArguments;
  private final ClassBodyWriter body;
  // TODO support nested types (currently, nested types must be fully-qualifiedly named)

  AnonymousClassWriter(TypeName supertypeOrImplementedInterface) {
    this.supertypeOrImplementedInterface = supertypeOrImplementedInterface;
    this.constructorArguments = Optional.absent();
    this.body = new ClassBodyWriter();
  }

  public void setConstructorArguments(Snippet parameters) {
    constructorArguments = Optional.of(parameters);
  }

  public void setConstructorArguments(String parameters, Object... args) {
    setConstructorArguments(Snippet.format(parameters, args));
  }

  public MethodWriter addMethod(TypeWriter returnType, String name) {
    return body.addMethod(returnType, name);
  }

  public MethodWriter addMethod(TypeMirror returnType, String name) {
    return body.addMethod(returnType, name);
  }

  public MethodWriter addMethod(TypeName returnType, String name) {
    return body.addMethod(returnType, name);
  }

  public MethodWriter addMethod(Class<?> returnType, String name) {
    return body.addMethod(returnType, name);
  }

  public FieldWriter addField(Class<?> type, String name) {
    return body.addField(type, name);
  }

  public FieldWriter addField(TypeElement type, String name) {
    return body.addField(type, name);
  }

  public FieldWriter addField(TypeName type, String name) {
    return body.addField(type, name);
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(ImmutableList.of(supertypeOrImplementedInterface, body))
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
    body.writeFields(appendable, context);
    body.writeMethods(appendable, context);
    appendable.append('}');
    return appendable;
  }
}
