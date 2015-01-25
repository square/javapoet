/**
 * Copyright (C) 2008 Google Inc.
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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Static methods for working with types. */
// Forked from a similar class in Gson.
public final class Types {
  private static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

  private static final Function<TypeMirror, Type> FOR_TYPE_MIRROR =
      new Function<TypeMirror, Type>() {
        @Override public Type apply(TypeMirror input) {
          return get(input);
        }
      };

  static final Type NULL = new Type() {
    @Override public String toString() {
      return "null";
    }
  };

  private Types() {
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType} and
   * with no enclosing owner type.
   */
  public static ParameterizedType parameterizedType(Type rawType, Type... typeArguments) {
    checkNotPrimitive(rawType);
    for (Type typeArgument : typeArguments) {
      checkNotPrimitive(typeArgument);
    }
    Map<String, Object> accessors = new LinkedHashMap<>();
    accessors.put("getActualTypeArguments", typeArguments.clone());
    accessors.put("getRawType", rawType);
    accessors.put("getOwnerType", null);
    int hashCode = Arrays.hashCode(typeArguments) ^ rawType.hashCode();
    return newType(ParameterizedType.class, accessors, hashCode);
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType} and
   * with no enclosing owner type.
   */
  public static ParameterizedType parameterizedType(Type rawType, Iterable<Type> typeArguments) {
    return parameterizedType(rawType, Iterables.toArray(typeArguments, Type.class));
  }

