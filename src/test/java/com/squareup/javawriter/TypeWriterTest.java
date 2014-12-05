/*
 * Copyright (C) 2014 Google, Inc.
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

import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TypeWriterTest {
  @Test public void referencedAndDeclaredSimpleName() {
    ClassName name = ClassName.create("test", "Top");
    ClassWriter topClass = ClassWriter.forClassName(name);
    topClass.addNestedClass("Middle").addNestedClass("Bottom");
    topClass.addField(ClassName.create("some.other.pkg", "Bottom"), "field");
    assertThat(topClass.toString()).doesNotContain("import some.other.pkg.Bottom;");
  }

  @Test public void zeroImportsSingleNewline() {
    ClassName name = ClassName.create("test", "Top");
    ClassWriter classWriter = ClassWriter.forClassName(name);

    String expected = ""
        + "package test;\n"
        + "\n"
        + "class Top {}\n";
    assertThat(classWriter.toString()).isEqualTo(expected);
  }

  @Test public void newlineBetweenImports() {
    ClassName name = ClassName.create("test", "Top");
    ClassWriter topClass = ClassWriter.forClassName(name);
    topClass.addField(Executor.class, "executor");

    String expected = ""
        + "package test;\n"
        + "\n"
        + "import java.util.concurrent.Executor;\n"
        + "\n"
        + "class Top {\n"
        + "  Executor executor;\n"
        + "}\n";
    assertThat(topClass.toString()).isEqualTo(expected);
  }
}
