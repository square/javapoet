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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;

public class AnnotatedTypeNameTest {

  private final static String NN = NeverNull.class.getCanonicalName();
  private final AnnotationSpec NEVER_NULL = AnnotationSpec.builder(NeverNull.class).build();

  public @interface NeverNull {}

  @Test(expected=NullPointerException.class) public void nullAnnotationArray() {
    TypeName.BOOLEAN.annotated((AnnotationSpec[]) null);
  }

  @Test(expected=NullPointerException.class) public void nullAnnotationList() {
    TypeName.DOUBLE.annotated((List<AnnotationSpec>) null);
  }

  @Test public void annotated() {
    TypeName simpleString = TypeName.get(String.class);
    assertFalse(simpleString.isAnnotated());
    assertEquals(simpleString, TypeName.get(String.class));
    TypeName annotated = simpleString.annotated(NEVER_NULL);
    assertTrue(annotated.isAnnotated());
    assertEquals(annotated, annotated.annotated());
  }

  @Test public void annotatedType() {
    String expected = "@" + NN + " java.lang.String";
    TypeName type = TypeName.get(String.class);
    String actual = type.annotated(NEVER_NULL).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedTwice() {
    String expected = "@" + NN + " @java.lang.Override java.lang.String";
    TypeName type = TypeName.get(String.class);
    String actual =
        type.annotated(NEVER_NULL)
            .annotated(AnnotationSpec.builder(Override.class).build())
            .toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedParameterizedType() {
    String expected = "@" + NN + " java.util.List<java.lang.String>";
    TypeName type = ParameterizedTypeName.get(List.class, String.class);
    String actual = type.annotated(NEVER_NULL).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedArgumentOfParameterizedType() {
    String expected = "java.util.List<@" + NN + " java.lang.String>";
    TypeName type = TypeName.get(String.class).annotated(NEVER_NULL);
    ClassName list = ClassName.get(List.class);
    String actual = ParameterizedTypeName.get(list, type).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedWildcardTypeNameWithSuper() {
    String expected = "? super @" + NN + " java.lang.String";
    TypeName type = TypeName.get(String.class).annotated(NEVER_NULL);
    String actual = WildcardTypeName.supertypeOf(type).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedWildcardTypeNameWithExtends() {
    String expected = "? extends @" + NN + " java.lang.String";
    TypeName type = TypeName.get(String.class).annotated(NEVER_NULL);
    String actual = WildcardTypeName.subtypeOf(type).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedEquivalence() {
    annotatedEquivalence(TypeName.VOID);
    annotatedEquivalence(ArrayTypeName.get(Object[].class));
    annotatedEquivalence(ClassName.get(Object.class));
    annotatedEquivalence(ParameterizedTypeName.get(List.class, Object.class));
    annotatedEquivalence(TypeVariableName.get(Object.class));
    annotatedEquivalence(WildcardTypeName.get(Object.class));
  }

  private void annotatedEquivalence(TypeName type) {
    assertFalse(type.isAnnotated());
    assertEquals(type, type);
    assertEquals(type.annotated(NEVER_NULL), type.annotated(NEVER_NULL));
    assertNotEquals(type, type.annotated(NEVER_NULL));
    assertEquals(type.hashCode(), type.hashCode());
    assertEquals(type.annotated(NEVER_NULL).hashCode(), type.annotated(NEVER_NULL).hashCode());
    assertNotEquals(type.hashCode(), type.annotated(NEVER_NULL).hashCode());
  }

  // https://github.com/square/javapoet/issues/431
  // @Target(ElementType.TYPE_USE) requires Java 1.8
  public @interface TypeUseAnnotation {}

  // https://github.com/square/javapoet/issues/431
  @Ignore @Test public void annotatedNestedType() {
    String expected = "java.util.Map.@" + TypeUseAnnotation.class.getCanonicalName() + " Entry";
    AnnotationSpec typeUseAnnotation = AnnotationSpec.builder(TypeUseAnnotation.class).build();
    TypeName type = TypeName.get(Map.Entry.class).annotated(typeUseAnnotation);
    String actual = type.toString();
    assertEquals(expected, actual);
  }

  // https://github.com/square/javapoet/issues/431
  @Ignore @Test public void annotatedNestedParameterizedType() {
    String expected = "java.util.Map.@" + TypeUseAnnotation.class.getCanonicalName()
        + " Entry<java.lang.Byte, java.lang.Byte>";
    AnnotationSpec typeUseAnnotation = AnnotationSpec.builder(TypeUseAnnotation.class).build();
    TypeName type = ParameterizedTypeName.get(Map.Entry.class, Byte.class, Byte.class)
        .annotated(typeUseAnnotation);
    String actual = type.toString();
    assertEquals(expected, actual);
  }
}
