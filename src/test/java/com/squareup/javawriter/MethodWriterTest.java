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

import com.google.common.base.Joiner;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class MethodWriterTest {
  @Test public void empty() {
    MethodWriter test = new MethodWriter(VoidName.VOID, "test");
    String actual = Writables.writeToString(test);
    assertThat(actual).isEqualTo("void test() {}\n");
  }

  @Test public void multilineBody() {
    MethodWriter test = new MethodWriter(VoidName.VOID, "test");
    test.body().addSnippet("String firstName;\nString lastName;");
    String actual = Writables.writeToString(test);
    assertThat(actual).isEqualTo(Joiner.on('\n').join(
        "void test() {",
        "  String firstName;",
        "  String lastName;",
        "}\n"
    ));
  }

  @Test public void singleThrowsTypeName() {
    MethodWriter method = new MethodWriter(VoidName.VOID, "test");
    method.addThrowsType(ClassName.fromClass(IOException.class));

    assertThat(Writables.writeToString(method)) //
        .isEqualTo("void test() throws java.io.IOException {}\n");
  }

  @Test public void singleThrowsClass() {
    MethodWriter method = new MethodWriter(VoidName.VOID, "test");
    method.addThrowsType(ClassName.fromClass(IOException.class));

    assertThat(Writables.writeToString(method)) //
        .isEqualTo("void test() throws java.io.IOException {}\n");
  }

  @Test public void throwsWithBody() {
    MethodWriter method = new MethodWriter(PrimitiveName.INT, "test");
    method.addThrowsType(ClassName.fromClass(IOException.class));
    method.body().addSnippet("return 0;");

    assertThat(Writables.writeToString(method)).isEqualTo(Joiner.on('\n').join(
        "int test() throws java.io.IOException {",
        "  return 0;",
        "}\n"
    ));
  }

  @Test public void multipleThrows() {
    MethodWriter method = new MethodWriter(VoidName.VOID, "test");
    method.addThrowsType(IOException.class);
    method.addThrowsType(ClassName.create("example", "ExampleException"));

    assertThat(Writables.writeToString(method)) //
        .isEqualTo("void test() throws java.io.IOException, example.ExampleException {}\n");
  }
}
