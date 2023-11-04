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
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

import javax.lang.model.element.Modifier;

public class FieldSpecTest {
  @Test public void equalsAndHashCode() {
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

  @Test public void nullAnnotationsAddition() {
    try {
      FieldSpec.builder(int.class, "foo").addAnnotations(null);
      fail();
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  @Test public void modifyAnnotations() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
          .addAnnotation(Override.class)
          .addAnnotation(SuppressWarnings.class);

    builder.annotations.remove(1);
    assertThat(builder.build().annotations).hasSize(1);
  }

  @Test public void modifyModifiers() {
    FieldSpec.Builder builder = FieldSpec.builder(int.class, "foo")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    builder.modifiers.remove(1);
    assertThat(builder.build().modifiers).containsExactly(Modifier.PUBLIC);
  }

  @Test
  public void testToBuilder() {
    TypeName fieldType = ClassName.get(String.class);
    String fieldName = "exampleField";
    String javadoc = "This is an example field.";
    AnnotationSpec annotation = AnnotationSpec.builder(Override.class).build();
    Modifier modifier = Modifier.PRIVATE;
    CodeBlock initializer = CodeBlock.of("\"default\"");

    FieldSpec originalField = FieldSpec.builder(fieldType, fieldName, modifier)
            .addJavadoc(javadoc)
            .addAnnotation(annotation)
            .initializer(initializer)
            .build();

    FieldSpec.Builder builder = originalField.toBuilder();
    FieldSpec rebuiltField = builder.build();
    assertEquals(originalField.type, rebuiltField.type);
    assertEquals(originalField.name, rebuiltField.name);
    assertEquals(originalField.javadoc.toString(), rebuiltField.javadoc.toString());
    assertEquals(originalField.annotations, rebuiltField.annotations);
    assertEquals(originalField.modifiers, rebuiltField.modifiers);
    assertEquals(originalField.initializer.toString(), rebuiltField.initializer.toString());
  }

  @Test
  public void testAddJavadocWithCodeBlock() {
    TypeName fieldType = ClassName.get(String.class);
    String fieldName = "exampleField";
    String originalJavadoc = "This is an example field.";
    CodeBlock originalJavadocCodeBlock = CodeBlock.builder().add(originalJavadoc).build();
    String updatedJavadoc = "Updated description.";
    CodeBlock updatedJavadocCodeBlock = CodeBlock.builder().add(updatedJavadoc).build();
    FieldSpec.Builder builder = FieldSpec.builder(fieldType, fieldName)
            .addJavadoc(originalJavadocCodeBlock);
    assertEquals(originalJavadocCodeBlock, builder.build().javadoc);
    builder.addJavadoc(updatedJavadocCodeBlock);
    CodeBlock expectedJavadoc = CodeBlock.builder()
            .add("$L$L", originalJavadoc, updatedJavadoc)
            .build();
    assertEquals(expectedJavadoc, builder.build().javadoc);
  }

}
