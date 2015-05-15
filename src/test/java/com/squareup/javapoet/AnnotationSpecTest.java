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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.lang.model.element.TypeElement;

import org.junit.Rule;
import org.junit.Test;

import com.google.testing.compile.CompilationRule;

public final class AnnotationSpecTest {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationA {
  }

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationB {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationC {
    String value();
  }

  static enum Breakfast {
    WAFFLES, PANCAKES
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface HasDefaultsAnnotation {

    byte a() default 5;

    short b() default 6;

    int c() default 7;

    long d() default 8;

    float e() default 9.0f;

    double f() default 10.0;

    char g() default 'k';

    boolean h() default true;

    Breakfast i() default Breakfast.WAFFLES;

    AnnotationA j() default @AnnotationA();

    String k() default "maple";

    Class<? extends Annotation> l() default AnnotationB.class;

    int[] m() default { 1, 2, 3 };

    Breakfast[] n() default { Breakfast.WAFFLES, Breakfast.PANCAKES };

    Breakfast o();

    int p();

    AnnotationC q() default @AnnotationC("foo");

    Class<? extends Number>[] r() default { Byte.class, Short.class, Integer.class, Long.class };

  }

  @HasDefaultsAnnotation(
      o = Breakfast.PANCAKES,
      p = 1701,
      f = 11.1,
      m = { 9, 8, 1 },
      l = Override.class,
      j = @AnnotationA,
      q = @AnnotationC("bar"),
      r = { Float.class, Double.class })
  public class IsAnnotated {
    // empty
  }

  @Rule
  public final CompilationRule compilation = new CompilationRule();

  @Test
  public void testHasDefaultAnnotation() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));
    assertThat(annotation.toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "o = com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", p = 1701"
            + ", f = 11.1"
            + ", m = {9, 8, 1}"
            + ", l = java.lang.Override.class"
            + ", j = @com.squareup.javapoet.AnnotationSpecTest.AnnotationA"
            + ", q = @com.squareup.javapoet.AnnotationSpecTest.AnnotationC(\"bar\")"
            + ", r = {java.lang.Float.class, java.lang.Double.class}"
            + ")");
  }

  @Test
  public void testHasDefaultAnnotationWithImport() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(IsAnnotated.class.getSimpleName());
    typeBuilder.addAnnotation(annotation);
    JavaFile file = JavaFile.builder("com.squareup.javapoet", typeBuilder.build()).build();
    assertThat(file.toString()).isEqualTo(
        "package com.squareup.javapoet;\n"
            + "\n"
            + "import java.lang.Double;\n"
            + "import java.lang.Float;\n"
            + "import java.lang.Override;\n"
            + "\n"
            + "@AnnotationSpecTest.HasDefaultsAnnotation(\n"
            + "    o = AnnotationSpecTest.Breakfast.PANCAKES,\n"
            + "    p = 1701,\n"
            + "    f = 11.1,\n"
            + "    m = {\n        9,\n        8,\n        1\n    },\n" // TODO beautify
            + "    l = Override.class,\n"
            + "    j = @AnnotationSpecTest.AnnotationA,\n"
            + "    q = @AnnotationSpecTest.AnnotationC(\"bar\"),\n"
            + "    r = {\n        Float.class,\n        Double.class\n    }\n" // TODO beautify
            + ")\n"
            + "class IsAnnotated {\n"
            + "}\n");
  }

  @Test
  public void testEmptyArray() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$L", "{}");
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation(" + "n = {}" + ")");
    builder.addMember("m", "$L", "{}");
    assertThat(builder.build().toString())
        .isEqualTo(
            "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
                + "n = {}, m = {}"
                + ")");
  }

  @Test
  public void testDynamicArrayOfEnumConstants() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES);
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ")");

    // builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES);
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder = builder.build().toBuilder(); // idempotent
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES);
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", com.squareup.javapoet.AnnotationSpecTest.Breakfast.WAFFLES"
            + "})");
  }

  @Test
  public void testHasDefaultAnnotationToBuilder() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec.Builder builder = AnnotationSpec.get(element.getAnnotationMirrors().get(0))
        .toBuilder();
    builder.addMember("m", "$L", 123);
    assertThat(builder.build().toString()).isEqualTo(
        "@com.squareup.javapoet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "o = com.squareup.javapoet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", p = 1701"
            + ", f = 11.1"
            + ", m = {9, 8, 1, 123}"
            + ", l = java.lang.Override.class"
            + ", j = @com.squareup.javapoet.AnnotationSpecTest.AnnotationA"
            + ", q = @com.squareup.javapoet.AnnotationSpecTest.AnnotationC(\"bar\")"
            + ", r = {java.lang.Float.class, java.lang.Double.class}"
            + ")");
  }
}
