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
package com.squareup.javawriter;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
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
  public static ParameterizedType parameterizedType(
      final Type rawType, final Type... typeArguments) {
    checkNotPrimitive(rawType);
    for (Type typeArgument : typeArguments) {
      checkNotPrimitive(typeArgument);
    }

    return new ParameterizedType() {
      @Override public Type[] getActualTypeArguments() {
        return typeArguments.clone();
      }
      @Override public Type getRawType() {
        return rawType;
      }
      @Override public Type getOwnerType() {
        return null;
      }
      @Override public boolean equals(Object other) {
        return other instanceof ParameterizedType
            && ((ParameterizedType) other).getOwnerType() == null
            && rawType.equals(((ParameterizedType) other).getRawType())
            && Arrays.equals(typeArguments, ((ParameterizedType) other).getActualTypeArguments());
      }
      @Override public int hashCode() {
        return Arrays.hashCode(typeArguments) ^ rawType.hashCode();
      }
      @Override public String toString() {
        return typeToString(this);
      }
    };
  }

  /**
   * Returns a new parameterized type, applying {@code typeArguments} to {@code rawType} and
   * with no enclosing owner type.
   */
  public static ParameterizedType parameterizedType(Type rawType, Iterable<Type> typeArguments) {
    return parameterizedType(rawType, Iterables.toArray(typeArguments, Type.class));
  }

  /** Returns an array type whose elements are all instances of {@code componentType}. */
  public static GenericArrayType arrayOf(final Type componentType) {
    checkNotNull(componentType, "componentType == null");

    return new GenericArrayType() {
      @Override public Type getGenericComponentType() {
        return componentType;
      }
      @Override public boolean equals(Object o) {
        return o instanceof GenericArrayType
            && Objects.equal(componentType, ((GenericArrayType) o).getGenericComponentType());
      }
      @Override public int hashCode() {
        return componentType.hashCode();
      }
      @Override public String toString() {
        return typeToString(this);
      }
    };
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

  private static WildcardType wildcardType(final Type[] upperBounds, final Type[] lowerBounds) {
    return new WildcardType() {
      @Override public Type[] getUpperBounds() {
        return upperBounds.clone();
      }
      @Override public Type[] getLowerBounds() {
        return lowerBounds.clone();
      }
      @Override public boolean equals(Object o) {
        return o instanceof WildcardType
            && Arrays.equals(upperBounds, ((WildcardType) o).getUpperBounds())
            && Arrays.equals(lowerBounds, ((WildcardType) o).getLowerBounds());
      }
      @Override public int hashCode() {
        return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
      }
      @Override public String toString() {
        return typeToString(this);
      }
    };
  }

  public static TypeVariable<?> typeVariable(final String name, final Type... bounds) {
    checkNotNull(name);
    for (Type bound : bounds) {
      checkNotPrimitive(bound);
    }

    return new TypeVariable<GenericDeclaration>() {
      @Override public Type[] getBounds() {
        return bounds.clone();
      }
      @Override public String getName() {
        return name;
      }
      @Override public GenericDeclaration getGenericDeclaration() {
        throw new UnsupportedOperationException();
      }
      @Override public boolean equals(Object o) {
        return o instanceof TypeVariable
            && name.equals(((TypeVariable<?>) o).getName());
      }
      @Override public int hashCode() {
        return name.hashCode();
      }
      @Override public String toString() {
        return typeToString(this);
      }

      // Java 8 requires these methods. We have them to compile, but we don't exercise them.
      @Override public AnnotatedType[] getAnnotatedBounds() {
        throw new UnsupportedOperationException();
      }
      @Override public <T extends Annotation> T getAnnotation(Class<T> aClass) {
        throw new UnsupportedOperationException();
      }
      @Override public Annotation[] getAnnotations() {
        throw new UnsupportedOperationException();
      }
      @Override public Annotation[] getDeclaredAnnotations() {
        throw new UnsupportedOperationException();
      }
    };
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
    return new IntersectionType() {
      @Override public Type[] getBounds() {
        return bounds;
      }
      @Override public int hashCode() {
        return Arrays.hashCode(bounds);
      }
      @Override public boolean equals(Object o) {
        return o instanceof IntersectionType
            && Arrays.equals(bounds, ((IntersectionType) o).getBounds());
      }
      @Override public String toString() {
        return typeToString(this);
      }
    };
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
}
