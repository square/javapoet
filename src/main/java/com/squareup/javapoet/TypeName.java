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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor7;

/**
 * Any type in Java's type system, plus {@code void}. This class is an identifier for primitive
 * types like {@code int} and raw reference types like {@code String} and {@code List}. It also
 * identifies composite types like {@code char[]} and {@code Set<Long>}.
 *
 * <p>Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for {@code java.lang.List} doesn't know about the {@code size()} method, the fact that
 * lists are collections, or even that it accepts a single type parameter.
 *
 * <p>Instances of this class are immutable value objects that implement {@code equals()} and {@code
 * hashCode()} properly.
 *
 * <h3>Referencing existing types</h3>
 *
 * <p>Primitives and void are constants that you can reference directly: see {@link #INT}, {@link
 * #DOUBLE}, and {@link #VOID}.
 *
 * <p>In an annotation processor you can get a type name instance for a type mirror by calling
 * {@link #get(TypeMirror)}. In reflection code, you can use {@link #get(Type)}.
 *
 * <h3>Defining new types</h3>
 *
 * <p>Create new reference types like {@code com.example.HelloWorld} with {@link
 * ClassName#get(String, String, String...)}. To build composite types like {@code char[]} and
 * {@code Set<Long>}, use the factory methods on {@link ArrayTypeName}, {@link
 * ParameterizedTypeName}, {@link TypeVariableName}, and {@link WildcardTypeName}.
 */
public class TypeName {
  public static final TypeName VOID = new TypeName("void");
  public static final TypeName BOOLEAN = new TypeName("boolean");
  public static final TypeName BYTE = new TypeName("byte");
  public static final TypeName SHORT = new TypeName("short");
  public static final TypeName INT = new TypeName("int");
  public static final TypeName LONG = new TypeName("long");
  public static final TypeName CHAR = new TypeName("char");
  public static final TypeName FLOAT = new TypeName("float");
  public static final TypeName DOUBLE = new TypeName("double");
  public static final ClassName OBJECT = ClassName.get("java.lang", "Object");

  private static final ClassName BOXED_VOID = ClassName.get("java.lang", "Void");
  private static final ClassName BOXED_BOOLEAN = ClassName.get("java.lang", "Boolean");
  private static final ClassName BOXED_BYTE = ClassName.get("java.lang", "Byte");
  private static final ClassName BOXED_SHORT = ClassName.get("java.lang", "Short");
  private static final ClassName BOXED_INT = ClassName.get("java.lang", "Integer");
  private static final ClassName BOXED_LONG = ClassName.get("java.lang", "Long");
  private static final ClassName BOXED_CHAR = ClassName.get("java.lang", "Character");
  private static final ClassName BOXED_FLOAT = ClassName.get("java.lang", "Float");
  private static final ClassName BOXED_DOUBLE = ClassName.get("java.lang", "Double");

  /** The name of this type if it is a keyword, or null. */
  private final String keyword;

  private TypeName(String keyword) {
    this.keyword = keyword;
  }

  // Package-private constructor to prevent third-party subclasses.
  TypeName() {
    this(null);
  }

  public boolean isPrimitive() {
    return keyword != null && this != VOID;
  }

  /**
   * Returns a boxed type if this is a primitive type (like {@code Integer} for {@code int}) or
   * {@code void}. Returns this type if boxing doesn't apply.
   */
  public TypeName box() {
    if (keyword == null) return this; // Doesn't need boxing.
    if (this == VOID) return BOXED_VOID;
    if (this == BOOLEAN) return BOXED_BOOLEAN;
    if (this == BYTE) return BOXED_BYTE;
    if (this == SHORT) return BOXED_SHORT;
    if (this == INT) return BOXED_INT;
    if (this == LONG) return BOXED_LONG;
    if (this == CHAR) return BOXED_CHAR;
    if (this == FLOAT) return BOXED_FLOAT;
    if (this == DOUBLE) return BOXED_DOUBLE;
    throw new AssertionError(keyword);
  }

