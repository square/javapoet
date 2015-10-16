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

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class CodeBlockTest {
  @Test public void equalsAndHashCode() {
    CodeBlock a = CodeBlock.builder().build();
    CodeBlock b = CodeBlock.builder().build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = CodeBlock.builder().add("$L", "taco").build();
    b = CodeBlock.builder().add("$L", "taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test public void indentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1>", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test public void deindentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1<", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test public void dollarSignEscapeCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1$", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
 
  @Test public void statementBeginningCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1[", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test public void statementEndingCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1]", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test public void nameFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1N", "taco").build();
    assertThat(block.toString()).isEqualTo("taco");
  }
  
  @Test public void literalFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1L", "taco").build();
    assertThat(block.toString()).isEqualTo("taco");
  }
  
  @Test public void stringFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1S", "taco").build();
    assertThat(block.toString()).isEqualTo("\"taco\"");
  }
  
  @Test public void typeFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$1T", String.class).build();
    assertThat(block.toString()).isEqualTo("java.lang.String");
  }
  
  @Test public void indexTooHigh() {
    try {
      CodeBlock.builder().add("$2T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("index 2 for '$2T' not in range (received 1 arguments)");
    }
  }
  
  @Test public void indexIsZero() {
    try {
      CodeBlock.builder().add("$0T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("index 0 for '$0T' not in range (received 1 arguments)");
    }
  }
  
  @Test public void indexIsNegative() {
    try {
      CodeBlock.builder().add("$-1T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("invalid format string: '$-1T'");
    }
  }
  
  @Test public void indexWithoutFormatType() {
    try {
      CodeBlock.builder().add("$1", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("dangling format characters in '$1'");
    }
  }
  
  @Test public void indexWithoutFormatTypeNotAtStringEnd() {
    try {
      CodeBlock.builder().add("$1 taco", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("invalid format string: '$1 taco'");
    }
  }
  
  @Test public void formatIndicatorAlone() {
    try {
      CodeBlock.builder().add("$", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("dangling format characters in '$'");
    }
  }
  
  @Test public void formatIndicatorWithoutIndexOrFormatType() {
    try {
      CodeBlock.builder().add("$ tacoString", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("invalid format string: '$ tacoString'");
    }
  }
  
  @Test public void sameIndexCanBeUsedWithDifferentFormats() {
    CodeBlock block = CodeBlock.builder()
        .add("$1T.out.println($1S)", ClassName.get(System.class))
        .build();
    assertThat(block.toString()).isEqualTo("java.lang.System.out.println(\"java.lang.System\")");
  }

  @Test public void tooManyStatementEnters() {
    CodeBlock codeBlock = CodeBlock.builder().add("$[$[").build();
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("statement enter $[ followed by statement enter $[");
    }
  }

  @Test public void statementExitWithoutStatementEnter() {
    CodeBlock codeBlock = CodeBlock.builder().add("$]").build();
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("statement exit $] has no matching statement enter $[");
    }
  }
}
