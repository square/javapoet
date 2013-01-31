/**
 * Copyright (C) 2012 Square, Inc.
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
package com.squareup.protoss.schema;

import com.example.Binding;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public final class JavaWriterTest {
  private final StringWriter stringWriter = new StringWriter();
  private final JavaWriter javaWriter = new JavaWriter(stringWriter);

  @Test public void typeDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", Modifier.PUBLIC | Modifier.FINAL);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "public final class Foo {\n"
        + "}\n");
  }

  @Test public void fieldDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.emitField("java.lang.String", "string", Modifier.PRIVATE | Modifier.STATIC);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  private static String string;\n"
        + "}\n");
  }

  @Test public void fieldDeclarationWithInitialValue() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.emitField("java.lang.String", "string", 0, "\"bar\" + \"baz\"");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  String string = \"bar\" + \"baz\";\n"
        + "}\n");
  }

  @Test public void abstractMethodDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("java.lang.String", "foo", Modifier.ABSTRACT | Modifier.PUBLIC,
        "java.lang.Object", "object", "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  public abstract String foo(Object object, String s);\n"
        + "}\n");
  }

  @Test public void nonAbstractMethodDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("int", "foo", 0, "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void constructorDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod(null, "com.squareup.Foo", Modifier.PUBLIC, "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  public Foo(String s) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void statement() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("int", "foo", 0, "java.lang.String", "s");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    int j = s.length() + 13;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void addImport() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitImports("java.util.ArrayList");
    javaWriter.beginType("com.squareup.Foo", "class", Modifier.PUBLIC | Modifier.FINAL);
    javaWriter.emitField("java.util.ArrayList", "list", 0, "new java.util.ArrayList()");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "import java.util.ArrayList;\n"
        + "\n"
        + "public final class Foo {\n"
        + "  ArrayList list = new java.util.ArrayList();\n"
        + "}\n");
  }

  @Test public void addImportFromSubpackage() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", Modifier.PUBLIC | Modifier.FINAL);
    javaWriter.emitField("com.squareup.bar.Baz", "baz", 0);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "public final class Foo {\n"
        + "  com.squareup.bar.Baz baz;\n"
        + "}\n");
  }

  @Test public void ifControlFlow() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("int", "foo", 0, "java.lang.String", "s");
    javaWriter.beginControlFlow("if (s.isEmpty())");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endControlFlow();
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    if (s.isEmpty()) {\n"
        + "      int j = s.length() + 13;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void doWhileControlFlow() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("int", "foo", 0, "java.lang.String", "s");
    javaWriter.beginControlFlow("do");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endControlFlow("while (s.isEmpty())");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    do {\n"
        + "      int j = s.length() + 13;\n"
        + "    } while (s.isEmpty());\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void tryCatchFinallyControlFlow() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.beginMethod("int", "foo", 0, "java.lang.String", "s");
    javaWriter.beginControlFlow("try");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.nextControlFlow("catch (RuntimeException e)");
    javaWriter.emitStatement("e.printStackTrace()");
    javaWriter.nextControlFlow("finally");
    javaWriter.emitStatement("int k = %s", 13);
    javaWriter.endControlFlow();
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    try {\n"
        + "      int j = s.length() + 13;\n"
        + "    } catch (RuntimeException e) {\n"
        + "      e.printStackTrace();\n"
        + "    } finally {\n"
        + "      int k = 13;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void annotatedType() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitImports("javax.inject.Singleton");
    javaWriter.emitAnnotation("javax.inject.Singleton");
    javaWriter.emitAnnotation(SuppressWarnings.class, JavaWriter.stringLiteral("unchecked"));
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "import javax.inject.Singleton;\n"
        + "\n"
        + "@Singleton\n"
        + "@SuppressWarnings(\"unchecked\")\n"
        + "class Foo {\n"
        + "}\n");
  }

  @Test public void annotatedMember() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.emitAnnotation(Deprecated.class);
    javaWriter.emitField("java.lang.String", "s", 0);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "class Foo {\n"
        + "  @Deprecated\n"
        + "  String s;\n"
        + "}\n");
  }

  @Test public void annotatedWithAttributes() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("overrides", true);
    attributes.put("entryPoints", new Object[] { "entryPointA", "entryPointB", "entryPointC" });
    attributes.put("staticInjections", "com.squareup.Quux");

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Module", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class", 0);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "@Module(\n"
        + "  overrides = true,\n"
        + "  entryPoints = {\n"
        + "    entryPointA,\n"
        + "    entryPointB,\n"
        + "    entryPointC\n"
        + "  },\n"
        + "  staticInjections = com.squareup.Quux\n"
        + ")\n"
        + "class FooModule {\n"
        + "}\n");
  }

  @Test public void parameterizedType() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitImports("java.util.Map", "java.util.Date");
    javaWriter.beginType("com.squareup.Foo", "class", 0);
    javaWriter.emitField("java.util.Map<java.lang.String, java.util.Date>", "map", 0);
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "import java.util.Date;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "class Foo {\n"
        + "  Map<String, Date> map;\n"
        + "}\n");
  }

  @Test public void eolComment() throws IOException {
    javaWriter.emitEndOfLineComment("foo");
    assertCode(""
        + "// foo\n");
  }

  @Test public void javadoc() throws IOException {
    javaWriter.emitJavadoc("foo");
    assertCode(""
        + "/**\n"
        + " * foo\n"
        + " */\n");
  }

  @Test public void multilineJavadoc() throws IOException {
    javaWriter.emitJavadoc("0123456789 0123456789 0123456789 0123456789 0123456789 0123456789\n"
        + "0123456789 0123456789 0123456789 0123456789");
    assertCode(""
        + "/**\n"
        + " * 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789\n"
        + " * 0123456789 0123456789 0123456789 0123456789\n"
        + " */\n");
  }

  @Test public void testStringLiteral() {
    assertThat(JavaWriter.stringLiteral("")).isEqualTo("\"\"");
    assertThat(JavaWriter.stringLiteral("JavaWriter")).isEqualTo("\"JavaWriter\"");
    assertThat(JavaWriter.stringLiteral("\\")).isEqualTo("\"\\\\\"");
    assertThat(JavaWriter.stringLiteral("\"")).isEqualTo("\"\\\"\"");
    assertThat(JavaWriter.stringLiteral("\t")).isEqualTo("\"\\\t\"");
    assertThat(JavaWriter.stringLiteral("\n")).isEqualTo("\"\\\n\"");
  }

  @Test public void compressType() throws IOException {
    javaWriter.emitPackage("blah");
    javaWriter.emitImports(Set.class.getName(), Binding.class.getName());
    String actual = javaWriter.compressType("java.util.Set<com.example.Binding<blah.Foo.Blah>>");
    assertEquals("Set<Binding<Foo.Blah>>", actual);
  }

  @Test public void compressDeeperType() throws IOException {
    javaWriter.emitPackage("blah");
    javaWriter.emitImports(Binding.class.getName());
    String actual = javaWriter.compressType("com.example.Binding<blah.foo.Foo.Blah>");
    assertEquals("Binding<blah.foo.Foo.Blah>", actual);
  }

  private void assertCode(String expected) {
    assertThat(stringWriter.toString()).isEqualTo(expected);
  }
}
