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
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

@RunWith(JUnit4.class)
public final class EnumWriterTest {
  @Test public void onlyTopLevelClassNames() {
    ClassName name = ClassName.bestGuessFromString("test.Foo.Bar");
    try {
      EnumWriter.forClassName(name);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("test.Foo.Bar must be top-level type.");
    }
  }

  @Test public void memberOrdering() {
    ClassName name = ClassName.create("example", "Test");
    EnumWriter writer = EnumWriter.forClassName(name);

    writer.addConstant("TEST");
    writer.addConstructor().addModifiers(PRIVATE);
    writer.addField(String.class, "ONE").addModifiers(STATIC);
    writer.addField(String.class, "three");
    writer.addMethod(VoidName.VOID, "two").addModifiers(STATIC);
    writer.addMethod(VoidName.VOID, "four");

    String expected = Joiner.on('\n').join(
        "package example;",
        "",
        "enum Test {",
        "  TEST;",
        "",
        "  static String ONE;",
        "",
        "  static void two() {}",
        "",
        "  String three;",
        "",
        "  private Test() {}",
        "",
        "  void four() {}",
        "}"
    );
    assertThat(writer.toString()).isEqualTo(expected);
  }
}
