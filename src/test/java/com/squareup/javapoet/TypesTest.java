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
package com.squareup.javapoet;

import com.google.testing.compile.CompilationRule;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class TypesTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private TypeMirror getType(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  @Test public void getBasicTypeMirror() {
    assertThat(Types.get(getType(Object.class)))
        .isEqualTo(ClassName.get(Object.class));
    assertThat(Types.get(getType(Charset.class)))
        .isEqualTo(ClassName.get(Charset.class));
    assertThat(Types.get(getType(TypesTest.class)))
        .isEqualTo(ClassName.get(TypesTest.class));
  }

  @Test public void getParameterizedTypeMirror() {
    DeclaredType setType =
        compilation.getTypes().getDeclaredType(getElement(Set.class), getType(Object.class));
    assertThat(Types.get(setType))
        .isEqualTo(Types.parameterizedType(ClassName.get(Set.class), ClassName.OBJECT));
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

    // Members of converted types use ClassName and not Class<?>.
    ClassName number = ClassName.get(Number.class);
    ClassName runnable = ClassName.get(Runnable.class);
    ClassName serializable = ClassName.get(Serializable.class);

    assertThat(Types.get(typeVariables.get(0).asType()))
        .isEqualTo(Types.typeVariable("Simple"));
    assertThat(Types.get(typeVariables.get(1).asType()))
        .isEqualTo(Types.typeVariable("ExtendsClass", number));
    assertThat(Types.get(typeVariables.get(2).asType()))
        .isEqualTo(Types.typeVariable("ExtendsInterface", runnable));
    assertThat(Types.get(typeVariables.get(3).asType()))
        .isEqualTo(Types.typeVariable("ExtendsTypeVariable", Types.typeVariable("Simple")));
    assertThat(Types.get(typeVariables.get(4).asType()))
        .isEqualTo(Types.typeVariable("Intersection", number, runnable));
    assertThat(Types.get(typeVariables.get(5).asType()))
        .isEqualTo(Types.typeVariable("IntersectionOfInterfaces", runnable, serializable));
    assertThat(((TypeVariable) Types.get(typeVariables.get(4).asType())).getBounds()).asList()
        .containsExactly(number, runnable);
  }

  @Test public void getPrimitiveTypeMirror() {
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(boolean.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(byte.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(short.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(int.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(long.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(char.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(float.class);
    assertThat(Types.get(compilation.getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(double.class);
  }

  @Test public void getArrayTypeMirror() {
    assertThat(Types.get(compilation.getTypes().getArrayType(getType(Object.class))))
        .isEqualTo(Types.arrayOf(ClassName.OBJECT));
  }

  @Test public void getVoidTypeMirror() {
    assertThat(Types.get(compilation.getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(void.class);
  }

  @Test public void getNullTypeMirror() {
    assertThat(Types.get(compilation.getTypes().getNullType()))
        .isEqualTo(Types.NULL);
  }

  @Test public void parameterizedType() throws Exception {
    ParameterizedType type = Types.parameterizedType(Map.class, String.class, Long.class);
    assertThat(type.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Long>");
  }

  @Test public void arrayType() throws Exception {
    GenericArrayType type = Types.arrayOf(String.class);
    assertThat(type.toString()).isEqualTo("java.lang.String[]");
  }

  @Test public void wildcardExtendsType() throws Exception {
    WildcardType type = Types.subtypeOf(CharSequence.class);
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  @Test public void wildcardExtendsObject() throws Exception {
    WildcardType type = Types.subtypeOf(Object.class);
    assertThat(type.toString()).isEqualTo("?");
  }

  @Test public void wildcardSuperType() throws Exception {
    WildcardType type = Types.supertypeOf(String.class);
    assertThat(type.toString()).isEqualTo("? super java.lang.String");
  }

  @Test public void typeVariable() throws Exception {
    TypeVariable<?> type = Types.typeVariable("T", CharSequence.class);
    assertThat(type.toString()).isEqualTo("T"); // (Bounds are only emitted in declaration.)
  }
}
