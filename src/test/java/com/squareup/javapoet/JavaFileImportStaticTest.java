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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class) public class JavaFileImportStaticTest {

  @Parameters(name = "{index}: `{0}` from format `{1}`") public static Iterable<Object[]> data() {
    MethodSpec method = MethodSpec.methodBuilder("method").build();    
    return Arrays.asList(new Object[][] {
        {"Runtime", "$T", new Object[] {Runtime.class} },
        {"a()", "$T.a()", new Object[] {Runtime.class} },
        {"X", "$T.X", new Object[] {Runtime.class} },
        {null, "$T.$N()", new Object[] {Runtime.class, method} },
        {"RuntimeRuntime", "$T$T", new Object[] {Runtime.class, Runtime.class} },
        {"Runtime.Runtime", "$T.$T", new Object[] {Runtime.class, Runtime.class} },
        {"RuntimeRuntime", "$1T$1T", new Object[] {Runtime.class} },
        {"Runtime?Runtime", "$1T$2L$1T", new Object[] {Runtime.class, "?"} },
        {"Runtime??Runtime", "$1T$2L$2L$1T", new Object[] {Runtime.class, "?"} },
        {null, "$1T$2L$2S$1T$3N$1T", new Object[] {Runtime.class, "?", method} },
        {"Runtime?", "$T$L", new Object[] {Runtime.class, "?"} },
        {"Runtime\"?\"", "$T$S", new Object[] {Runtime.class, "?"} }
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
            .addStaticBlock(CodeBlock.builder()
                .addStatement(format, args)
                .build())
            .build())
        .addStaticImport(Runtime.class, "*")
        .build()
        .toString();
    Assert.assertTrue(source.contains("import static java.lang.Runtime.*;"));
    int indexOfStaticInit = source.indexOf("static {\n") + "static {\n".length();
    String actual = source.substring(indexOfStaticInit, source.indexOf(";", indexOfStaticInit));
    Assume.assumeNotNull(expected);
    Assert.assertEquals(expected, actual.trim());
  }
}
