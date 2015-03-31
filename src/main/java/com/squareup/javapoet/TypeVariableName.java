/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.javapoet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

public final class TypeVariableName extends TypeName {
  public final String name;
  public final List<TypeName> bounds;

  private TypeVariableName(String name, List<TypeName> bounds) {
    // Strip java.lang.Object from bounds if it is present.
    List<TypeName> boundsNoObject = new ArrayList<>(bounds);
    boundsNoObject.remove(OBJECT);

    this.name = checkNotNull(name, "name == null");
    this.bounds = Collections.unmodifiableList(boundsNoObject);

    for (TypeName bound : this.bounds) {
      checkArgument(!bound.isPrimitive() && bound != VOID, "invalid bound: %s", bound);
    }
  }

  @Override public boolean equals(Object o) {
    return o instanceof TypeVariableName
        && ((TypeVariableName) o).name.equals(name)
        && ((TypeVariableName) o).bounds.equals(bounds);
  }

  @Override public int hashCode() {
    return name.hashCode() ^ bounds.hashCode();
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    return out.emitAndIndent(name);
  }

  /** Returns type variable named {@code name} without bounds. */
  public static TypeVariableName get(String name) {
    return new TypeVariableName(name, Collections.<TypeName>emptyList());
  }

  /** Returns type variable named {@code name} with {@code bounds}. */
  public static TypeVariableName get(String name, TypeName... bounds) {
    return new TypeVariableName(name, Arrays.asList(bounds));
  }

  /** Returns type variable named {@code name} with {@code bounds}. */
  public static TypeVariableName get(String name, Type... bounds) {
    return new TypeVariableName(name, TypeName.list(bounds));
  }

  /** Returns type variable equivalent to {@code mirror}. */
  public static TypeVariableName get(javax.lang.model.type.TypeVariable mirror) {
    String name = mirror.asElement().getSimpleName().toString();
    List<? extends TypeMirror> boundsMirrors = typeVariableBounds(mirror);

    List<TypeName> boundsTypeNames = new ArrayList<>();
    for (TypeMirror typeMirror : boundsMirrors) {
      boundsTypeNames.add(TypeName.get(typeMirror));
    }

    return new TypeVariableName(name, boundsTypeNames);
  }

  /**
   * Returns a list of type mirrors representing the unpacked bounds of {@code typeVariable}. This
   * is made gnarly by the need to unpack Java 8's new IntersectionType with reflection. We don't
   * have that type in Java 7, and {@link TypeVariable}'s array of bounds is sufficient anyway.
   */
  @SuppressWarnings("unchecked") // Gross things in support of Java 7 and Java 8.
  private static List<? extends TypeMirror> typeVariableBounds(
      javax.lang.model.type.TypeVariable typeVariable) {
    TypeMirror upperBound = typeVariable.getUpperBound();

    // On Java 8, unwrap an intersection type into its component bounds.
    if ("INTERSECTION".equals(upperBound.getKind().name())) {
      try {
        Method method = upperBound.getClass().getMethod("getBounds");
        return (List<? extends TypeMirror>) method.invoke(upperBound);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // On Java 7, intersection types exist but without explicit API. Use a (clumsy) heuristic.
    if (upperBound.getKind() == TypeKind.DECLARED) {
      TypeElement upperBoundElement = (TypeElement) ((DeclaredType) upperBound).asElement();
      if (upperBoundElement.getNestingKind() == NestingKind.ANONYMOUS) {
        List<TypeMirror> result = new ArrayList<>();
        result.add(upperBoundElement.getSuperclass());
        result.addAll(upperBoundElement.getInterfaces());
        return result;
      }
    }

    return Collections.singletonList(upperBound);
  }

  /** Returns type variable equivalent to {@code type}. */
  public static TypeVariableName get(TypeVariable<?> type) {
    return new TypeVariableName(type.getName(), TypeName.list(type.getBounds()));
  }
}
