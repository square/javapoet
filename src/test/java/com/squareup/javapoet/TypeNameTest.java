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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

public class TypeNameTest {

  protected <E extends Enum<E>> E generic(E[] values) {
    return values[0];
  }

  @Test public void genericType() throws Exception {
    Method recursiveEnum = getClass().getDeclaredMethod("generic", Enum[].class);
    TypeName.get(recursiveEnum.getReturnType());
    TypeName.get(recursiveEnum.getGenericReturnType());
    TypeName.get(recursiveEnum.getParameterTypes()[0]);
    TypeName.get(recursiveEnum.getGenericParameterTypes()[0]);
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

  private void assertEqualsHashCodeAndToString(TypeName a, TypeName b) {
    assertEquals(a.toString(), b.toString());
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }
}
