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

public class CodeBlockTest {
  
  @Test
  public void indentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1>", "taco").build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test
  public void deindentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1<", "taco").build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test
  public void dollarSignEscapeCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1$", "taco").build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
 
  @Test
  public void statementBeginningCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1[", "taco").build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test
  public void statementEndingCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1]", "taco").build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("$$, $>, $<, $[ and $] may not have an index");
    }
  }
  
  @Test
  public void nameFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$N $1N", "taco").build();
    assertThat(block.toString()).isEqualTo("taco taco");
  }
  
  @Test
  public void literalFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$L $1L", "taco").build();
    assertThat(block.toString()).isEqualTo("taco taco");
  }
  
  @Test
  public void stringFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$S $1S", "taco").build();
    assertThat(block.toString()).isEqualTo("\"taco\" \"taco\"");
  }
  
  @Test
  public void typeFormatCanBeIndexed() {
    CodeBlock block = CodeBlock.builder().add("$T $1T", String.class).build();
    assertThat(block.toString()).isEqualTo("java.lang.String java.lang.String");
  }
  
  @Test
  public void indexTooHigh() {
    try {
      CodeBlock.builder().add("$T $2T", String.class).build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("Argument index 2 in '$T $2T' is larger than number of parameters");
    }
  }
  
  @Test
  public void indexIsZero() {
    try {
    CodeBlock.builder().add("$T $0T", String.class).build();
    fail();
    } catch(IllegalArgumentException exp) {
      assertThat(exp).hasMessage("Argument index 0 in '$T $0T' is less than one, the minimum format index");
    }
  }
  
  @Test
  public void indexIsNegative() {
    try {
      CodeBlock.builder().add("$T $-1T", String.class).build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("invalid format string: $T $-1T");
    }
  }
  
  @Test
  public void indexWithoutFormatType() {
    try {
      CodeBlock.builder().add("$1", String.class).build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("Dangling format characters '$1' in format string '$1'");
    }
  }
  
  @Test
  public void indexWithoutFormatTypeNotAtStringEnd() {
    try {
      CodeBlock.builder().add("$1 taco", String.class).build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("invalid format string: $1 taco");
    }
  }
  
  @Test
  public void formatIndicatorAlone() {
    try {
      CodeBlock.builder().add("$", String.class).build();
      fail();
    } catch (IllegalStateException exp) {
      assertThat(exp).hasMessage("dangling $ in format string $");
    }
  }
  
  @Test
  public void formatIndicatorWithoutIndexOrFormatType() {
    try {
      CodeBlock.builder().add("$ tacoString", String.class).build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp).hasMessage("invalid format string: $ tacoString");
    }
  }
  
  @Test
  public void indexingDoesNotIncreaseNaturalIndex() {
    CodeBlock block = CodeBlock.builder().add("$L $L $2L $L", 1, 2, 3).build();
    assertThat(block.toString()).isEqualTo("1 2 2 3");
  }
  
  @Test
  public void indexingSelectsProperPosition() {
    CodeBlock block = CodeBlock.builder().add("$L $L $L $3L $2L $1L", 1, 2, 3).build();
    assertThat(block.toString()).isEqualTo("1 2 3 3 2 1");
  }
  
  @Test
  public void indexingCanBeInterleved() {
    CodeBlock block = CodeBlock.builder().add("$L $3L $L $2L $L $1L", 1, 2, 3).build();
    assertThat(block.toString()).isEqualTo("1 3 2 2 3 1");
  }
  
  @Test
  public void sameIndexCanBeUsedWithDifferentFormats() {
    CodeBlock block = CodeBlock.builder().add("$1T.out.println($1S)", ClassName.get(System.class)).build();
    assertThat(block.toString()).isEqualTo("java.lang.System.out.println(\"java.lang.System\")");
  }
  
}
