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
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Set;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import static com.squareup.javawriter.TypeNames.FOR_TYPE_MIRROR;

public final class TypeVariableName extends TypeName {
  private final String name;
  private final Optional<TypeName> upperBound;

  TypeVariableName(String name, Optional<TypeName> upperBound) {
    this.name = name;
    this.upperBound = upperBound;
  }

  public static TypeVariableName create(String name) {
    return new TypeVariableName(name, Optional.<TypeName>absent());
  }

  public static TypeVariableName create(String name, TypeName upperBound) {
    return new TypeVariableName(name, Optional.of(upperBound));
  }

  static TypeVariableName forTypeMirror(TypeVariable mirror) {
    String name = mirror.asElement().getSimpleName().toString();

    TypeMirror upperBound = mirror.getUpperBound();
    FluentIterable<TypeMirror> bounds = FluentIterable.from(ImmutableList.of(upperBound));
    // Try to detect intersection types for Java 7 (Java 8+ has a new TypeKind for that)
    // Unfortunately, we can't put this logic into TypeNames.forTypeMirror() as this heuristic
    // only really works in the context of a TypeVariable's upper bound.
    if (upperBound.getKind() == TypeKind.DECLARED) {
      TypeElement bound = (TypeElement) ((DeclaredType) upperBound).asElement();
      if (bound.getNestingKind() == NestingKind.ANONYMOUS) {
        // This is (likely) an intersection type.
        bounds = FluentIterable
            .from(ImmutableList.of(bound.getSuperclass()))
            .append(bound.getInterfaces());
      }
    }
    ImmutableList<TypeName> typeNames = bounds.transform(FOR_TYPE_MIRROR)
        .filter(Predicates.not(Predicates.<TypeName>equalTo(ClassName.fromClass(Object.class))))
        .toList();
    if (typeNames.size() == 1) {
      return new TypeVariableName(name, Optional.of(Iterables.getOnlyElement(typeNames)));
    } else if (!typeNames.isEmpty()) {
      return new TypeVariableName(name, Optional.<TypeName>of(new IntersectionTypeName(typeNames)));
    }
    return new TypeVariableName(name, Optional.<TypeName>absent());
  }

  public String name() {
    return name;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), upperBound.asSet());
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    super.write(appendable, context);
    appendable.append(name);
    if (upperBound.isPresent()) {
      appendable.append(" extends ");
      upperBound.get().write(appendable, context);
    }
    return appendable;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (super.equals(obj) && obj instanceof TypeVariableName) {
      TypeVariableName that = (TypeVariableName) obj;
      return this.name.equals(that.name)
          && this.upperBound.equals(that.upperBound);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), name, upperBound);
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }
}
