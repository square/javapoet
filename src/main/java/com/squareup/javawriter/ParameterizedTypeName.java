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

import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

public final class ParameterizedTypeName extends TypeName {
  private final ClassName type;
  private final ImmutableList<? extends TypeName> parameters;

  ParameterizedTypeName(ClassName type, Iterable<? extends TypeName> parameters) {
    this.type = type;
    this.parameters = ImmutableList.copyOf(parameters);
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat
        = Iterables.concat(super.referencedClasses(), parameters, ImmutableList.of(type));
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    verify(!parameters.isEmpty(), type.toString());
    super.write(appendable, context);
    appendable.append(context.sourceReferenceForClassName(type));
    Writables.Joiner.on(", ").wrap("<", ">").appendTo(appendable, context, parameters);
    return appendable;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (super.equals(obj) && obj instanceof ParameterizedTypeName) {
      ParameterizedTypeName that = (ParameterizedTypeName) obj;
      return this.type.equals(that.type)
          && this.parameters.equals(that.parameters);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), type, parameters);
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }

  public static ParameterizedTypeName create(ClassName className,
      TypeName... parameters) {
    return new ParameterizedTypeName(className, ImmutableList.copyOf(parameters));
  }

  public static ParameterizedTypeName create(Class<?> parameterizedClass,
      TypeName... parameters) {
    checkArgument(parameterizedClass.getTypeParameters().length == parameters.length);
    return new ParameterizedTypeName(ClassName.fromClass(parameterizedClass),
        ImmutableList.copyOf(parameters));
  }
}