  /** Returns an array type whose elements are all instances of {@code componentType}. */
  public static GenericArrayType arrayOf(Type componentType) {
    checkNotNull(componentType, "componentType == null");

    Map<String, Object> accessors = new LinkedHashMap<>();
    accessors.put("getGenericComponentType", componentType);
    int hashCode = componentType.hashCode();
    return newType(GenericArrayType.class, accessors, hashCode);
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for {@code
   * ? extends Object}.
   */
  public static WildcardType subtypeOf(Type bound) {
    checkNotPrimitive(bound);
    return wildcardType(new Type[] {bound}, EMPTY_TYPE_ARRAY);
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if {@code
   * bound} is {@code String.class}, this returns {@code ? super String}.
   */
  public static WildcardType supertypeOf(Type bound) {
    checkNotPrimitive(bound);
    return wildcardType(new Type[] {Object.class}, new Type[] {bound});
  }

  private static WildcardType wildcardType(Type[] upperBounds, Type[] lowerBounds) {
    Map<String, Object> accessors = new LinkedHashMap<>();
    accessors.put("getUpperBounds", upperBounds.clone());
    accessors.put("getLowerBounds", lowerBounds.clone());
    int hashCode = Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
    return newType(WildcardType.class, accessors, hashCode);
  }

  public static TypeVariable<?> typeVariable(String name, Type... bounds) {
    checkNotNull(name);
    for (Type bound : bounds) {
      checkNotPrimitive(bound);
    }

    Map<String, Object> accessors = new LinkedHashMap<>();
    accessors.put("getBounds", bounds.clone());
    accessors.put("getName", name);
    int hashCode = name.hashCode();
    return newType(TypeVariable.class, accessors, hashCode);
  }

  public static Type get(TypeMirror mirror) {
    return mirror.accept(new SimpleTypeVisitor6<Type, Void>() {
      @Override protected Type defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException(e.toString());
      }

      @Override public GenericArrayType visitArray(ArrayType t, Void p) {
        return arrayOf(t.getComponentType().accept(this, null));
      }

      @Override public Type visitDeclared(DeclaredType t, Void p) {
        return get(t);
      }

      @Override public Class<?> visitPrimitive(PrimitiveType t, Void p) {
        return get(t);
      }

      @Override public Type visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
        return get(t);
      }

      @Override public Type visitIntersection(javax.lang.model.type.IntersectionType t, Void p) {
        return get(t);
      }

      @Override public Type visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
        return get(t);
      }

      @Override public Type visitNull(NullType t, Void p) {
        return NULL;
      }

      @Override public Type visitNoType(NoType t, Void p) {
        if (t.getKind() == TypeKind.VOID) return void.class;
        return super.visitUnknown(t, p);
      }
    }, null);
  }

  private static Type get(DeclaredType t) {
    return t.getTypeArguments().isEmpty()
        ? ClassName.get((TypeElement) t.asElement())
        : parameterizedType(ClassName.get((TypeElement) t.asElement()),
            FluentIterable.from(t.getTypeArguments()).transform(FOR_TYPE_MIRROR));
  }

  private static TypeVariable<?> get(javax.lang.model.type.TypeVariable mirror) {
    String name = mirror.asElement().getSimpleName().toString();

    TypeMirror upperBound = mirror.getUpperBound();
    FluentIterable<TypeMirror> bounds = FluentIterable.from(ImmutableList.of(upperBound));
    // Try to detect intersection types for Java 7 (Java 8+ has a new TypeKind for that)
    // Unfortunately, we can't put this logic into Types.get() as this heuristic only really works
    // in the context of a TypeVariable's upper bound.
    if (upperBound.getKind() == TypeKind.DECLARED) {
      TypeElement bound = (TypeElement) ((DeclaredType) upperBound).asElement();
      if (bound.getNestingKind() == NestingKind.ANONYMOUS) {
        // This is (likely) an intersection type.
        bounds = FluentIterable
            .from(ImmutableList.of(bound.getSuperclass()))
            .append(bound.getInterfaces());
      }
    }
    Type[] types = bounds.transform(FOR_TYPE_MIRROR)
        .filter(Predicates.not(Predicates.<Type>equalTo(ClassName.OBJECT)))
        .toArray(Type.class);
    return typeVariable(name, types);
  }

  private static Type get(javax.lang.model.type.WildcardType mirror) {
    Type extendsBound = get(mirror.getExtendsBound());
    Type superBound = get(mirror.getSuperBound());
    return superBound != null ? Types.supertypeOf(superBound) : Types.subtypeOf(extendsBound);
  }

  private static Class<?> get(PrimitiveType mirror) {
    switch (mirror.getKind()) {
      case BOOLEAN:
        return boolean.class;
      case BYTE:
        return byte.class;
      case SHORT:
        return short.class;
      case INT:
        return int.class;
      case LONG:
        return long.class;
      case CHAR:
        return char.class;
      case FLOAT:
        return float.class;
      case DOUBLE:
        return double.class;
      default:
        throw new AssertionError();
    }
  }

  private static Type get(javax.lang.model.type.IntersectionType mirror) {
    final Type[] bounds = FluentIterable.from(mirror.getBounds())
        .transform(FOR_TYPE_MIRROR)
        .filter(Predicates.not(Predicates.<Type>equalTo(ClassName.OBJECT)))
        .toArray(Type.class);
    return intersection(bounds);
  }

  static Type intersection(Type... bounds) {
    Map<String, Object> accessors = new LinkedHashMap<>();
    accessors.put("getBounds", bounds.clone());
    int hashCode = Arrays.hashCode(bounds);
    return newType(IntersectionType.class, accessors, hashCode);
  }

  private static void checkNotPrimitive(Type type) {
    checkNotNull(type, "type cannot be primitive.");
    checkArgument(!(type instanceof Class<?>) || !((Class<?>) type).isPrimitive(),
        "type cannot be primitive.");
  }

  private static String typeToString(Type type) {
    try {
      StringBuilder result = new StringBuilder();
      new CodeWriter(result).emit("$T", type);
      return result.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  /** Returns the array component of {@code type}, or null if {@code type} is not an array. */
  static Type arrayComponent(Type type) {
    if (type instanceof Class<?>) {
      return ((Class<?>) type).getComponentType();
    } else if (type instanceof GenericArrayType) {
      return ((GenericArrayType) type).getGenericComponentType();
    } else {
      return null;
    }
  }

  /**
   * Implement reflective types using a dynamic proxy so we can compile on both Java 7 and Java 8,
   * even though {@link TypeVariable} gained new methods in Java 8 that won't compile on Java 7.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Type> T newType(
      final Class<T> type, final Map<String, Object> accessors, final int hashCode) {
    ClassLoader classLoader = Types.class.getClassLoader();
    Class[] classes = {type};
    return (T) Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
      @Override public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        switch (method.getName()) {
          case "equals":
            Object other = objects[0];
            if (!type.isInstance(other)) return false;
            for (Map.Entry<String, Object> entry : accessors.entrySet()) {
              Object otherProperty = other.getClass().getMethod(entry.getKey()).invoke(other);
              if (!equal(otherProperty, entry.getValue())) return false;
            }
            return true;

          case "hashCode":
            return hashCode;

          case "toString":
            return typeToString((Type) o);

          default:
            Object result = accessors.get(method.getName());
            if (result == null && !accessors.containsKey(method.getName())) {
              throw new UnsupportedOperationException(method.getName());
            }
            return result instanceof Object[] ? ((Object[]) result).clone() : result;
        }
      }

      boolean equal(Object a, Object b) {
        if (a == null) return b == null;
        if (a instanceof Object[]) return Arrays.equals((Object[]) a, (Object[]) b);
        return a.equals(b);
      }
    });
  }
}
