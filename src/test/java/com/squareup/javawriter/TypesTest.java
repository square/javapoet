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

import com.google.testing.compile.CompilationRule;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assert_;

public final class TypesTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private TypeMirror getType(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  @Test public void getBasicTypeMirror() {
    assert_().that(Types.get(getType(Object.class)))
        .isEqualTo(ClassName.fromClass(Object.class));
    assert_().that(Types.get(getType(Charset.class)))
        .isEqualTo(ClassName.fromClass(Charset.class));
    assert_().that(Types.get(getType(TypesTest.class)))
        .isEqualTo(ClassName.fromClass(TypesTest.class));
  }

  @Test public void getParameterizedTypeMirror() {
    DeclaredType setType =
        compilation.getTypes().getDeclaredType(getElement(Set.class), getType(Object.class));
    assert_().that(Types.get(setType))
        .isEqualTo(Types.parameterizedType(ClassName.fromClass(Set.class), ClassName.OBJECT));
  }

  static class Parameterized<
      Simple,
      ExtendsClass extends Number,
      ExtendsInterface extends Runnable,
      ExtendsTypeVariable extends Simple,
      Intersection extends Number & Runnable,
      IntersectionOfInterfaces extends Runnable & Serializable> {}

  @Test public void getTypeVariableTypeMirror() {
    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();

    assert_().that(Types.get(typeVariables.get(0).asType()))
        .isEqualTo(Types.typeVariable("Simple"));
    assert_().that(Types.get(typeVariables.get(1).asType()))
        .isEqualTo(Types.typeVariable("ExtendsClass", Number.class));
    assert_().that(Types.get(typeVariables.get(2).asType()))
        .isEqualTo(Types.typeVariable("ExtendsInterface", Runnable.class));
    assert_().that(Types.get(typeVariables.get(3).asType()))
        .isEqualTo(Types.typeVariable("ExtendsTypeVariable", Types.typeVariable("Simple")));
    assert_().that(Types.get(typeVariables.get(4).asType()))
        .isEqualTo(Types.typeVariable("Intersection", Number.class, Runnable.class));
    assert_().that(Types.get(typeVariables.get(5).asType()))
        .isEqualTo(Types.typeVariable("IntersectionOfInterfaces",
            Runnable.class, Serializable.class));
  }

  @Test public void typeVariableBounds() {
    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();
    TypeVariable typeVariable = (TypeVariable) Types.get(typeVariables.get(4).asType());
    Type[] bounds = typeVariable.getBounds();

    if (bounds.length == 1) {
      // Java 8.
      IntersectionType intersectionType = (IntersectionType) bounds[0];
      assert_().that(intersectionType.getBounds()).asList()
          .containsExactly(ClassName.fromClass(Number.class), ClassName.fromClass(Runnable.class));
      assert_().that(intersectionType.toString())
          .isEqualTo("java.lang.Number & java.lang.Runnable");
    } else {
      // Java ≤ 7.
      assert_().that(bounds).asList()
          .containsExactly(ClassName.fromClass(Number.class), ClassName.fromClass(Runnable.class));
    }
  }

  @Test public void getPrimitiveTypeMirror() {
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(boolean.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(byte.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(short.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(int.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(long.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(char.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(float.class);
    assert_().that(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(double.class);
  }

  @Test public void getArrayTypeMirror() {
    assert_().that(Types.get(compilation.getTypes().getArrayType(getType(Object.class))))
        .isEqualTo(Types.arrayOf(ClassName.OBJECT));
  }

  @Test public void getVoidTypeMirror() {
    assert_().that(Types.get(compilation.getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(void.class);
  }

  @Test public void getNullTypeMirror() {
    assert_().that(Types.get(compilation.getTypes().getNullType()))
        .isEqualTo(Types.NULL);
  }

  @Test public void parameterizedType() throws Exception {
    ParameterizedType type = Types.parameterizedType(Map.class, String.class, Long.class);
    assert_().that(type.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Long>");
  }

  @Test public void arrayType() throws Exception {
    GenericArrayType type = Types.arrayOf(String.class);
    assert_().that(type.toString()).isEqualTo("java.lang.String[]");
  }

  @Test public void wildcardExtendsType() throws Exception {
    WildcardType type = Types.subtypeOf(CharSequence.class);
    assert_().that(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  @Test public void wildcardExtendsObject() throws Exception {
    WildcardType type = Types.subtypeOf(Object.class);
    assert_().that(type.toString()).isEqualTo("?");
  }

  @Test public void wildcardSuperType() throws Exception {
    WildcardType type = Types.supertypeOf(String.class);
    assert_().that(type.toString()).isEqualTo("? super java.lang.String");
  }

  @Test public void typeVariable() throws Exception {
    TypeVariable<?> type = Types.typeVariable("T", CharSequence.class);
    assert_().that(type.toString()).isEqualTo("T"); // (Bounds are only emitted in declaration.)
  }
}
