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

import org.junit.Test;

public class UtilTest {
  @Test public void characterLiteral() {
    assertEquals("a", Util.characterLiteralWithoutSingleQuotes('a'));
    assertEquals("b", Util.characterLiteralWithoutSingleQuotes('b'));
    assertEquals("c", Util.characterLiteralWithoutSingleQuotes('c'));
    assertEquals("%", Util.characterLiteralWithoutSingleQuotes('%'));
    // common escapes
    assertEquals("\\b", Util.characterLiteralWithoutSingleQuotes('\b'));
    assertEquals("\\t", Util.characterLiteralWithoutSingleQuotes('\t'));
    assertEquals("\\n", Util.characterLiteralWithoutSingleQuotes('\n'));
    assertEquals("\\f", Util.characterLiteralWithoutSingleQuotes('\f'));
    assertEquals("\\r", Util.characterLiteralWithoutSingleQuotes('\r'));
    assertEquals("\"", Util.characterLiteralWithoutSingleQuotes('"'));
    assertEquals("\\'", Util.characterLiteralWithoutSingleQuotes('\''));
    assertEquals("\\\\", Util.characterLiteralWithoutSingleQuotes('\\'));
    // octal escapes
    assertEquals("\\u0000", Util.characterLiteralWithoutSingleQuotes('\0'));
    assertEquals("\\u0007", Util.characterLiteralWithoutSingleQuotes('\7'));
    assertEquals("?", Util.characterLiteralWithoutSingleQuotes('\77'));
    assertEquals("\\u007f", Util.characterLiteralWithoutSingleQuotes('\177'));
    assertEquals("¿", Util.characterLiteralWithoutSingleQuotes('\277'));
    assertEquals("ÿ", Util.characterLiteralWithoutSingleQuotes('\377'));
    // unicode escapes
    assertEquals("\\u0000", Util.characterLiteralWithoutSingleQuotes('\u0000'));
    assertEquals("\\u0001", Util.characterLiteralWithoutSingleQuotes('\u0001'));
    assertEquals("\\u0002", Util.characterLiteralWithoutSingleQuotes('\u0002'));
    assertEquals("€", Util.characterLiteralWithoutSingleQuotes('\u20AC'));
    assertEquals("☃", Util.characterLiteralWithoutSingleQuotes('\u2603'));
    assertEquals("♠", Util.characterLiteralWithoutSingleQuotes('\u2660'));
    assertEquals("♣", Util.characterLiteralWithoutSingleQuotes('\u2663'));
    assertEquals("♥", Util.characterLiteralWithoutSingleQuotes('\u2665'));
    assertEquals("♦", Util.characterLiteralWithoutSingleQuotes('\u2666'));
    assertEquals("✵", Util.characterLiteralWithoutSingleQuotes('\u2735'));
    assertEquals("✺", Util.characterLiteralWithoutSingleQuotes('\u273A'));
    assertEquals("／", Util.characterLiteralWithoutSingleQuotes('\uFF0F'));
  }

  @Test public void stringLiteral() {
    stringLiteral("abc");
    stringLiteral("♦♥♠♣");
    stringLiteral("€\\t@\\t$", "€\t@\t$", " ");
    stringLiteral("abc();\\n\"\n  + \"def();", "abc();\ndef();", " ");
    stringLiteral("This is \\\"quoted\\\"!", "This is \"quoted\"!", " ");
    stringLiteral("e^{i\\\\pi}+1=0", "e^{i\\pi}+1=0", " ");
  }

  void stringLiteral(String string) {
    stringLiteral(string, string, " ");
  }

  void stringLiteral(String expected, String value, String indent) {
    assertEquals("\"" + expected + "\"", Util.stringLiteralWithDoubleQuotes(value, indent));
  }
}
