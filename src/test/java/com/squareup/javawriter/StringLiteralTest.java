// Copyright 2014 Square, Inc.
package com.squareup.javawriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public final class StringLiteralTest {
  @Test public void stringLiteral() {
    assertThat(StringLiteral.forValue("").toString()).isEqualTo("\"\"");
    assertThat(StringLiteral.forValue("JavaWriter").toString()).isEqualTo("\"JavaWriter\"");
    assertThat(StringLiteral.forValue("\\").toString()).isEqualTo("\"\\\\\"");
    assertThat(StringLiteral.forValue("\"").toString()).isEqualTo("\"\\\"\"");
    assertThat(StringLiteral.forValue("\b").toString()).isEqualTo("\"\\b\"");
    assertThat(StringLiteral.forValue("\t").toString()).isEqualTo("\"\\t\"");
    assertThat(StringLiteral.forValue("\n").toString()).isEqualTo("\"\\n\"");
    assertThat(StringLiteral.forValue("\f").toString()).isEqualTo("\"\\f\"");
    assertThat(StringLiteral.forValue("\r").toString()).isEqualTo("\"\\r\"");

    // Control characters
    for (char i = 0x1; i <= 0x1f; i++) {
      checkCharEscape(i);
    }
    for (char i = 0x7f; i <= 0x9f; i++) {
      checkCharEscape(i);
    }
  }

  private void checkCharEscape(char codePoint) {
    String test = "" + codePoint;
    String expected;
    switch (codePoint) {
      case 8: expected = "\"\\b\""; break;
      case 9: expected = "\"\\t\""; break;
      case 10: expected = "\"\\n\""; break;
      case 12: expected = "\"\\f\""; break;
      case 13: expected = "\"\\r\""; break;
      default: expected = "\"\\u" + String.format("%04x", (int) codePoint) + "\"";
    }
    assertThat(StringLiteral.forValue(test).toString()).isEqualTo(expected);
  }

}
