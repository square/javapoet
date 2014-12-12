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

@RunWith(JUnit4.class)
public final class PrimitiveNameTest {
  @Test public void primitives() {
    assertThat(Writables.writeToString(PrimitiveName.createBoolean()))
        .isEqualTo("boolean");
    assertThat(Writables.writeToString(PrimitiveName.createByte()))
        .isEqualTo("byte");
    assertThat(Writables.writeToString(PrimitiveName.createChar()))
        .isEqualTo("char");
    assertThat(Writables.writeToString(PrimitiveName.createDouble()))
        .isEqualTo("double");
    assertThat(Writables.writeToString(PrimitiveName.createFloat()))
        .isEqualTo("float");
    assertThat(Writables.writeToString(PrimitiveName.createInt()))
        .isEqualTo("int");
    assertThat(Writables.writeToString(PrimitiveName.createLong()))
        .isEqualTo("long");
    assertThat(Writables.writeToString(PrimitiveName.createShort()))
        .isEqualTo("short");
  }

  @Test public void annotated() {
    PrimitiveName aLong = PrimitiveName.createLong();
    ClassName nonNull = ClassName.create("test", "NonNull");
    aLong.annotate(nonNull);
    assertThat(Writables.writeToString(aLong)).isEqualTo("@test.NonNull long");
  }
}
