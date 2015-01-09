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

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class ParameterizedTypeNameTest {
  @Test public void oneParameter() {
    ClassName string = ClassName.fromClass(String.class);
    ParameterizedTypeName list = ParameterizedTypeName.create(List.class, string);

    assertThat(Writables.writeToString(list)).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test public void manyParameters() {
    ClassName table = ClassName.create("example", "Table");
    ClassName string = ClassName.fromClass(String.class);
    ParameterizedTypeName list = ParameterizedTypeName.create(table, string, string, string);

    assertThat(Writables.writeToString(list))
        .isEqualTo("example.Table<java.lang.String, java.lang.String, java.lang.String>");
  }

  @Test public void annotated() {
    ClassName string = ClassName.fromClass(String.class);
    ParameterizedTypeName list = ParameterizedTypeName.create(List.class, string);

    ClassName nonNull = ClassName.create("test", "NonNull");
    list.annotate(nonNull);

    assertThat(Writables.writeToString(list))
        .isEqualTo("@test.NonNull java.util.List<java.lang.String>");
  }
}
