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
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Set;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import static com.squareup.javawriter.TypeNames.FOR_TYPE_MIRROR;

public final class TypeVariableName implements TypeName {
  private final String name;
  private final ImmutableList<TypeName> bounds;

  TypeVariableName(String name, Iterable<TypeName> bounds) {
    this.name = name;
    this.bounds = FluentIterable.from(bounds)
        .filter(Predicates.not(Predicates.<TypeName>equalTo(ClassName.fromClass(Object.class))))
        .toList();
  }

  static TypeVariableName named(String name) {
    return new TypeVariableName(name, ImmutableList.<TypeName>of());
  }

  static TypeVariableName forTypeMirror(TypeVariable mirror) {
    FluentIterable<TypeMirror> bounds =
        FluentIterable.from(ImmutableList.of(mirror.getUpperBound()));
    if (mirror.getUpperBound().getKind() == TypeKind.DECLARED) {
      TypeElement bound = (TypeElement) ((DeclaredType) mirror.getUpperBound()).asElement();
      if (bound.getNestingKind() == NestingKind.ANONYMOUS) {
        // This is (likely) an intersection type.
        bounds = FluentIterable
            .from(ImmutableList.of(bound.getSuperclass()))
            .append(bound.getInterfaces());
      }
    }
    return new TypeVariableName(
        mirror.asElement().getSimpleName().toString(),
        bounds.transform(FOR_TYPE_MIRROR));
  }

  public String name() {
    return name;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(bounds)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    appendable.append(name);
    Writables.Joiner.on(" & ").prefix(" extends ").appendTo(appendable, context, bounds);
    return appendable;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TypeVariableName) {
      TypeVariableName that = (TypeVariableName) obj;
      return this.name.equals(that.name)
          && this.bounds.equals(that.bounds);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, bounds);
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }
}
