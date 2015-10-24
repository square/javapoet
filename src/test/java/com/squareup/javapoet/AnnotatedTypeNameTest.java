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
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class AnnotatedTypeNameTest {

  private final static String NN = NeverNull.class.getCanonicalName();

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
    TypeName annotated = simpleString.annotated(AnnotationSpec.builder(NeverNull.class).build());
    assertTrue(annotated.isAnnotated());
    assertEquals(simpleString, annotated.annotated());
    assertFalse(annotated.annotated().isAnnotated());
  }

  @Test public void annotatedType() {
    String expected = "@" + NN + " java.lang.String";
    AnnotationSpec annotation = AnnotationSpec.builder(NeverNull.class).build();
    TypeName type = TypeName.get(String.class);
    String actual = type.annotated(annotation).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedParameterizedType() {
    String expected = "@" + NN + " java.util.List<java.lang.String>";
    AnnotationSpec annotation = AnnotationSpec.builder(NeverNull.class).build();
    TypeName type = ParameterizedTypeName.get(List.class, String.class);
    String actual = type.annotated(annotation).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedArgumentOfParameterizedType() {
    String expected = "java.util.List<@" + NN + " java.lang.String>";
    AnnotationSpec annotation = AnnotationSpec.builder(NeverNull.class).build();
    TypeName type = TypeName.get(String.class).annotated(annotation);
    ClassName list = ClassName.get(List.class);
    String actual = ParameterizedTypeName.get(list, type).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedWildcardTypeNameWithSuper() {
    String expected = "? super @" + NN + " java.lang.String";
    AnnotationSpec annotation = AnnotationSpec.builder(NeverNull.class).build();
    TypeName type = TypeName.get(String.class).annotated(annotation);
    String actual = WildcardTypeName.supertypeOf(type).toString();
    assertEquals(expected, actual);
  }

  @Test public void annotatedWildcardTypeNameWithExtends() {
    String expected = "? extends @" + NN + " java.lang.String";
    AnnotationSpec annotation = AnnotationSpec.builder(NeverNull.class).build();
    TypeName type = TypeName.get(String.class).annotated(annotation);
    String actual = WildcardTypeName.subtypeOf(type).toString();
    assertEquals(expected, actual);
  }

}
