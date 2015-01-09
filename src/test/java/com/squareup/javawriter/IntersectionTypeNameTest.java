/*
 * Copyright (C) 2014 Square, Inc.
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

import java.io.Serializable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class IntersectionTypeNameTest {
  @Test public void noAnnotations() {
    ClassName runnable = ClassName.fromClass(Runnable.class);
    ClassName serializable = ClassName.fromClass(Serializable.class);
    IntersectionTypeName intersection = IntersectionTypeName.create(runnable, serializable);
    try {
      intersection.annotate(Override.class);
    } catch (UnsupportedOperationException e) {
      assertThat(e.getMessage()).isEqualTo("Cannot annotate intersection type.");
    }
    try {
      intersection.annotate(ClassName.bestGuessFromString("test.Thing"));
    } catch (UnsupportedOperationException e) {
      assertThat(e.getMessage()).isEqualTo("Cannot annotate intersection type.");
    }
  }
}
