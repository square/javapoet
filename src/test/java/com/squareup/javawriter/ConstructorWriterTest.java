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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class ConstructorWriterTest {
  @Test public void empty() {
    ConstructorWriter test = new ConstructorWriter("Test");
    String actual = Writables.writeToString(test);
    assertThat(actual).isEqualTo("Test() {\n}\n");
  }

  @Test public void multilineBody() {
    ConstructorWriter test = new ConstructorWriter("Test");
    test.body().addSnippet("String firstName;\nString lastName;");
    String actual = Writables.writeToString(test);
    assertThat(actual).isEqualTo(Joiner.on('\n').join(
        "Test() {",
        "  String firstName;",
        "  String lastName;",
        "}\n"
    ));
  }
}
