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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * ParameterWriter only differs from FieldWriter in the format of its annotations.
 */
@RunWith(JUnit4.class)
public final class ParameterWriterTest {
  @Test public void simple() {
    ClassName runnable = ClassName.fromClass(Runnable.class);
    ParameterWriter parameterWriter = new ParameterWriter(runnable, "runnable");

    assertThat(Writables.writeToString(parameterWriter))
        .isEqualTo("java.lang.Runnable runnable");
  }

  @Test public void annotated() {
    ClassName notNull = ClassName.create("example", "NotNull");
    ClassName redacted = ClassName.create("example", "Redacted");
    ClassName runnable = ClassName.fromClass(Runnable.class);
    ParameterWriter parameterWriter = new ParameterWriter(runnable, "runnable");
    parameterWriter.annotate(notNull);
    parameterWriter.annotate(redacted);

    assertThat(Writables.writeToString(parameterWriter))
        .isEqualTo("@example.NotNull @example.Redacted java.lang.Runnable runnable");
  }
}
