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
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.STATIC;

@RunWith(JUnit4.class)
public final class InterfaceWriterTest {
  @Test public void onlyTopLevelClassNames() {
    ClassName name = ClassName.bestGuessFromString("test.Foo.Bar");
    try {
      InterfaceWriter.forClassName(name);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("test.Foo.Bar must be top-level type.");
    }
  }

  @Test public void memberOrdering() {
    ClassName name = ClassName.create("example", "Test");
    InterfaceWriter writer = InterfaceWriter.forClassName(name);

    writer.addField(String.class, "ONE").addModifiers(STATIC);
    writer.addMethod(VoidName.VOID, "two").addModifiers(STATIC);
    writer.addMethod(VoidName.VOID, "four").addModifiers(ABSTRACT);

    String expected = Joiner.on('\n').join(
        "package example;",
        "",
        "class Test {",
        "  static String ONE;",
        "",
        "  static void two() {}",
        "",
        "  void four();",
        "}"
    );
    assertThat(writer.toString()).isEqualTo(expected);
  }
}
