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

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class AnnotationWriterTest {
  private final ClassName className = ClassName.bestGuessFromString("com.example.Thing");
  private final AnnotationWriter writer = new AnnotationWriter(className);

  @Test public void bare() throws IOException {
    String expected = "@com.example.Thing";

    assertThat(Writables.writeToString(writer)).isEqualTo(expected);
  }

  @Test public void value() throws IOException {
    String expected = "@com.example.Thing(\"Hello, world!\")";

    writer.setValue("Hello, world!");

    assertThat(Writables.writeToString(writer)).isEqualTo(expected);
  }

  @Test public void valueMember() throws IOException {
    String expected = "@com.example.Thing(\"Hello, world!\")";

    writer.setMember("value", "Hello, world!");

    assertThat(Writables.writeToString(writer)).isEqualTo(expected);
  }

  @Test public void singleNonValueParameter() throws IOException {
    String expected = "@com.example.Thing(greeting = \"Hello, world!\")";

    writer.setMember("greeting", "Hello, world!");

    assertThat(Writables.writeToString(writer)).isEqualTo(expected);
  }

  @Test public void multipleParameters() throws IOException {
    String expected = "@com.example.Thing(name = \"Hello, world!\", value = 42)";

    writer.setMember("name", "Hello, world!");
    writer.setMember("value", 42);

    assertThat(Writables.writeToString(writer)).isEqualTo(expected);
  }
}
