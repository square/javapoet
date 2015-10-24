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

import java.io.Serializable;
import java.rmi.server.UID;
import java.util.Comparator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TypeNameTest {

  @Test public void equalsAndHashCodePrimitive() {
    test(TypeName.BOOLEAN, TypeName.BOOLEAN);
    test(TypeName.BYTE, TypeName.BYTE);
    test(TypeName.CHAR, TypeName.CHAR);
    test(TypeName.DOUBLE, TypeName.DOUBLE);
    test(TypeName.FLOAT, TypeName.FLOAT);
    test(TypeName.INT, TypeName.INT);
    test(TypeName.LONG, TypeName.LONG);
    test(TypeName.SHORT, TypeName.SHORT);
    test(TypeName.VOID, TypeName.VOID);
  }

  @Test public void equalsAndHashCodeArrayTypeName() {
    test(ArrayTypeName.of(Object.class), ArrayTypeName.of(Object.class));
    test(TypeName.get(Object[].class), ArrayTypeName.of(Object.class));
    // ? check(ClassName.bestGuess("java.lang.Object[]"), ArrayTypeName.of(Object.class));
  }

  @Test public void equalsAndHashCodeClassName() {
    test(ClassName.get(Object.class), ClassName.get(Object.class));
    test(TypeName.get(Object.class), ClassName.get(Object.class));
    test(ClassName.bestGuess("java.lang.Object"), ClassName.get(Object.class));
  }
  
  @Test public void equalsAndHashCodeParameterizedTypeName() {
    test(ParameterizedTypeName.get(Object.class), ParameterizedTypeName.get(Object.class));
    test(ParameterizedTypeName.get(Set.class, UID.class), ParameterizedTypeName.get(Set.class, UID.class));
  }
  
  @Test public void equalsAndHashCodeTypeVariableName() {
    test(TypeVariableName.get(Object.class), TypeVariableName.get(Object.class));
    TypeVariableName typeVariable1 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeVariableName typeVariable2 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    test(typeVariable1, typeVariable2);
  }

  @Test public void equalsAndHashCodeWildcardTypeName() {
    test(WildcardTypeName.subtypeOf(Object.class), WildcardTypeName.subtypeOf(Object.class));
    test(WildcardTypeName.subtypeOf(Serializable.class), WildcardTypeName.subtypeOf(Serializable.class));
    test(WildcardTypeName.supertypeOf(String.class), WildcardTypeName.supertypeOf(String.class));
  }

  private void test(TypeName a, TypeName b) {
    Assert.assertEquals(a.toString(), b.toString());
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

}
