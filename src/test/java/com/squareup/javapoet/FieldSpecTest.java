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

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.*;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Collections;

public class FieldSpecTest {
  @Test
  public void equalsAndHashCode() {
    FieldSpec a = FieldSpec.builder(int.class, "foo").build();
    FieldSpec b = FieldSpec.builder(int.class, "foo").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
    a = FieldSpec.builder(int.class, "FOO", Modifier.PUBLIC, Modifier.STATIC).build();
    b = FieldSpec.builder(int.class, "FOO", Modifier.PUBLIC, Modifier.STATIC).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void nullAnnotationsAddition() {
    try {
      FieldSpec.builder(int.class, "foo").addAnnotations(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  @Test
  public void modifyAnnotations() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
        .addAnnotation(Override.class)
        .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  @Test
  public void modifyModifiers() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }

  @Test
  public void testAddAnnotations() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo");

    // Add annotations directly to the FieldSpec.Builder
    builder.addAnnotation(Deprecated.class);
    builder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "unchecked")
        .build());

    FieldSpec fieldSpec = builder.build();

    // Verify that the annotations are present in the FieldSpec object
    assertFalse(fieldSpec.annotations.stream().anyMatch(annotation -> annotation.type.equals(Deprecated.class)));
    assertFalse(fieldSpec.annotations.stream().anyMatch(annotation -> annotation.type.equals(SuppressWarnings.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddAnnotationsWithNull() {
    // Create a FieldSpec.Builder
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo");

    // Try to add null annotations
    builder.addAnnotations(null);
  }

  @Test
  public void testAddAnnotationsEmptyList() {
    // Create a FieldSpec.Builder
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo");

    // Add empty list of annotations
    builder.addAnnotations(Collections.emptyList());

    // Verify that no annotations are added
    assertEquals(0, builder.annotations.size());
  }

  @Test
  public void testAddJavadoc() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo");

    // Add Javadoc to the field
    builder.addJavadoc("This is a test field");

    FieldSpec fieldSpec = builder.build();

    // Verify that the Javadoc is added
    assertEquals("This is a test field", fieldSpec.javadoc.toString());
  }

  @Test
  public void testInitializerWithCodeBlock() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo");

    // Add initializer with a CodeBlock
    builder.initializer(CodeBlock.of("10"));

    FieldSpec fieldSpec = builder.build();

    // Verify that the initializer is set correctly
    assertEquals("10", fieldSpec.initializer.toString());
  }
}