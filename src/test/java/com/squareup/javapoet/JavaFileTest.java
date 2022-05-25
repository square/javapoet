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

import java.io.*;

import com.google.testing.compile.CompilationRule;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class JavaFileTest {

  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

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
    String LS = getLineSeparator();
    assertThat(example.toString()).isEqualTo(""
        + "package com.example.helloworld;" + LS
        + "" + LS
        + "import static com.mattel.Hoverboard.Boards.*;" + LS
        + "import static com.mattel.Hoverboard.createNimbus;" + LS
        + "import static java.util.Collections.*;" + LS
        + "" + LS
        + "import com.mattel.Hoverboard;" + LS
        + "import java.util.ArrayList;" + LS
        + "import java.util.List;" + LS
        + "" + LS
        + "class HelloWorld {" + LS
        + "  List<Hoverboard> beyond() {" + LS
        + "    List<Hoverboard> result = new ArrayList<>();" + LS
        + "    result.add(createNimbus(2000));" + LS
        + "    result.add(createNimbus(\"2001\"));" + LS
        + "    result.add(createNimbus(THUNDERBOLT));" + LS
        + "    sort(result);" + LS
        + "    return result.isEmpty() ? emptyList() : result;" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import static java.lang.System.*;" + LS
        + "import static java.lang.Thread.State.BLOCKED;" + LS
        + "import static java.lang.Thread.State.valueOf;" + LS
        + "" + LS
        + "import java.lang.Thread;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  static {" + LS
        + "    assert valueOf(\"BLOCKED\") == BLOCKED;" + LS
        + "    gc();" + LS
        + "    out.println(nanoTime());" + LS
        + "  }" + LS
        + "" + LS
        + "  Taco(Thread.State... states) {" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source.toString()).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import static java.lang.System.out;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  void main() {" + LS
        + "    out.println(\"hello\");" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void importStaticNone() {
    String LS = getLineSeparator();
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .build().toString()).isEqualTo(""
        + "package readme;" + LS
        + "" + LS
        + "import java.lang.System;" + LS
        + "import java.util.concurrent.TimeUnit;" + LS
        + "" + LS
        + "class Util {" + LS
        + "  public static long minutesToSeconds(long minutes) {" + LS
        + "    System.gc();" + LS
        + "    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES);" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void importStaticOnce() {
    String LS = getLineSeparator();
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .build().toString()).isEqualTo(""
        + "package readme;" + LS
        + "" + LS
        + "import static java.util.concurrent.TimeUnit.SECONDS;" + LS
        + "" + LS
        + "import java.lang.System;" + LS
        + "import java.util.concurrent.TimeUnit;" + LS
        + "" + LS
        + "class Util {" + LS
        + "  public static long minutesToSeconds(long minutes) {" + LS
        + "    System.gc();" + LS
        + "    return SECONDS.convert(minutes, TimeUnit.MINUTES);" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void importStaticTwice() {
    String LS = getLineSeparator();
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .addStaticImport(TimeUnit.MINUTES)
        .build().toString()).isEqualTo(""
            + "package readme;" + LS
            + "" + LS
            + "import static java.util.concurrent.TimeUnit.MINUTES;" + LS
            + "import static java.util.concurrent.TimeUnit.SECONDS;" + LS
            + "" + LS
            + "import java.lang.System;" + LS
            + "" + LS
            + "class Util {" + LS
            + "  public static long minutesToSeconds(long minutes) {" + LS
            + "    System.gc();" + LS
            + "    return SECONDS.convert(minutes, MINUTES);" + LS
            + "  }" + LS
            + "}" + LS);
  }

  @Test public void importStaticUsingWildcards() {
    String LS = getLineSeparator();
    assertThat(JavaFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.class, "*")
        .addStaticImport(System.class, "*")
        .build().toString()).isEqualTo(""
            + "package readme;" + LS
            + "" + LS
            + "import static java.lang.System.*;" + LS
            + "import static java.util.concurrent.TimeUnit.*;" + LS
            + "" + LS
            + "class Util {" + LS
            + "  public static long minutesToSeconds(long minutes) {" + LS
            + "    gc();" + LS
            + "    return SECONDS.convert(minutes, MINUTES);" + LS
            + "  }" + LS
            + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "}" + LS);
  }

  @Test public void singleImport() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import java.util.Date;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  Date madeFreshDate;" + LS
        + "}" + LS);
  }

  @Test public void conflictingImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .addField(ClassName.get("java.sql", "Date"), "madeFreshDatabaseDate")
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import java.util.Date;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  Date madeFreshDate;" + LS
        + "" + LS
        + "  java.sql.Date madeFreshDatabaseDate;" + LS
        + "}" + LS);
  }

  @Test public void annotatedTypeParam() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ParameterizedTypeName.get(ClassName.get(List.class),
                ClassName.get("com.squareup.meat", "Chorizo")
                    .annotated(AnnotationSpec.builder(ClassName.get("com.squareup.tacos", "Spicy"))
                        .build())), "chorizo")
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import com.squareup.meat.Chorizo;" + LS
        + "import java.util.List;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  List<@Spicy Chorizo> chorizo;" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  Float litres;" + LS
        + "" + LS
        + "  com.squareup.soda.Float beverage;" + LS // Second 'Float' is fully qualified.
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import com.squareup.soda.Float;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  Float beverage;" + LS
        + "" + LS
        + "  java.lang.Float litres;" + LS // Second 'Float' is fully qualified.
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class A {" + LS
        + "  class B {" + LS
        + "    class Twin {" + LS
        + "    }" + LS
        + "" + LS
        + "    class C {" + LS
        + "      A.Twin.D d;" + LS
        + "    }" + LS
        + "  }" + LS
        + "" + LS
        + "  class Twin {" + LS
        + "    class D {" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class A {" + LS
        + "  class B {" + LS
        + "    class C {" + LS
        + "      A.Twin.D d;" + LS
        + "" + LS
        + "      class Twin {" + LS
        + "      }" + LS
        + "    }" + LS
        + "  }" + LS
        + "" + LS
        + "  class Twin {" + LS
        + "    class D {" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class A {" + LS
        + "  class B {" + LS
        + "    class C {" + LS
        + "      Twin.D d;" + LS
        + "" + LS
        + "      class Nested {" + LS
        + "        class Twin {" + LS
        + "        }" + LS
        + "      }" + LS
        + "    }" + LS
        + "  }" + LS
        + "" + LS
        + "  class Twin {" + LS
        + "    class D {" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import com.squareup.wire.Message;" + LS
        + "" + LS
        + "class Taco extends Message {" + LS
        + "  class Builder extends Message.Builder {" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void classAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco extends com.taco.bell.Taco {" + LS
        + "}" + LS);
  }

  @Test public void conflictingAnnotation() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addAnnotation(ClassName.get("com.taco.bell", "Taco"))
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "@com.taco.bell.Taco" + LS
        + "class Taco {" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "@MyAnno(com.taco.bell.Taco.class)" + LS
        + "class Taco {" + LS
        + "}" + LS);
  }

  @Test public void conflictingTypeVariableBound() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addTypeVariable(
                TypeVariableName.get("T", ClassName.get("com.taco.bell", "Taco")))
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco<T extends com.taco.bell.Taco> {" + LS
        + "}" + LS);
  }

  @Test public void superclassReferencesSelf() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ParameterizedTypeName.get(
                ClassName.get(Comparable.class), ClassName.get("com.squareup.tacos", "Taco")))
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import java.lang.Comparable;" + LS
        + "" + LS
        + "class Taco extends Comparable<Taco> {" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import dagger.Component;" + LS
        + "" + LS
        + "@Component" + LS
        + "class TestComponent {" + LS
        + "  @Component.Builder" + LS
        + "  class Builder {" + LS
        + "  }" + LS
        + "}" + LS);
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
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "import java.lang.String;" + LS
        + "import java.lang.System;" + LS
        + "" + LS
        + "class HelloWorld {" + LS
        + "  public static void main(String[] args) {" + LS
        + "    System.out.println(\"Hello World!\");" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void defaultPackageTypesAreNotImported() throws Exception {
    String source = JavaFile.builder("hello",
          TypeSpec.classBuilder("World").addSuperinterface(ClassName.get("", "Test")).build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package hello;" + LS
        + "" + LS
        + "class World implements Test {" + LS
        + "}" + LS);
  }

  @Test public void topOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated $L by JavaPoet. DO NOT EDIT!", "2015-01-13")
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "// Generated 2015-01-13 by JavaPoet. DO NOT EDIT!" + LS
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "}" + LS);
  }

  @Test public void emptyLinesInTopOfFileComment() throws Exception {
    String LS = getLineSeparator();
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "//" + LS
        + "// GENERATED FILE:" + LS
        + "//" + LS
        + "// DO NOT EDIT!" + LS
        + "//" + LS
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "}" + LS);
  }

  @Test public void packageClassConflictsWithNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  com.squareup.tacos.A a;" + LS
        + "" + LS
        + "  class A {" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void packageClassConflictsWithSuperlass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.taco.bell", "A"))
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco extends com.taco.bell.A {" + LS
        + "  A a;" + LS
        + "}" + LS);
  }

  @Test public void modifyStaticImports() throws Exception {
    JavaFile.Builder builder = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .build())
            .addStaticImport(File.class, "separator");

    builder.staticImports.clear();
    builder.staticImports.add(File.class.getCanonicalName() + ".separatorChar");

    String source = builder.build().toString();

    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import static java.io.File.separatorChar;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "}" + LS);
  }

  @Test public void alwaysQualifySimple() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Thread.class, "thread")
            .alwaysQualify("Thread")
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  java.lang.Thread thread;" + LS
        + "}" + LS);
  }

  @Test public void alwaysQualifySupersedesJavaLangImports() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Thread.class, "thread")
            .alwaysQualify("Thread")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  java.lang.Thread thread;" + LS
        + "}" + LS);
  }

  @Test public void avoidClashesWithNestedClasses_viaClass() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .avoidClashesWithNestedClasses(Foo.class)
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import other.Foo;" + LS
        + "import other.NestedTypeC;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  other.NestedTypeA nestedA;" + LS
        + "" + LS
        + "  other.NestedTypeB nestedB;" + LS
        + "" + LS
        + "  NestedTypeC nestedC;" + LS
        + "" + LS
        + "  Foo foo;" + LS
        + "}" + LS);
  }

  @Test public void avoidClashesWithNestedClasses_viaTypeElement() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .avoidClashesWithNestedClasses(getElement(Foo.class))
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;" + LS
        + "" + LS
        + "import other.Foo;" + LS
        + "import other.NestedTypeC;" + LS
        + "" + LS
        + "class Taco {" + LS
        + "  other.NestedTypeA nestedA;" + LS
        + "" + LS
        + "  other.NestedTypeB nestedB;" + LS
        + "" + LS
        + "  NestedTypeC nestedC;" + LS
        + "" + LS
        + "  Foo foo;" + LS
        + "}" + LS);
  }

  @Test public void avoidClashesWithNestedClasses_viaSuperinterfaceType() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            // These two should get qualified
            .addField(ClassName.get("other", "NestedTypeA"), "nestedA")
            .addField(ClassName.get("other", "NestedTypeB"), "nestedB")
            // This one shouldn't since it's not a nested type of Foo
            .addField(ClassName.get("other", "NestedTypeC"), "nestedC")
            // This one shouldn't since we only look at nested types
            .addField(ClassName.get("other", "Foo"), "foo")
            .addType(TypeSpec.classBuilder("NestedTypeA").build())
            .addType(TypeSpec.classBuilder("NestedTypeB").build())
            .addSuperinterface(FooInterface.class)
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.tacos;" + LS
        + "" + LS
        + "import com.squareup.javapoet.JavaFileTest;" + LS
        + "import other.Foo;" + LS
        + "import other.NestedTypeC;" + LS
        + "" + LS
        + "class Taco implements JavaFileTest.FooInterface {" + LS
        + "  other.NestedTypeA nestedA;" + LS
        + "" + LS
        + "  other.NestedTypeB nestedB;" + LS
        + "" + LS
        + "  NestedTypeC nestedC;" + LS
        + "" + LS
        + "  Foo foo;" + LS
        + "" + LS
        + "  class NestedTypeA {" + LS
        + "  }" + LS
        + "" + LS
        + "  class NestedTypeB {" + LS
        + "  }" + LS
        + "}" + LS);
  }

  static class Foo {
    static class NestedTypeA {

    }
    static class NestedTypeB {

    }
  }

  interface FooInterface {
    class NestedTypeA {

    }
    class NestedTypeB {

    }
  }

  private TypeSpec.Builder childTypeBuilder() {
    return TypeSpec.classBuilder("Child")
        .addMethod(MethodSpec.methodBuilder("optionalString")
            .returns(ParameterizedTypeName.get(Optional.class, String.class))
            .addStatement("return $T.empty()", Optional.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("pattern")
            .returns(Pattern.class)
            .addStatement("return null")
            .build());
  }

  @Test
  public void avoidClashes_parentChild_superclass_type() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().superclass(Parent.class).build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.javapoet;" + LS
        + "" + LS
        + "import java.lang.String;" + LS
        + "" + LS
        + "class Child extends JavaFileTest.Parent {" + LS
        + "  java.util.Optional<String> optionalString() {" + LS
        + "    return java.util.Optional.empty();" + LS
        + "  }" + LS
        + "" + LS
        + "  java.util.regex.Pattern pattern() {" + LS
        + "    return null;" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test
  public void avoidClashes_parentChild_superclass_typeMirror() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().superclass(getElement(Parent.class).asType()).build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.javapoet;" + LS
        + "" + LS
        + "import java.lang.String;" + LS
        + "" + LS
        + "class Child extends JavaFileTest.Parent {" + LS
        + "  java.util.Optional<String> optionalString() {" + LS
        + "    return java.util.Optional.empty();" + LS
        + "  }" + LS
        + "" + LS
        + "  java.util.regex.Pattern pattern() {" + LS
        + "    return null;" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test
  public void avoidClashes_parentChild_superinterface_type() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().addSuperinterface(ParentInterface.class).build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.javapoet;" + LS
        + "" + LS
        + "import java.lang.String;" + LS
        + "import java.util.regex.Pattern;" + LS
        + "" + LS
        + "class Child implements JavaFileTest.ParentInterface {" + LS
        + "  java.util.Optional<String> optionalString() {" + LS
        + "    return java.util.Optional.empty();" + LS
        + "  }" + LS
        + "" + LS
        + "  Pattern pattern() {" + LS
        + "    return null;" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test
  public void avoidClashes_parentChild_superinterface_typeMirror() {
    String source = JavaFile.builder("com.squareup.javapoet",
        childTypeBuilder().addSuperinterface(getElement(ParentInterface.class).asType()).build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.javapoet;" + LS
        + "" + LS
        + "import java.lang.String;" + LS
        + "import java.util.regex.Pattern;" + LS
        + "" + LS
        + "class Child implements JavaFileTest.ParentInterface {" + LS
        + "  java.util.Optional<String> optionalString() {" + LS
        + "    return java.util.Optional.empty();" + LS
        + "  }" + LS
        + "" + LS
        + "  Pattern pattern() {" + LS
        + "    return null;" + LS
        + "  }" + LS
        + "}" + LS);
  }

  // Regression test for https://github.com/square/javapoet/issues/77
  // This covers class and inheritance
  static class Parent implements ParentInterface {
    static class Pattern {

    }
  }

  interface ParentInterface {
    class Optional {

    }
  }

  // Regression test for case raised here: https://github.com/square/javapoet/issues/77#issuecomment-519972404
  @Test
  public void avoidClashes_mapEntry() {
    String source = JavaFile.builder("com.squareup.javapoet",
        TypeSpec.classBuilder("MapType")
            .addMethod(MethodSpec.methodBuilder("optionalString")
                .returns(ClassName.get("com.foo", "Entry"))
                .addStatement("return null")
                .build())
            .addSuperinterface(Map.class)
            .build())
        .build()
        .toString();
    String LS = getLineSeparator();
    assertThat(source).isEqualTo("package com.squareup.javapoet;" + LS
        + "" + LS
        + "import java.util.Map;" + LS
        + "" + LS
        + "class MapType implements Map {" + LS
        + "  com.foo.Entry optionalString() {" + LS
        + "    return null;" + LS
        + "  }" + LS
        + "}" + LS);
  }

  @Test public void testLineSeparator() {
    String string = "abc";
    String LS = string + getLineSeparator();
    assertThat(JavaFile.addLineSeparator(string)).isEqualTo(LS);
  }

  @Test public void importHelloWorldDemo() {
    MethodSpec main = MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(void.class)
            .addParameter(String[].class, "args")
            .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
            .build();
    TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(main)
            .build();
    JavaFile example = JavaFile.builder("", helloWorld)
            .build();

    String LS = getLineSeparator();

    assertThat(example.toString()).isEqualTo(
            "import java.lang.String;" + LS
                    + "import java.lang.System;" + LS
                    + "" + LS
                    + "public final class HelloWorld {" + LS
                    + "  public static void main(String[] args) {" + LS
                    + "    System.out.println(\"Hello, JavaPoet!\");" + LS
                    +   "  }" + LS
                    +   "}" + LS);
  }

  public String getLineSeparator(){
    Properties props = System.getProperties();
    String osName = props.getProperty("os.name");
    String LS;
    if (osName.contains("Windows")){
      LS = "\r\n";
    }else if (osName.contains("Linux")){
      LS = "\n";
    }else if (osName.contains("Mac OS X")){
      LS = "\n";
    }else if (osName.contains("Mac OS")){
      LS = "\r";
    }else{
      LS = "\n";
    }
    return LS;
  }
}
