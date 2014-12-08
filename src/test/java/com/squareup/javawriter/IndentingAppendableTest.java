/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.javawriter;

import com.google.common.base.Joiner;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class IndentingAppendableTest {
  private final StringBuilder data = new StringBuilder();
  private final Appendable appendable = new IndentingAppendable(data);

  @Test public void newlineCharacterNeverTriggerPendingIndent() throws IOException {
    appendable.append("Hello\n").append('\n').append("World!");
    assertThat(data.toString()).isEqualTo(Joiner.on('\n').join(
        "  Hello",
        "",
        "  World!"
    ));
  }

  @Test public void newlineStringNeverTriggerPendingIndent() throws IOException {
    appendable.append("Hello\n").append("\n").append("World!");
    assertThat(data.toString()).isEqualTo(Joiner.on('\n').join("  Hello", "", "  World!"));
  }

  @Test public void nestingAppendables() throws IOException {
    appendable.append("Hello\n");
    new IndentingAppendable(appendable).append("World\n");
    appendable.append("This Is\n");
    new IndentingAppendable(appendable).append("A Test\n");

    assertThat(data.toString()).isEqualTo(Joiner.on('\n').join(
        "  Hello",
        "    World",
        "  This Is",
        "    A Test",
        ""
    ));
  }

  @Test public void nestingInsideContent() throws IOException {
    appendable.append(Joiner.on('\n').join(
        "def fib(num):",
        "  if num == 1 or num == 2:",
        "    return 1",
        "  return fib(num - 1) + fib(num - 2)"
    ));
    assertThat(data.toString()).isEqualTo(Joiner.on('\n').join(
        "  def fib(num):",
        "    if num == 1 or num == 2:",
        "      return 1",
        "    return fib(num - 1) + fib(num - 2)"
    ));
  }
}
