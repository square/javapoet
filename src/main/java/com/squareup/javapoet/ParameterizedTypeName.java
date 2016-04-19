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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

public final class ParameterizedTypeName extends TypeName {
  private final TypeName enclosingTypeOrRawType;
  private final String nestedClassName;
  public final ClassName rawType;
  public final List<TypeName> typeArguments;

  ParameterizedTypeName(ClassName rawType, List<TypeName> typeArguments) {
    this(rawType, typeArguments, new ArrayList<AnnotationSpec>());
  }

  ParameterizedTypeName(ClassName rawType, List<TypeName> typeArguments,
      List<AnnotationSpec> annotations) {
    this(
        rawType,
        rawType.enclosingClassName() != null ? rawType.enclosingClassName() : rawType,
        rawType.enclosingClassName() != null ? rawType.simpleName() : null,
        typeArguments,
        annotations);
  }

  private ParameterizedTypeName(
      ClassName rawType,
      TypeName enclosingTypeOrRawType,
      String nestedClassName,
      List<TypeName> typeArguments,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.rawType = checkNotNull(rawType, "rawType == null");
    this.enclosingTypeOrRawType =
        checkNotNull(enclosingTypeOrRawType, "enclosingTypeOrRawType == null");
    this.nestedClassName = nestedClassName;
    this.typeArguments = Util.immutableList(typeArguments);

    checkArgument(
        !this.typeArguments.isEmpty() || enclosingTypeOrRawType instanceof ParameterizedTypeName,
        "no type arguments: %s",
        enclosingTypeOrRawType);
    for (TypeName typeArgument : this.typeArguments) {
      checkArgument(!typeArgument.isPrimitive() && typeArgument != VOID,
          "invalid type parameter: %s", typeArgument);
    }
  }

  @Override public ParameterizedTypeName annotated(List<AnnotationSpec> annotations) {
    return new ParameterizedTypeName(
        rawType,
        enclosingTypeOrRawType,
        nestedClassName,
        typeArguments,
        concatAnnotations(annotations));
  }

  @Override public TypeName withoutAnnotations() {
    return new ParameterizedTypeName(
        rawType,
        enclosingTypeOrRawType,
        nestedClassName,
        typeArguments,
        new ArrayList<AnnotationSpec>());
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    enclosingTypeOrRawType.emitAnnotations(out);
    enclosingTypeOrRawType.emit(out);
    if (nestedClassName != null) {
      out.emit("." + nestedClassName);
    }
    if (!typeArguments.isEmpty()) {
      out.emitAndIndent("<");
      boolean firstParameter = true;
      for (TypeName parameter : typeArguments) {
        if (!firstParameter) out.emitAndIndent(", ");
        parameter.emitAnnotations(out);
        parameter.emit(out);
        firstParameter = false;
      }
      out.emitAndIndent(">");
    }
    return out;
  }

  /**
   * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
   * inside this class.
   */
  public ParameterizedTypeName nestedClass(String name) {
    checkNotNull(name, "name == null");
    return new ParameterizedTypeName(
        rawType.nestedClass(name),
        this,
        name,
        new ArrayList<TypeName>(),
        new ArrayList<AnnotationSpec>());
  }

  /**
   * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
   * inside this class, with the specified {@code typeArguments}.
   */
  public ParameterizedTypeName nestedClass(String name, List<TypeName> typeArguments) {
    checkNotNull(name, "name == null");
    return new ParameterizedTypeName(
        rawType.nestedClass(name),
        this,
        name,
        typeArguments,
        new ArrayList<AnnotationSpec>());
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
    ParameterizedType toTest = type;
    Deque<ParameterizedType> enclosingStack = new ArrayDeque<ParameterizedType>();
    do {
      enclosingStack.push(toTest);
      toTest = (toTest.getOwnerType() instanceof ParameterizedType)
          && ! Modifier.isFinal(((Class<?>) type.getRawType()).getModifiers())
          ? (ParameterizedType) toTest.getOwnerType() : null;
    } while (toTest != null);

    ParameterizedTypeName paramTypeName = null;
    while (!enclosingStack.isEmpty()) {
      ParameterizedType ownerType = enclosingStack.pop();
      ClassName rawType = ClassName.get((Class<?>) type.getRawType());
      List<TypeName> typeArguments = TypeName.list(ownerType.getActualTypeArguments(), map);
      paramTypeName = (paramTypeName != null)
          ? paramTypeName.nestedClass(rawType.simpleName(), typeArguments)
          : new ParameterizedTypeName(rawType, typeArguments);
    }
    return paramTypeName;
  }
}
