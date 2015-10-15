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

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.State.NEW;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import javax.lang.model.element.Modifier;
import org.junit.Test;
import com.squareup.javapoet.MemberRef.Kind;

public class MemberRefTest {
  @Test public void readmeExampleWithThreadStateNew() {
    CodeBlock block = CodeBlock.builder().add("$R", NEW).build();
    assertThat(block.toString()).isEqualTo("java.lang.Thread.State.NEW");
  }

  @Test public void readmeExampleWithTimeUnitConvert() throws Exception {
    Method convert = TimeUnit.class.getMethod("convert", long.class, TimeUnit.class);
    MethodSpec.Builder method = MethodSpec.methodBuilder("minutesToSeconds")
        .returns(long.class)
        .addParameter(long.class, "minutes")
        .addStatement("$R()", System.class.getMethod("gc"))
        .addStatement("return $R.$R(minutes, $R)", TimeUnit.SECONDS, convert, TimeUnit.MINUTES);
    String unit = "java.util.concurrent.TimeUnit";
    assertThat(method.build().toString()).isEqualTo(""
        + "long minutesToSeconds(long minutes) {\n"
        + "  java.lang.System.gc();\n"
        + "  return " + unit + ".SECONDS.convert(minutes, " + unit + ".MINUTES);\n"
        + "}\n");
  }

  @Test public void readmeExampleWithMethodChaining() throws Exception {
    Method builder = CodeBlock.class.getMethod("builder");
    Method add = CodeBlock.Builder.class.getMethod("add", String.class, Object[].class);
    Method build = CodeBlock.Builder.class.getMethod("build");
    CodeBlock block = CodeBlock.builder()
        .add("$R().$R(\"$$R\", $R).$R()", builder, add, NEW, build)
        .build();
    assertThat(block.toString()).isEqualTo(""
        + "com.squareup.javapoet.CodeBlock.builder()"
        + ".add(\"$R\", java.lang.Thread.State.NEW)"
        + ".build()");
  }

  @Test public void equals() throws Exception {
    MemberRef expected = MemberRef.get(NEW);
    assertThat(expected.kind).isEqualTo(Kind.ENUM);
    String name = "NEW";
    assertThat(expected).isEqualTo(MemberRef.get(NEW));
    assertThat(expected).isEqualTo(MemberRef.get(
        Kind.ENUM,
        ClassName.get(Thread.State.class),
        name,
        EnumSet.of(Modifier.PUBLIC, Modifier.STATIC)));
    name = "serialVersionUID";
    expected = MemberRef.get(String.class.getDeclaredField(name));
    assertThat(expected.kind).isEqualTo(Kind.FIELD);
    assertThat(expected).isEqualTo(MemberRef.get(String.class.getDeclaredField(name)));
    assertThat(expected).isEqualTo(MemberRef.get(
        Kind.FIELD,
        ClassName.get(String.class),
        name,
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)));
    name = "valueOf";
    expected = MemberRef.get(String.class.getMethod(name, int.class));
    assertThat(expected.kind).isEqualTo(Kind.METHOD);
    assertThat(expected).isEqualTo(MemberRef.get(String.class.getMethod(name, int.class)));
    assertThat(expected).isEqualTo(MemberRef.get(
        Kind.METHOD,
        ClassName.get(String.class),
        name,
        EnumSet.of(Modifier.PUBLIC, Modifier.STATIC)));
  }

  @Test public void importStaticNone() throws Exception {
    assertThat(JavaFile.builder("readme", typeSpec("Util"))
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

  @Test public void importStaticOnce() throws Exception {
    MemberRef seconds = MemberRef.get(TimeUnit.SECONDS);
    assertThat(JavaFile.builder("readme", typeSpec("Util"))
        .addStaticImport(seconds)
        .build().toString()).isEqualTo(""
        + "package readme;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "import java.util.concurrent.TimeUnit;\n"
        + "\n"
        + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
        + "\n"
        + "class Util {\n"
        + "  public static long minutesToSeconds(long minutes) {\n"
        + "    System.gc();\n"
        + "    return SECONDS.convert(minutes, TimeUnit.MINUTES);\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void importStaticTwice() throws Exception {
    MemberRef seconds = MemberRef.get(TimeUnit.SECONDS);
    MemberRef minutes = MemberRef.get(TimeUnit.MINUTES);
    assertThat(JavaFile.builder("readme", typeSpec("Util"))
        .addStaticImport(seconds, minutes)
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import java.lang.System;\n"
            + "\n"
            + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
            + "import static java.util.concurrent.TimeUnit.MINUTES;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    System.gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  @Test public void importStaticTwiceAndGC() throws Exception {
    MemberRef seconds = MemberRef.get(TimeUnit.SECONDS);
    MemberRef minutes = MemberRef.get(TimeUnit.MINUTES);
    MemberRef gc = MemberRef.get(System.class.getMethod("gc"));
    assertThat(JavaFile.builder("readme", typeSpec("Util"))
        .addStaticImport(seconds, minutes, gc)
        .build().toString()).isEqualTo(""
            + "package readme;\n"
            + "\n"
            + "import static java.util.concurrent.TimeUnit.SECONDS;\n"
            + "import static java.util.concurrent.TimeUnit.MINUTES;\n"
            + "import static java.lang.System.gc;\n"
            + "\n"
            + "class Util {\n"
            + "  public static long minutesToSeconds(long minutes) {\n"
            + "    gc();\n"
            + "    return SECONDS.convert(minutes, MINUTES);\n"
            + "  }\n"
            + "}\n");
  }

  TypeSpec typeSpec(String name) {
    try {
      Method convert = TimeUnit.class.getMethod("convert", long.class, TimeUnit.class);
      MethodSpec method = MethodSpec.methodBuilder("minutesToSeconds")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(long.class)
          .addParameter(long.class, "minutes")
          .addStatement("$R()", System.class.getMethod("gc"))
          .addStatement("return $R.$R(minutes, $R)", TimeUnit.SECONDS, convert, TimeUnit.MINUTES)
          .build();
      return TypeSpec.classBuilder(name).addMethod(method).build();
    } catch (Exception e) {
      throw new RuntimeException("");
    }
  }
}
