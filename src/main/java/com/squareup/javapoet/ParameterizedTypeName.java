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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

public final class ParameterizedTypeName extends TypeName {
  public final ClassName rawType;
  public final List<TypeName> typeArguments;

  ParameterizedTypeName(ClassName rawType, List<TypeName> typeArguments) {
    this(rawType, typeArguments, new ArrayList<AnnotationSpec>());
  }

  ParameterizedTypeName(ClassName rawType, List<TypeName> typeArguments,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.rawType = checkNotNull(rawType, "rawType == null");
    this.typeArguments = Util.immutableList(typeArguments);

    checkArgument(!this.typeArguments.isEmpty(), "no type arguments: %s", rawType);
    for (TypeName typeArgument : this.typeArguments) {
      checkArgument(!typeArgument.isPrimitive() && typeArgument != VOID,
          "invalid type parameter: %s", typeArgument);
    }
  }

  @Override public ParameterizedTypeName annotated(List<AnnotationSpec> annotations) {
    return new ParameterizedTypeName(rawType, typeArguments, concatAnnotations(annotations));
  }

  @Override public TypeName withoutAnnotations() {
    return new ParameterizedTypeName(rawType, typeArguments);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    rawType.toString(out, annotations); // awkward: use our annotations with the raw type...
    out.emitAndIndent("<");
    boolean firstParameter = true;
    for (TypeName parameter : typeArguments) {
      if (!firstParameter) out.emitAndIndent(", ");
      parameter.toString(out, parameter.annotations);
      firstParameter = false;
    }
    return out.emitAndIndent(">");
  }

  @Override CodeWriter toString(CodeWriter out, Iterable<AnnotationSpec> annos) throws IOException {
    // annotations are handled by emit(out), see #431
    return emit(out);
  }

  /** Returns a parameterized type, applying {@code typeArguments} to {@code rawType}. */
  public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
    return new ParameterizedTypeName(rawType, Arrays.asList(typeArguments));
  }

  /** Returns a parameterized type, applying {@code typeArguments} to {@code rawType}. */
  public static ParameterizedTypeName get(Class<?> rawType, Type... typeArguments) {
    return new ParameterizedTypeName(ClassName.get(rawType), list(typeArguments));
  }

  /** Returns a parameterized type equivalent to {@code type}. */
  public static ParameterizedTypeName get(ParameterizedType type) {
    return get(type, new LinkedHashMap<Type, TypeVariableName>());
  }

  /** Returns a parameterized type equivalent to {@code type}. */
  static ParameterizedTypeName get(ParameterizedType type, Map<Type, TypeVariableName> map) {
    return new ParameterizedTypeName(ClassName.get((Class<?>) type.getRawType()),
        TypeName.list(type.getActualTypeArguments(), map));
  }
}
