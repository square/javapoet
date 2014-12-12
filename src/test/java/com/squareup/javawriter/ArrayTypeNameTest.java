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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class ArrayTypeNameTest {
  @Test public void oneDimension() {
    ClassName string = ClassName.fromClass(String.class);
    ArrayTypeName arrayTypeName = ArrayTypeName.create(string);
    assertThat(Writables.writeToString(arrayTypeName)).isEqualTo("java.lang.String[]");
  }

  @Test public void manyDimensions() {
    ClassName string = ClassName.fromClass(String.class);
    ArrayTypeName arrayTypeName = ArrayTypeName.create(string);
    arrayTypeName = ArrayTypeName.create(arrayTypeName);
    arrayTypeName = ArrayTypeName.create(arrayTypeName);
    arrayTypeName = ArrayTypeName.create(arrayTypeName);
    assertThat(Writables.writeToString(arrayTypeName)).isEqualTo("java.lang.String[][][][]");
  }

  @Test public void annotated() {
    ClassName string = ClassName.fromClass(String.class);
    ArrayTypeName arrayTypeName = ArrayTypeName.create(string);

    ClassName nonNull = ClassName.create("test", "NonNull");
    arrayTypeName.annotate(nonNull);

    assertThat(Writables.writeToString(arrayTypeName)).isEqualTo("@test.NonNull java.lang.String[]");
  }
}