  /**
   * Returns an unboxed type if this is a boxed primitive type (like {@code int} for {@code
   * Integer}) or {@code Void}. Returns this type if it is already unboxed.
   *
   * @throws UnsupportedOperationException if this type isn't eligible for unboxing.
   */
  public TypeName unbox() {
    if (keyword != null) return this; // Already unboxed.
    if (this.equals(BOXED_VOID)) return VOID;
    if (this.equals(BOXED_BOOLEAN)) return BOOLEAN;
    if (this.equals(BOXED_BYTE)) return BYTE;
    if (this.equals(BOXED_SHORT)) return SHORT;
    if (this.equals(BOXED_INT)) return INT;
    if (this.equals(BOXED_LONG)) return LONG;
    if (this.equals(BOXED_CHAR)) return CHAR;
    if (this.equals(BOXED_FLOAT)) return FLOAT;
    if (this.equals(BOXED_DOUBLE)) return DOUBLE;
    throw new UnsupportedOperationException("cannot unbox " + this);
  }

  @Override public final String toString() {
    try {
      StringBuilder result = new StringBuilder();
      emit(new CodeWriter(result));
      return result.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  CodeWriter emit(CodeWriter out) throws IOException {
    if (keyword == null) throw new AssertionError();
    return out.emitAndIndent(keyword);
  }

  /** Returns a type name equivalent to {@code mirror}. */
  public static TypeName get(TypeMirror mirror) {
    return mirror.accept(new SimpleTypeVisitor7<TypeName, Void>() {
      @Override public TypeName visitPrimitive(PrimitiveType t, Void p) {
        switch (t.getKind()) {
          case BOOLEAN:
            return TypeName.BOOLEAN;
          case BYTE:
            return TypeName.BYTE;
          case SHORT:
            return TypeName.SHORT;
          case INT:
            return TypeName.INT;
          case LONG:
            return TypeName.LONG;
          case CHAR:
            return TypeName.CHAR;
          case FLOAT:
            return TypeName.FLOAT;
          case DOUBLE:
            return TypeName.DOUBLE;
          default:
            throw new AssertionError();
        }
      }

      @Override public TypeName visitDeclared(DeclaredType t, Void p) {
        ClassName rawType = ClassName.get((TypeElement) t.asElement());
        if (t.getTypeArguments().isEmpty()) return rawType;

        List<TypeName> typeArgumentNames = new ArrayList<>();
        for (TypeMirror mirror : t.getTypeArguments()) {
          typeArgumentNames.add(get(mirror));
        }
        return new ParameterizedTypeName(rawType, typeArgumentNames);
      }

      @Override public ArrayTypeName visitArray(ArrayType t, Void p) {
        return ArrayTypeName.get(t);
      }

      @Override public TypeName visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
        return TypeVariableName.get(t);
      }

      @Override public TypeName visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
        return WildcardTypeName.get(t);
      }

      @Override public TypeName visitNoType(NoType t, Void p) {
        if (t.getKind() == TypeKind.VOID) return TypeName.VOID;
        return super.visitUnknown(t, p);
      }

      @Override protected TypeName defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException("Unexpected type mirror: " + e);
      }
    }, null);
  }

  /** Returns a type name equivalent to {@code type}. */
  public static TypeName get(Type type) {
    if (type instanceof Class<?>) {
      Class<?> classType = (Class<?>) type;
      if (type == void.class) return VOID;
      if (type == boolean.class) return BOOLEAN;
      if (type == byte.class) return BYTE;
      if (type == short.class) return SHORT;
      if (type == int.class) return INT;
      if (type == long.class) return LONG;
      if (type == char.class) return CHAR;
      if (type == float.class) return FLOAT;
      if (type == double.class) return DOUBLE;
      if (classType.isArray()) return ArrayTypeName.of(get(classType.getComponentType()));
      return ClassName.get(classType);

    } else if (type instanceof ParameterizedType) {
      return ParameterizedTypeName.get((ParameterizedType) type);

    } else if (type instanceof WildcardType) {
      return WildcardTypeName.get((WildcardType) type);

    } else if (type instanceof TypeVariable<?>) {
      return TypeVariableName.get((TypeVariable<?>) type);

    } else if (type instanceof GenericArrayType) {
      return ArrayTypeName.get((GenericArrayType) type);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type);
    }
  }

  /** Converts an array of types to a list of type names. */
  static List<TypeName> list(Type[] types) {
    List<TypeName> result = new ArrayList<>(types.length);
    for (Type type : types) {
      result.add(get(type));
    }
    return result;
  }

  /** Returns the array component of {@code type}, or null if {@code type} is not an array. */
  static TypeName arrayComponent(TypeName type) {
    return type instanceof ArrayTypeName
        ? ((ArrayTypeName) type).componentType
        : null;
  }
}
