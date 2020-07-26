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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UtilTest {

  @TestFactory Stream<DynamicNode> characterLiteralWithoutSingleQuotes() {
    return Stream.of(
        dynamicContainer(
            "basic (not escaped)",
            Stream.of(
                assertCharacterLiteralWithoutSingleQuotes("a", 'a'),
                assertCharacterLiteralWithoutSingleQuotes("b", 'b'),
                assertCharacterLiteralWithoutSingleQuotes("c", 'c'),
                assertCharacterLiteralWithoutSingleQuotes("%", '%'))),
        dynamicContainer(
            "common escapes",
            Stream.of(
                assertCharacterLiteralWithoutSingleQuotes("\\b", '\b'),
                assertCharacterLiteralWithoutSingleQuotes("\\t", '\t'),
                assertCharacterLiteralWithoutSingleQuotes("\\n", '\n'),
                assertCharacterLiteralWithoutSingleQuotes("\\f", '\f'),
                assertCharacterLiteralWithoutSingleQuotes("\\r", '\r'),
                assertCharacterLiteralWithoutSingleQuotes("\"", '"'),
                assertCharacterLiteralWithoutSingleQuotes("\\'", '\''),
                assertCharacterLiteralWithoutSingleQuotes("\\\\", '\\'))),
        dynamicContainer(
            "octal escapes",
            Stream.of(
                assertCharacterLiteralWithoutSingleQuotes("\\u0000", '\0'),
                assertCharacterLiteralWithoutSingleQuotes("\\u0007", '\7'),
                assertCharacterLiteralWithoutSingleQuotes("?", '\77'),
                assertCharacterLiteralWithoutSingleQuotes("\\u007f", '\177'),
                assertCharacterLiteralWithoutSingleQuotes("¿", '\277'),
                assertCharacterLiteralWithoutSingleQuotes("ÿ", '\377'))),
        dynamicContainer(
            "unicode escapes",
            Stream.of(
                assertCharacterLiteralWithoutSingleQuotes("\\u0000", '\u0000'),
                assertCharacterLiteralWithoutSingleQuotes("\\u0001", '\u0001'),
                assertCharacterLiteralWithoutSingleQuotes("\\u0002", '\u0002'),
                assertCharacterLiteralWithoutSingleQuotes("€", '\u20AC'),
                assertCharacterLiteralWithoutSingleQuotes("☃", '\u2603'),
                assertCharacterLiteralWithoutSingleQuotes("♠", '\u2660'),
                assertCharacterLiteralWithoutSingleQuotes("♣", '\u2663'),
                assertCharacterLiteralWithoutSingleQuotes("♥", '\u2665'),
                assertCharacterLiteralWithoutSingleQuotes("♦", '\u2666'),
                assertCharacterLiteralWithoutSingleQuotes("✵", '\u2735'),
                assertCharacterLiteralWithoutSingleQuotes("✺", '\u273A'),
                assertCharacterLiteralWithoutSingleQuotes("／", '\uFF0F'))));
  }

  private DynamicTest assertCharacterLiteralWithoutSingleQuotes(String expected, char c) {
    String displayName = "char(" + c + ") -> " + expected;
    String actual = Util.characterLiteralWithoutSingleQuotes(c);
    return dynamicTest(displayName, () -> assertEquals(expected, actual));
  }

  @Test void stringLiteralWithDoubleQuotes() {
    assertAll(
        "stringLiteralWithDoubleQuotes assertions",
        () -> stringLiteral("abc"),
        () -> stringLiteral("♦♥♠♣"),
        () -> stringLiteral("€\\t@\\t$", "€\t@\t$"),
        () -> stringLiteral("abc();\\n\"\n  + \"def();", "abc();\ndef();"),
        () -> stringLiteral("This is \\\"quoted\\\"!", "This is \"quoted\"!"),
        () -> stringLiteral("e^{i\\\\pi}+1=0", "e^{i\\pi}+1=0"));
  }

  void stringLiteral(String string) {
    stringLiteral(string, string);
  }

  void stringLiteral(String expected, String value) {
    assertEquals("\"" + expected + "\"", Util.stringLiteralWithDoubleQuotes(value, " "));
  }
}
