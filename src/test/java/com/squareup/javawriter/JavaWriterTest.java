// Copyright 2013 Square, Inc.
package com.squareup.javawriter;

import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.junit.Test;

import com.example.Binding;

public final class JavaWriterTest {
  private final StringWriter stringWriter = new StringWriter();
  private final JavaWriter javaWriter = new JavaWriter(stringWriter);

  @Test public void typeDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public final class Foo {\n"
        + "}\n");
  }

  @Test public void enumDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "enum", EnumSet.of(PUBLIC));
    javaWriter.emitEnumValue("BAR");
    javaWriter.emitEnumValue("BAZ");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public enum Foo {\n"
        + "  BAR,\n"
        + "  BAZ,\n"
        + "}\n");
  }

  @Test public void enumDeclarationWithMethod() throws IOException{
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "enum", EnumSet.of(PUBLIC));
    javaWriter.emitEnumValues(Arrays.asList("BAR", "BAZ"));
    javaWriter.beginMethod("void", "foo", EnumSet.of(PUBLIC));
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public enum Foo {\n"
        + "  BAR,\n"
        + "  BAZ;\n"
        + "  public void foo() {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void fieldDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.emitField("java.lang.String", "string", EnumSet.of(PRIVATE, STATIC));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  private static String string;\n"
        + "}\n");
  }

  @Test public void fieldDeclarationWithInitialValue() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.emitField("java.lang.String", "string", EnumSet.noneOf(Modifier.class),
        "\"bar\" + \"baz\"");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  String string = \"bar\" + \"baz\";\n"
        + "}\n");
  }

  @Test public void abstractMethodDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("java.lang.String", "foo", EnumSet.of(ABSTRACT, PUBLIC),
        "java.lang.Object", "object", "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  public abstract String foo(Object object, String s);\n"
        + "}\n");
  }

  @Test public void abstractMethodDeclarationWithThrows() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("java.lang.String", "foo", EnumSet.of(ABSTRACT, PUBLIC),
        Arrays.asList("java.lang.Object", "object", "java.lang.String", "s"),
        Arrays.asList("java.io.IOException"));
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  public abstract String foo(Object object, String s)\n"
        + "      throws java.io.IOException;\n"
        + "}\n");
  }

  @Test public void nonAbstractMethodDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void nonAbstractMethodDeclarationWithThrows() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class),
        Arrays.asList("java.lang.String", "s"), Arrays.asList("java.io.IOException"));
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  int foo(String s)\n"
        + "      throws java.io.IOException {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void constructorDeclaration() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod(null, "com.squareup.Foo", EnumSet.of(PUBLIC), "java.lang.String", "s");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  public Foo(String s) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void constructorDeclarationWithThrows() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod(null, "com.squareup.Foo", EnumSet.of(PUBLIC),
        Arrays.asList("java.lang.String", "s"), Arrays.asList("java.io.IOException"));
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  public Foo(String s)\n"
        + "      throws java.io.IOException {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void statement() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    int j = s.length() + 13;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void statementPrecededByComment() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
    javaWriter.emitSingleLineComment("foo");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  int foo(String s) {\n"
        + "    // foo\n"
        + "    int j = s.length() + 13;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void multiLineStatement() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Triangle", "class");
    javaWriter.beginMethod("double", "pythagorean", EnumSet.noneOf(Modifier.class),
        "int", "a", "int", "b");
    javaWriter.emitStatement("int cSquared = a * a\n+ b * b");
    javaWriter.emitStatement("return Math.sqrt(cSquared)");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Triangle {\n"
        + "  double pythagorean(int a, int b) {\n"
        + "    int cSquared = a * a\n"
        + "        + b * b;\n"
        + "    return Math.sqrt(cSquared);\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void addImport() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitImports("java.util.ArrayList");
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.emitField("java.util.ArrayList", "list", EnumSet.noneOf(Modifier.class),
        "new java.util.ArrayList()");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "import java.util.ArrayList;\n"
        + "public final class Foo {\n"
        + "  ArrayList list = new java.util.ArrayList();\n"
        + "}\n");
  }

  @Test public void addStaticImport() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitStaticImports("java.lang.System.getProperty");
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.emitField("String", "bar", EnumSet.noneOf(Modifier.class), "getProperty(\"bar\")");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "import static java.lang.System.getProperty;\n"
        + "public final class Foo {\n"
        + "  String bar = getProperty(\"bar\");\n"
        + "}\n");
  }

  @Test public void addStaticWildcardImport() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitStaticImports("java.lang.System.*");
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.emitField("String", "bar", EnumSet.noneOf(Modifier.class), "getProperty(\"bar\")");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "import static java.lang.System.*;\n"
        + "public final class Foo {\n"
        + "  String bar = getProperty(\"bar\");\n"
        + "}\n");
  }

  @Test public void emptyImports() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitImports(Collections.<String>emptyList());
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public final class Foo {\n"
        + "}\n");
  }

  @Test public void emptyStaticImports() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.emitStaticImports(Collections.<String>emptyList());
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public final class Foo {\n"
        + "}\n");
  }

  @Test public void addImportFromSubpackage() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class", EnumSet.of(PUBLIC, FINAL));
    javaWriter.emitField("com.squareup.bar.Baz", "baz");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "public final class Foo {\n"
        + "  com.squareup.bar.Baz baz;\n"
        + "}\n");
  }

  @Test public void ifControlFlow() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
    javaWriter.beginControlFlow("if (s.isEmpty())");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endControlFlow();
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
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
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
    javaWriter.beginControlFlow("do");
    javaWriter.emitStatement("int j = s.length() + %s", 13);
    javaWriter.endControlFlow("while (s.isEmpty())");
    javaWriter.endMethod();
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
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
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.beginMethod("int", "foo", EnumSet.noneOf(Modifier.class), "java.lang.String", "s");
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
        + "\n"
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
    javaWriter.emitAnnotation(SuppressWarnings.class,
        JavaWriter.stringLiteral("unchecked"));
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "import javax.inject.Singleton;\n"
        + "@Singleton\n"
        + "@SuppressWarnings(\"unchecked\")\n"
        + "class Foo {\n"
        + "}\n");
  }

  @Test public void annotatedMember() throws IOException {
    javaWriter.emitPackage("com.squareup");
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.emitAnnotation(Deprecated.class);
    javaWriter.emitField("java.lang.String", "s");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "class Foo {\n"
        + "  @Deprecated\n"
        + "  String s;\n"
        + "}\n");
  }

  @Test public void annotatedWithSingleAttribute() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("overrides", true);

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Module", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class", EnumSet.noneOf(Modifier.class));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "@Module(overrides = true)\n"
        + "class FooModule {\n"
        + "}\n");
  }

  @Test public void annotatedWithSingleValueAttribute() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("value", stringLiteral("blah.Generator"));

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Generated", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class", EnumSet.noneOf(Modifier.class));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "@Generated(\"blah.Generator\")\n"
        + "class FooModule {\n"
        + "}\n");
  }

  @Test public void annotatedWithTwoNonArrayAttributes() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("overrides", true);
    attributes.put("foo", "bar");

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Module", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class", EnumSet.noneOf(Modifier.class));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "@Module(overrides = true, foo = bar)\n"
        + "class FooModule {\n"
        + "}\n");
  }

  @Test public void annotatedWithThreeNonArrayAttributes() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("overrides", true);
    attributes.put("foo", "bar");
    attributes.put("bar", "baz");

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Module", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class", EnumSet.noneOf(Modifier.class));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "@Module(overrides = true, foo = bar, bar = baz)\n"
        + "class FooModule {\n"
        + "}\n");
  }

  @Test public void annotatedWithAttributes() throws IOException {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.put("overrides", true);
    attributes.put("entryPoints", new Object[] { "entryPointA", "entryPointB", "entryPointC" });
    attributes.put("staticInjections", "com.squareup.Quux");

    javaWriter.emitPackage("com.squareup");
    javaWriter.emitAnnotation("Module", attributes);
    javaWriter.beginType("com.squareup.FooModule", "class");
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
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
    javaWriter.beginType("com.squareup.Foo", "class");
    javaWriter.emitField("java.util.Map<java.lang.String, java.util.Date>", "map",
        EnumSet.noneOf(Modifier.class));
    javaWriter.endType();
    assertCode(""
        + "package com.squareup;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "import java.util.Map;\n"
        + "class Foo {\n"
        + "  Map<String, Date> map;\n"
        + "}\n");
  }

  @Test public void eolComment() throws IOException {
    javaWriter.emitSingleLineComment("foo");
    assertCode("// foo\n");
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
    assertThat(JavaWriter.stringLiteral("\b")).isEqualTo("\"\\b\"");
    assertThat(JavaWriter.stringLiteral("\t")).isEqualTo("\"\\t\"");
    assertThat(JavaWriter.stringLiteral("\n")).isEqualTo("\"\\n\"");
    assertThat(JavaWriter.stringLiteral("\f")).isEqualTo("\"\\f\"");
    assertThat(JavaWriter.stringLiteral("\r")).isEqualTo("\"\\r\"");

    // Control characters
    for (char i = 0x1; i <= 0x1f; i++) {
      checkCharEscape(i);
    }
    for (char i = 0x7f; i <= 0x9f; i++) {
      checkCharEscape(i);
    }
  }

  private void checkCharEscape(char codePoint) {
    String test = "" + codePoint;
    String expected;
    switch (codePoint) {
      case 8: expected = "\"\\b\""; break;
      case 9: expected = "\"\\t\""; break;
      case 10: expected = "\"\\n\""; break;
      case 12: expected = "\"\\f\""; break;
      case 13: expected = "\"\\r\""; break;
      default: expected = "\"\\u" + String.format("%04x", (int) codePoint) + "\"";
    }
    assertThat(JavaWriter.stringLiteral(test)).isEqualTo(expected);
  }

  @Test public void testType() {
    assertThat(JavaWriter.type(String.class)).as("simple type").isEqualTo("java.lang.String");
    assertThat(JavaWriter.type(Set.class)).as("raw type").isEqualTo("java.util.Set");
    assertThat(JavaWriter.type(Set.class, "?")).as("wildcard type").isEqualTo("java.util.Set<?>");
    assertThat(JavaWriter.type(Map.class, JavaWriter.type(String.class), "?"))
        .as("mixed type and wildcard generic type parameters")
        .isEqualTo("java.util.Map<java.lang.String, ?>");
    try {
      JavaWriter.type(String.class, "foo");
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (Throwable e) {
      assertThat(e).as("parameterized non-generic").isInstanceOf(IllegalArgumentException.class);
    }
    try {
      JavaWriter.type(Map.class, "foo");
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (Throwable e) {
      assertThat(e).as("too few type arguments").isInstanceOf(IllegalArgumentException.class);
    }
    try {
      JavaWriter.type(Set.class, "foo", "bar");
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (Throwable e) {
      assertThat(e).as("too many type arguments").isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test public void compressType() throws IOException {
    javaWriter.emitPackage("blah");
    javaWriter.emitImports(Set.class.getCanonicalName(), Binding.class.getCanonicalName());
    String actual = javaWriter.compressType("java.util.Set<com.example.Binding<blah.Foo.Blah>>");
    assertThat(actual).isEqualTo("Set<Binding<Foo.Blah>>");
  }

  @Test public void compressDeeperType() throws IOException {
    javaWriter.emitPackage("blah");
    javaWriter.emitImports(Binding.class.getCanonicalName());
    String actual = javaWriter.compressType("com.example.Binding<blah.foo.Foo.Blah>");
    assertThat(actual).isEqualTo("Binding<blah.foo.Foo.Blah>");
  }

  @Test public void compressWildcardType() throws IOException {
    javaWriter.emitPackage("blah");
    javaWriter.emitImports(Binding.class.getCanonicalName());
    String actual = javaWriter.compressType("com.example.Binding<? extends blah.Foo.Blah>");
    assertThat(actual).isEqualTo("Binding<? extends Foo.Blah>");
  }

  @Test public void compressSimpleNameCollisionInSamePackage() throws IOException {
    javaWriter.emitPackage("denominator");
    javaWriter.emitImports("javax.inject.Provider", "dagger.internal.Binding");
    String actual = javaWriter.compressType("dagger.internal.Binding<denominator.Provider>");
    assertThat(actual).isEqualTo("Binding<denominator.Provider>");
  }

  private void assertCode(String expected) {
    assertThat(stringWriter.toString()).isEqualTo(expected);
  }
}
