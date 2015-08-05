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
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.TypeParameterElement;
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
    return get((TypeParameterElement) mirror.asElement());
  }

  /** Returns type variable equivalent to {@code element}. */
  public static TypeVariableName get(TypeParameterElement element) {
    String name = element.getSimpleName().toString();
    List<? extends TypeMirror> boundsMirrors = element.getBounds();

    List<TypeName> boundsTypeNames = new ArrayList<>();
    for (TypeMirror typeMirror : boundsMirrors) {
      boundsTypeNames.add(TypeName.get(typeMirror));
    }

    return new TypeVariableName(name, boundsTypeNames);
  }

  /** Returns type variable equivalent to {@code type}. */
  public static TypeVariableName get(TypeVariable<?> type) {
    return new TypeVariableName(type.getName(), TypeName.list(type.getBounds()));
  }
}
