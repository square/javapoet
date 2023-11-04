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

import java.util.*;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

  @Test public void of() {
    CodeBlock a = CodeBlock.of("$L taco", "delicious");
    assertThat(a.toString()).isEqualTo("delicious taco");
  }

  @Test public void isEmpty() {
    assertTrue(CodeBlock.builder().isEmpty());
    assertTrue(CodeBlock.builder().add("").isEmpty());
    assertFalse(CodeBlock.builder().add(" ").isEmpty());
  }

  @Test public void indentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1>", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  @Test public void deindentCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1<", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  @Test public void dollarSignEscapeCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1$", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  @Test public void statementBeginningCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1[", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
    }
  }

  @Test public void statementEndingCannotBeIndexed() {
    try {
      CodeBlock.builder().add("$1]", "taco").build();
      fail();
    } catch (IllegalArgumentException exp) {
      assertThat(exp)
          .hasMessageThat()
          .isEqualTo("$$, $>, $<, $[, $], $W, and $Z may not have an index");
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

  @Test public void simpleNamedArgument() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "taco");
    CodeBlock block = CodeBlock.builder().addNamed("$text:S", map).build();
    assertThat(block.toString()).isEqualTo("\"taco\"");
  }

  @Test public void repeatedNamedArgument() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "tacos");
    CodeBlock block = CodeBlock.builder()
        .addNamed("\"I like \" + $text:S + \". Do you like \" + $text:S + \"?\"", map)
        .build();
    assertThat(block.toString()).isEqualTo(
        "\"I like \" + \"tacos\" + \". Do you like \" + \"tacos\" + \"?\"");
  }

  @Test public void namedAndNoArgFormat() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("text", "tacos");
    CodeBlock block = CodeBlock.builder()
        .addNamed("$>\n$text:L for $$3.50", map).build();
    assertThat(block.toString()).isEqualTo("\n  tacos for $3.50");
  }

  @Test public void missingNamedArgument() {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      CodeBlock.builder().addNamed("$text:S", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Missing named argument for $text");
    }
  }

  @Test public void lowerCaseNamed() {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("Text", "tacos");
      CodeBlock block = CodeBlock.builder().addNamed("$Text:S", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("argument 'Text' must start with a lowercase character");
    }
  }

  @Test public void multipleNamedArguments() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("pipe", System.class);
    map.put("text", "tacos");

    CodeBlock block = CodeBlock.builder()
        .addNamed("$pipe:T.out.println(\"Let's eat some $text:L\");", map)
        .build();

    assertThat(block.toString()).isEqualTo(
        "java.lang.System.out.println(\"Let's eat some tacos\");");
  }

  @Test public void namedNewline() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("clazz", Integer.class);
    CodeBlock block = CodeBlock.builder().addNamed("$clazz:T\n", map).build();
    assertThat(block.toString()).isEqualTo("java.lang.Integer\n");
  }

  @Test public void danglingNamed() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("clazz", Integer.class);
    try {
      CodeBlock.builder().addNamed("$clazz:T$", map).build();
      fail();
    } catch(IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling $ at end");
    }
  }

  @Test public void indexTooHigh() {
    try {
      CodeBlock.builder().add("$2T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 2 for '$2T' not in range (received 1 arguments)");
    }
  }

  @Test public void indexIsZero() {
    try {
      CodeBlock.builder().add("$0T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 0 for '$0T' not in range (received 1 arguments)");
    }
  }

  @Test public void indexIsNegative() {
    try {
      CodeBlock.builder().add("$-1T", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$-1T'");
    }
  }

  @Test public void indexWithoutFormatType() {
    try {
      CodeBlock.builder().add("$1", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling format characters in '$1'");
    }
  }

  @Test public void indexWithoutFormatTypeNotAtStringEnd() {
    try {
      CodeBlock.builder().add("$1 taco", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$1 taco'");
    }
  }

  @Test public void indexButNoArguments() {
    try {
      CodeBlock.builder().add("$1T").build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("index 1 for '$1T' not in range (received 0 arguments)");
    }
  }

  @Test public void formatIndicatorAlone() {
    try {
      CodeBlock.builder().add("$", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("dangling format characters in '$'");
    }
  }

  @Test public void formatIndicatorWithoutIndexOrFormatType() {
    try {
      CodeBlock.builder().add("$ tacoString", String.class).build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("invalid format string: '$ tacoString'");
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
      assertThat(expected).hasMessageThat().isEqualTo("statement enter $[ followed by statement enter $[");
    }
  }

  @Test public void statementExitWithoutStatementEnter() {
    CodeBlock codeBlock = CodeBlock.builder().add("$]").build();
    try {
      // We can't report this error until rendering type because code blocks might be composed.
      codeBlock.toString();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("statement exit $] has no matching statement enter $[");
    }
  }

  @Test public void join() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = CodeBlock.join(codeBlocks, " || ");
    assertThat(joined.toString()).isEqualTo("\"hello\" || world.World || need tacos");
  }

  @Test public void joining() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
    assertThat(joined.toString()).isEqualTo("\"hello\" || world.World || need tacos");
  }

  @Test public void joiningSingle() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || "));
    assertThat(joined.toString()).isEqualTo("\"hello\"");
  }

  @Test public void joiningWithPrefixAndSuffix() {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    codeBlocks.add(CodeBlock.of("$S", "hello"));
    codeBlocks.add(CodeBlock.of("$T", ClassName.get("world", "World")));
    codeBlocks.add(CodeBlock.of("need tacos"));

    CodeBlock joined = codeBlocks.stream().collect(CodeBlock.joining(" || ", "start {", "} end"));
    assertThat(joined.toString()).isEqualTo("start {\"hello\" || world.World || need tacos} end");
  }

  @Test public void clear() {
    CodeBlock block = CodeBlock.builder()
        .addStatement("$S", "Test string")
        .clear()
        .build();

    assertThat(block.toString()).isEmpty();
  }
  @Test
  public void testJoining() {
    List<CodeBlock> codeBlocks = Arrays.asList(
            CodeBlock.of("String"),
            CodeBlock.of("name"),
            CodeBlock.of("="),
            CodeBlock.of("\"John\"")
    );

    CodeBlock joinedCodeBlock = CodeBlock.join(codeBlocks, " ");
    assertEquals("String name = \"John\"", joinedCodeBlock.toString());
  }
  @Test
  public void testCodeBlockJoinerMerge() {
    CodeBlock.Builder builder1 = CodeBlock.builder();
    builder1.add("$L", "first");
    CodeBlock.Builder builder2 = CodeBlock.builder();
    builder2.add("$L", "second");
    CodeBlock codeBlock1 = builder1.build();
    CodeBlock codeBlock2 = builder2.build();
    CodeBlockJoiner joiner1 = new CodeBlockJoiner(", ", CodeBlock.builder().add(codeBlock1));
    CodeBlockJoiner joiner2 = new CodeBlockJoiner(", ", CodeBlock.builder().add(codeBlock2));
    CodeBlockJoiner mergedJoiner = new CodeBlockJoiner(", ", CodeBlock.builder())
            .merge(joiner1)
            .merge(joiner2);
    CodeBlock mergedCodeBlock = mergedJoiner.join();
    String expected = "first, second";
    assertEquals(expected, mergedCodeBlock.toString());
  }

  public class CodeBlockJoiner {
    private final String delimiter;
    private final CodeBlock.Builder builder;
    private boolean first = true;

    public CodeBlockJoiner(String delimiter, CodeBlock.Builder builder) {
      this.delimiter = delimiter;
      this.builder = builder;
    }
    public CodeBlockJoiner add(CodeBlock codeBlock) {
      if (!first) {
        builder.add(delimiter); // Add delimiter only if not the first block
      }
      first = false;
      builder.add(codeBlock);
      return this;
    }
    public CodeBlockJoiner merge(CodeBlockJoiner other) {
      CodeBlock otherBlock = other.builder.build();
      if (!otherBlock.isEmpty()) {
        if (!builder.isEmpty()) {
          builder.add(delimiter); // Add delimiter only if the builder is not empty
        }
        builder.add(otherBlock);
      }
      return this;
    }
    public CodeBlock join() {
      return builder.build();
    }
  }
}
