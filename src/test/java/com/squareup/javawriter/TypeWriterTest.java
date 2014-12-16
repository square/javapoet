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

import com.google.common.collect.ImmutableList;
import java.util.concurrent.Executor;
import javax.lang.model.element.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

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

  @Test public void explicitImports() {
    ClassName name = ClassName.create("test", "Top");
    ClassWriter topClass = ClassWriter.forClassName(name);
    topClass.addImport(ClassName.create("other", "Thing"));

    String expected = ""
        + "package test;\n"
        + "\n"
        + "import other.Thing;\n"
        + "\n"
        + "class Top {}\n";
    assertThat(topClass.toString()).isEqualTo(expected);
  }

  @Test public void nestedTypesPropagateOriginatingElements() {
    ClassWriter outer = ClassWriter.forClassName(ClassName.create("test", "Outer"));
    Element outerElement = Mockito.mock(Element.class);
    outer.addOriginatingElement(outerElement);

    ClassWriter middle = outer.addNestedClass("Middle");
    Element middleElement1 = Mockito.mock(Element.class);
    Element middleElement2 = Mockito.mock(Element.class);
    middle.addOriginatingElement(middleElement1, middleElement2);

    ClassWriter inner = middle.addNestedClass("Inner");
    Element innerElement1 = Mockito.mock(Element.class);
    Element innerElement2 = Mockito.mock(Element.class);
    Element innerElement3 = Mockito.mock(Element.class);
    inner.addOriginatingElement(ImmutableList.of(innerElement1, innerElement2, innerElement3));

    assertThat(outer.originatingElements()).containsExactly(outerElement, middleElement1,
        middleElement2, innerElement1, innerElement2, innerElement3);
  }
}
