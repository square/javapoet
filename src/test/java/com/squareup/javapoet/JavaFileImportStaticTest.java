/*
 * Copyright (C) 2016 Square, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class) public class JavaFileImportStaticTest {

  private static Object[] args(Object... args) {
    return args;
  }

  @Parameters(name = "{index}: {0}") public static Iterable<Object[]> data() throws Exception {
    return Arrays.asList(new Object[][] {
        { "abs(-5)", "$T.abs(-5)", args(Math.class) },
        { "E", "$T.E", args(Math.class) },
        { "PI", "$R", args(Math.class.getField("PI")) },
        { "PI=PI", "$R = PI", args(Math.class.getField("PI")) },
        { "tan()", "$R()", args(Math.class.getMethod("tan", double.class)) },
        { "sin(42.1)", "$R($L)", args(Math.class.getMethod("sin", double.class), 42.1) }
    });
  }

  private final String expected;
  private final String format;
  private final Object[] args;

  public JavaFileImportStaticTest(String expected, String format, Object... args) {
    this.expected = expected;
    this.format = format;
    this.args = args;
  }

  @Test public void statementMatchesExpectation() {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.of(format + ";", args))
            .build())
        .addStaticImport(Math.class, "*")
        .build()
        .toString();
    assertTrue(source.contains("import static java.lang.Math.*;"));
    int indexOfStaticInit = source.indexOf("static {\n") + "static {\n".length();
    String actual = source.substring(indexOfStaticInit, source.indexOf(";", indexOfStaticInit));
    assumeNotNull(expected);
    assertEquals(expected, actual.replaceAll("\\s", ""));
  }
}