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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import javax.lang.model.element.Modifier;

public class ParameterSpecTest {
  @Test public void equalsAndHashCode() {
    ParameterSpec a = ParameterSpec.builder(int.class, "foo").build();
    ParameterSpec b = ParameterSpec.builder(int.class, "foo").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
    a = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
    b = ParameterSpec.builder(int.class, "i").addModifiers(Modifier.STATIC).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test public void nullAnnotationsAddition() {
    try {
      ParameterSpec.builder(int.class, "foo").addAnnotations(null);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  @Test public void addNonFinalModifier() {
    List<Modifier> modifiers = new ArrayList<>();
    modifiers.add(Modifier.FINAL);
    modifiers.add(Modifier.PUBLIC);

    try {
      ParameterSpec.builder(int.class, "foo").addModifiers(modifiers);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("unexpected parameter modifier: public");
    }
  }
}
