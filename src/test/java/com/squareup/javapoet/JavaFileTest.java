/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.javapoet;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.lang.model.element.Modifier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class JavaFileTest {
  @Test public void importStaticReadmeExample() {
    ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
    ClassName namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards");
    ClassName list = ClassName.get("java.util", "List");
    ClassName arrayList = ClassName.get("java.util", "ArrayList");
    TypeName listOfHoverboards = ParameterizedTypeName.get(list, hoverboard);
    MethodSpec beyond = MethodSpec.methodBuilder("beyond")
        .returns(listOfHoverboards)
        .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
        .addStatement("result.add($T.createNimbus(2000))", hoverboard)
        .addStatement("result.add($T.createNimbus(\"2001\"))", hoverboard)
        .addStatement("result.add($T.createNimbus($T.THUNDERBOLT))", hoverboard, namedBoards)
        .addStatement("$T.sort(result)", Collections.class)
        .addStatement("return result.isEmpty() ? $T.emptyList() : result", Collections.class)
        .build();
    TypeSpec hello = TypeSpec.classBuilder("HelloWorld")
        .addMethod(beyond)
        .build();
    JavaFile example = JavaFile.builder("com.example.helloworld", hello)
        .addStaticImport(hoverboard, "createNimbus")
        .addStaticImport(namedBoards, "*")
        .addStaticImport(Collections.class, "*")
        .build();
    assertThat(example.toString()).isEqualTo(""
        + "package com.example.helloworld;\n"
        + "\n"
        + "import static com.mattel.Hoverboard.Boards.*;\n"
        + "import static com.mattel.Hoverboard.createNimbus;\n"
        + "import static java.util.Collections.*;\n"
        + "\n"
        + "import com.mattel.Hoverboard;\n"
        + "import java.util.ArrayList;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class HelloWorld {\n"
        + "  List<Hoverboard> beyond() {\n"
        + "    List<Hoverboard> result = new ArrayList<>();\n"
        + "    result.add(createNimbus(2000));\n"
        + "    result.add(createNimbus(\"2001\"));\n"
        + "    result.add(createNimbus(THUNDERBOLT));\n"
        + "    sort(result);\n"
        + "    return result.isEmpty() ? emptyList() : result;\n"
        + "  }\n"
        + "}\n");
  }
  @Test public void importStaticForCrazyFormatsWorks() {
    MethodSpec method = MethodSpec.methodBuilder("method").build();
    JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("$T", Runtime.class)
                .addStatement("$T.a()", Runtime.class)
                .addStatement("$T.X", Runtime.class)
                .addStatement("$T$T", Runtime.class, Runtime.class)
                .addStatement("$T.$T", Runtime.class, Runtime.class)
                .addStatement("$1T$1T", Runtime.class)
                .addStatement("$1T$2L$1T", Runtime.class, "?")
                .addStatement("$1T$2L$2S$1T", Runtime.class, "?")
                .addStatement("$1T$2L$2S$1T$3N$1T", Runtime.class, "?", method)
                .addStatement("$T$L", Runtime.class, "?")
                .addStatement("$T$S", Runtime.class, "?")
                .addStatement("$T$N", Runtime.class, method)
                .build())
            .build())
        .addStaticImport(Runtime.class, "*")
        .build()
        .toString(); // don't look at the generated code...
  }

  @Test public void importStaticMixed() {
    JavaFile source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("assert $1T.valueOf(\"BLOCKED\") == $1T.BLOCKED", Thread.State.class)
                .addStatement("$T.gc()", System.class)
                .addStatement("$1T.out.println($1T.nanoTime())", System.class)
                .build())
            .addMethod(MethodSpec.constructorBuilder()
                .addParameter(Thread.State[].class, "states")
                .varargs(true)
                .build())
            .build())
        .addStaticImport(Thread.State.BLOCKED)
        .addStaticImport(System.class, "*")
        .addStaticImport(Thread.State.class, "valueOf")
        .build();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import static java.lang.System.*;\n"
        + "import static java.lang.Thread.State.BLOCKED;\n"
        + "import static java.lang.Thread.State.valueOf;\n"
        + "\n"
        + "import java.lang.Thread;\n"
        + "\n"
        + "class Taco {\n"
        + "  static {\n"
        + "    assert valueOf(\"BLOCKED\") == BLOCKED;\n"
        + "    gc();\n"
        + "    out.println(nanoTime());\n"
        + "  }\n"
        + "\n"
        + "  Taco(Thread.State... states) {\n"
        + "  }\n"
        + "}\n");
  }

  @Ignore("addStaticImport doesn't support members with $L")
  @Test public void importStaticDynamic() {
    JavaFile source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addMethod(MethodSpec.methodBuilder("main")
                .addStatement("$T.$L.println($S)", System.class, "out", "hello")
                .build())
            .build())
        .addStaticImport(System.class, "out")
        .build();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import static java.lang.System.out;\n"
        + "\n"
        + "class Taco {\n"
        + "  void main() {\n"
        + "    out.println(\"hello\");\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void importStaticNone() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .build().toString()).isEqualTo(""
        + "package readme;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "import java.util.concurrent.TimeUnit;\n"
        + "\n"
        + "class Util {\n"
        + "  public static long minutesToSeconds(long minutes) {\n"
        + "    System.gc();\n"
        + "    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES);\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void importStaticOnce() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .build().toString()).isEqualTo(""
        + "package readme;\n"
        + "\n"
        + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "import java.util.concurrent.TimeUnit;\n"
        + "\n"
        + "class Util {\n"
        + "  public static long minutesToSeconds(long minutes) {\n"
        + "    System.gc();\n"
        + "    return SECONDS.convert(minutes, TimeUnit.MINUTES);\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void importStaticTwice() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .addStaticImport(TimeUnit.MINUTES)
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import static java.util.concurrent.TimeUnit.MINUTES;\n"
            + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
            + "\n"
            + "import java.lang.System;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    System.gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  @Test public void importStaticUsingWildcards() {
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.class, "*")
        .addStaticImport(System.class, "*")
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import static java.lang.System.*;\n"
            + "import static java.util.concurrent.TimeUnit.*;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  private TypeSpec importStaticTypeSpec(String name) {
    MethodSpec method = MethodSpec.methodBuilder("minutesToSeconds")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(long.class)
        .addParameter(long.class, "minutes")
        .addStatement("$T.gc()", System.class)
        .addStatement("return $1T.SECONDS.convert(minutes, $1T.MINUTES)", TimeUnit.class)
        .build();
    return TypeSpec.classBuilder(name).addMethod(method).build();

  }
  @Test public void noImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void singleImport() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "}\n");
  }

  @Test public void conflictingImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .addField(ClassName.get("java.sql", "Date"), "madeFreshDatabaseDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "\n"
        + "  java.sql.Date madeFreshDatabaseDate;\n"
        + "}\n");
  }

  @Test public void skipJavaLangImportsWithConflictingClassLast() throws Exception {
    // Whatever is used first wins! In this case the Float in java.lang is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float litres;\n"
        + "\n"
        + "  com.squareup.soda.Float beverage;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  @Test public void skipJavaLangImportsWithConflictingClassFirst() throws Exception {
    // Whatever is used first wins! In this case the Float in com.squareup.soda is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.soda.Float;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float beverage;\n"
        + "\n"
        + "  java.lang.Float litres;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  @Test public void conflictingParentName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class Twin {\n"
        + "    }\n"
        + "\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void conflictingChildName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Twin").build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "\n"
        + "      class Twin {\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void conflictingNameOutOfScope() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Nested")
                        .addType(TypeSpec.classBuilder("Twin").build())
                        .build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      Twin.D d;\n"
        + "\n"
        + "      class Nested {\n"
        + "        class Twin {\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void nestedClassAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName.get("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.wire.Message;\n"
        + "\n"
        + "class Taco extends Message {\n"
        + "  class Builder extends Message.Builder {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void classAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco extends com.taco.bell.Taco {\n"
        + "}\n");
  }

  @Test public void conflictingAnnotation() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addAnnotation(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@com.taco.bell.Taco\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void conflictingAnnotationReferencedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addAnnotation(AnnotationSpec.builder(ClassName.get("com.squareup.tacos", "MyAnno"))
                .addMember("value", "$T.class", ClassName.get("com.taco.bell", "Taco"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@MyAnno(com.taco.bell.Taco.class)\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void conflictingTypeVariableBound() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addTypeVariable(
                TypeVariableName.get("T", ClassName.get("com.taco.bell", "Taco")))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco<T extends com.taco.bell.Taco> {\n"
        + "}\n");
  }

  @Test public void superclassReferencesSelf() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ParameterizedTypeName.get(
                ClassName.get(Comparable.class), ClassName.get("com.squareup.tacos", "Taco")))
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Comparable;\n"
        + "\n"
        + "class Taco extends Comparable<Taco> {\n"
        + "}\n");
  }

  /** https://github.com/square/javapoet/issues/366 */
  @Test public void annotationIsNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName.get("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName.get("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import dagger.Component;\n"
        + "\n"
        + "@Component\n"
        + "class TestComponent {\n"
        + "  @Component.Builder\n"
        + "  class Builder {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultPackage() throws Exception {
    String source = JavaFile.builder("",
        TypeSpec.classBuilder("HelloWorld")
            .addMethod(MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .addCode("$T.out.println($S);\n", System.class, "Hello World!")
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "import java.lang.String;\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class HelloWorld {\n"
        + "  public static void main(String[] args) {\n"
        + "    System.out.println(\"Hello World!\");\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultPackageTypesAreNotImported() throws Exception {
    String source = JavaFile.builder("hello",
          TypeSpec.classBuilder("World").addSuperinterface(ClassName.get("", "Test")).build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package hello;\n"
        + "\n"
        + "class World implements Test {\n"
        + "}\n");
  }

  @Test public void topOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated $L by JavaPoet. DO NOT EDIT!", "2015-01-13")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "// Generated 2015-01-13 by JavaPoet. DO NOT EDIT!\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void emptyLinesInTopOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "//\n"
        + "// GENERATED FILE:\n"
        + "//\n"
        + "// DO NOT EDIT!\n"
        + "//\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void packageClassConflictsWithNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  com.squareup.tacos.A a;\n"
        + "\n"
        + "  class A {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void packageClassConflictsWithSuperlass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "A"))
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco extends com.taco.bell.A {\n"
        + "  A a;\n"
        + "}\n");
  }
}
