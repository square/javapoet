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
import javax.lang.model.element.Modifier;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class EnumWriterTest {
  @Test public void onlyTopLevelClassNames() {
    ClassName name = ClassName.bestGuessFromString("test.Foo.Bar");
    try {
      EnumWriter.forClassName(name);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("test.Foo.Bar must be top-level type.");
    }
  }

  @Test public void constantsAreRequired() {
    EnumWriter enumWriter = EnumWriter.forClassName(ClassName.create("test", "Test"));
    try {
      Writables.writeToString(enumWriter);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Cannot write an enum with no constants.");
    }
  }

  @Test public void constantsAreIndented() {
    EnumWriter enumWriter = EnumWriter.forClassName(ClassName.create("test", "Test"));
    enumWriter.addConstant("HELLO");
    enumWriter.addConstant("WORLD");

    String expected = ""
        + "package test;\n"
        + "\n"
        + "enum Test {\n"
        + "  HELLO,\n"
        + "  WORLD;\n"
        + "}\n";
    assertThat(enumWriter.toString()).isEqualTo(expected);
  }

  @Test public void constantsWithConstructorArguments() {
    EnumWriter enumWriter = EnumWriter.forClassName(ClassName.create("test", "Test"));
    enumWriter.addConstant("HELLO").addArgument(Snippet.format("\"Hello\""));
    enumWriter.addConstant("WORLD").addArgument(Snippet.format("\"World!\""));

    FieldWriter valueWriter = enumWriter.addField(String.class, "value");
    valueWriter.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    ConstructorWriter constructorWriterWriter = enumWriter.addConstructor();
    constructorWriterWriter.addModifiers(Modifier.PRIVATE);
    constructorWriterWriter.addParameter(String.class, "value");
    constructorWriterWriter.body().addSnippet("this.value = value;");

    assertThat(enumWriter.toString()).isEqualTo(""
        + "package test;\n"
        + "\n"
        + "enum Test {\n"
        + "  HELLO(\"Hello\"),\n"
        + "  WORLD(\"World!\");\n"
        + "\n"
        + "  public final String value;\n"
        + "\n"
        + "  private Test(String value) {\n"
        + "    this.value = value;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void constantsWithClassBody() {
    EnumWriter enumWriter = EnumWriter.forClassName(ClassName.create("test", "Test"));

    EnumWriter.ConstantWriter helloWriter = enumWriter.addConstant("HELLO");
    MethodWriter helloToStringWriter = helloWriter.addMethod(String.class, "toString");
    helloToStringWriter.annotate(Override.class);
    helloToStringWriter.addModifiers(Modifier.PUBLIC);
    helloToStringWriter.body().addSnippet("return \"Hello\";");

    EnumWriter.ConstantWriter worldWriter = enumWriter.addConstant("WORLD");
    MethodWriter worldToStringWriter = worldWriter.addMethod(String.class, "toString");
    worldToStringWriter.annotate(Override.class);
    worldToStringWriter.addModifiers(Modifier.PUBLIC);
    worldToStringWriter.body().addSnippet("return \"World!\";");

    assertThat(enumWriter.toString()).isEqualTo(""
        + "package test;\n"
        + "\n"
        + "enum Test {\n"
        + "  HELLO {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"Hello\";\n"
        + "    }\n"
        + "  },\n"
        + "  WORLD {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"World!\";\n"
        + "    }\n"
        + "  };\n"
        + "}\n");
  }
}
