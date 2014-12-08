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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.lang.reflect.Method;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;

public final class TypeNames {
  static final Function<TypeMirror, TypeName> FOR_TYPE_MIRROR =
      new Function<TypeMirror, TypeName>() {
        @Override public TypeName apply(TypeMirror input) {
          return forTypeMirror(input);
        }
      };

  public static TypeName forClass(Class<?> clazz) {
    if (void.class.equals(clazz)) {
      return VoidName.VOID;
    } else if (clazz.isPrimitive()) {
      return PrimitiveName.forClass(clazz);
    } else if (clazz.isArray()) {
      return new ArrayTypeName(forClass(clazz.getComponentType()));
    } else {
      return ClassName.fromClass(clazz);
    }
  }

  public static TypeName forTypeMirror(TypeMirror mirror) {
    return mirror.accept(new SimpleTypeVisitor6<TypeName, Void>() {
      @Override
      protected TypeName defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException(e.toString());
      }

      @Override
      public TypeName visitUnknown(TypeMirror t, Void p) {
        if ("INTERSECTION".equals(t.getKind().name())) {
          return visitIntersectionType(t);
        }
        return super.visitUnknown(t, p);
      }

      @SuppressWarnings("unchecked") // Gross things in support of Java 8.
      private TypeName visitIntersectionType(TypeMirror t) {
        try {
          Method method = t.getClass().getMethod("getBounds");
          List<? extends TypeMirror> bounds = (List<? extends TypeMirror>) method.invoke(t);
          return new IntersectionTypeName(Lists.transform(bounds, FOR_TYPE_MIRROR));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public ArrayTypeName visitArray(ArrayType t, Void p) {
        return new ArrayTypeName(t.getComponentType().accept(this, null));
      }

      @Override
      public TypeName visitDeclared(DeclaredType t, Void p) {
        return t.getTypeArguments().isEmpty()
            ? ClassName.fromTypeElement((TypeElement) t.asElement())
            : new ParameterizedTypeName(
                ClassName.fromTypeElement((TypeElement) t.asElement()),
                FluentIterable.from(t.getTypeArguments()).transform(FOR_TYPE_MIRROR));
      }

      @Override
      public PrimitiveName visitPrimitive(PrimitiveType t, Void p) {
        return PrimitiveName.forTypeMirror(t);
      }

      @Override
      public TypeName visitTypeVariable(TypeVariable t, Void p) {
        return TypeVariableName.forTypeMirror(t);
      }

      @Override
      public WildcardName visitWildcard(WildcardType t, Void p) {
        return WildcardName.forTypeMirror(t);
      }

      @Override
      public NullName visitNull(NullType t, Void p) {
        return NullName.NULL;
      }

      @Override
      public TypeName visitNoType(NoType t, Void p) {
        switch (t.getKind()) {
          case VOID:
            return VoidName.VOID;
          case PACKAGE:
            throw new IllegalArgumentException();
          default:
            throw new IllegalStateException();
        }
      }
    }, null);
  }

  private TypeNames() {
  }
}
