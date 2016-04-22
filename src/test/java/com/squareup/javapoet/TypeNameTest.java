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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TypeNameTest {

  protected <E extends Enum<E>> E generic(E[] values) {
    return values[0];
  }

  protected static class TestGeneric<T> {
    class Inner {}

    class InnerGeneric<T2> {}

    static class NestedNonGeneric {}
  }

  protected static TestGeneric<String>.Inner testGenericStringInner() {
    return null;
  }

  protected static TestGeneric<Integer>.Inner testGenericIntInner() {
    return null;
  }

  protected static TestGeneric<Short>.InnerGeneric<Long> testGenericInnerLong() {
    return null;
  }

  protected static TestGeneric<Short>.InnerGeneric<Integer> testGenericInnerInt() {
    return null;
  }

  protected static TestGeneric.NestedNonGeneric testNestedNonGeneric() {
    return null;
  }

  @Test public void genericType() throws Exception {
    Method recursiveEnum = getClass().getDeclaredMethod("generic", Enum[].class);
    TypeName.get(recursiveEnum.getReturnType());
    TypeName.get(recursiveEnum.getGenericReturnType());
    TypeName genericTypeName = TypeName.get(recursiveEnum.getParameterTypes()[0]);
    TypeName.get(recursiveEnum.getGenericParameterTypes()[0]);

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).contains("Enum");
  }

  @Test public void innerClassInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericStringInner");
    TypeName.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeName.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeName.get(genericStringInner.getGenericReturnType()),
        TypeName.get(getClass().getDeclaredMethod("testGenericIntInner").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).contains("String");
  }

  @Test public void innerGenericInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericInnerLong");
    TypeName.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeName.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeName.get(genericStringInner.getGenericReturnType()),
        TypeName.get(getClass().getDeclaredMethod("testGenericInnerInt").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).contains("Long");
    assertThat(genericTypeName.toString()).contains("Short");
  }

  @Test public void innerStaticInGenericType() throws Exception {
    Method staticInGeneric = getClass().getDeclaredMethod("testNestedNonGeneric");
    TypeName.get(staticInGeneric.getReturnType());
    TypeName typeName = TypeName.get(staticInGeneric.getGenericReturnType());
    
    // Make sure there are no generic arguments
    assertThat(typeName.toString()).doesNotContain("<");
  }

  @Test public void equalsAndHashCodePrimitive() {
    assertEqualsHashCodeAndToString(TypeName.BOOLEAN, TypeName.BOOLEAN);
    assertEqualsHashCodeAndToString(TypeName.BYTE, TypeName.BYTE);
    assertEqualsHashCodeAndToString(TypeName.CHAR, TypeName.CHAR);
    assertEqualsHashCodeAndToString(TypeName.DOUBLE, TypeName.DOUBLE);
    assertEqualsHashCodeAndToString(TypeName.FLOAT, TypeName.FLOAT);
    assertEqualsHashCodeAndToString(TypeName.INT, TypeName.INT);
    assertEqualsHashCodeAndToString(TypeName.LONG, TypeName.LONG);
    assertEqualsHashCodeAndToString(TypeName.SHORT, TypeName.SHORT);
    assertEqualsHashCodeAndToString(TypeName.VOID, TypeName.VOID);
  }

  @Test public void equalsAndHashCodeArrayTypeName() {
    assertEqualsHashCodeAndToString(ArrayTypeName.of(Object.class),
        ArrayTypeName.of(Object.class));
    assertEqualsHashCodeAndToString(TypeName.get(Object[].class),
        ArrayTypeName.of(Object.class));
  }

  @Test public void equalsAndHashCodeClassName() {
    assertEqualsHashCodeAndToString(ClassName.get(Object.class), ClassName.get(Object.class));
    assertEqualsHashCodeAndToString(TypeName.get(Object.class), ClassName.get(Object.class));
    assertEqualsHashCodeAndToString(ClassName.bestGuess("java.lang.Object"),
        ClassName.get(Object.class));
  }

  @Test public void equalsAndHashCodeParameterizedTypeName() {
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(Object.class),
        ParameterizedTypeName.get(Object.class));
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(Set.class, UUID.class),
        ParameterizedTypeName.get(Set.class, UUID.class));
    assertNotEquals(ClassName.get(List.class), ParameterizedTypeName.get(List.class,
        String.class));
  }

  @Test public void equalsAndHashCodeTypeVariableName() {
    assertEqualsHashCodeAndToString(TypeVariableName.get(Object.class),
        TypeVariableName.get(Object.class));
    TypeVariableName typeVar1 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeVariableName typeVar2 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    assertEqualsHashCodeAndToString(typeVar1, typeVar2);
  }

  @Test public void equalsAndHashCodeWildcardTypeName() {
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Object.class),
        WildcardTypeName.subtypeOf(Object.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Serializable.class),
        WildcardTypeName.subtypeOf(Serializable.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.supertypeOf(String.class),
        WildcardTypeName.supertypeOf(String.class));
  }

  @Test public void isPrimitive() throws Exception {
    assertThat(TypeName.INT.isPrimitive()).isTrue();
    assertThat(ClassName.get("java.lang", "Integer").isPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "String").isPrimitive()).isFalse();
    assertThat(TypeName.VOID.isPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Void").isPrimitive()).isFalse();
  }

  @Test public void isBoxedPrimitive() throws Exception {
    assertThat(TypeName.INT.isBoxedPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Integer").isBoxedPrimitive()).isTrue();
    assertThat(ClassName.get("java.lang", "String").isBoxedPrimitive()).isFalse();
    assertThat(TypeName.VOID.isBoxedPrimitive()).isFalse();
    assertThat(ClassName.get("java.lang", "Void").isBoxedPrimitive()).isFalse();
  }

  private void assertEqualsHashCodeAndToString(TypeName a, TypeName b) {
    assertEquals(a.toString(), b.toString());
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }
}
