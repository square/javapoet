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

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class NameAllocatorTest {

  @Test void usage() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("foo", 1)).isEqualTo("foo");
    assertThat(nameAllocator.newName("bar", 2)).isEqualTo("bar");
    assertThat(nameAllocator.get(1)).isEqualTo("foo");
    assertThat(nameAllocator.get(2)).isEqualTo("bar");
  }

  @Test void nameCollision() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo");
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo_");
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo__");
  }

  @Test void nameCollisionWithTag() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("foo", 1)).isEqualTo("foo");
    assertThat(nameAllocator.newName("foo", 2)).isEqualTo("foo_");
    assertThat(nameAllocator.newName("foo", 3)).isEqualTo("foo__");
    assertThat(nameAllocator.get(1)).isEqualTo("foo");
    assertThat(nameAllocator.get(2)).isEqualTo("foo_");
    assertThat(nameAllocator.get(3)).isEqualTo("foo__");
  }

  @Test void characterMappingSubstitute() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("a-b", 1)).isEqualTo("a_b");
  }

  @Test void characterMappingSurrogate() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("a\uD83C\uDF7Ab", 1)).isEqualTo("a_b");
  }

  @Test void characterMappingInvalidStartButValidPart() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("1ab", 1)).isEqualTo("_1ab");
    assertThat(nameAllocator.newName("a-1", 2)).isEqualTo("a_1");
  }

  @Test void characterMappingInvalidStartIsInvalidPart() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("&ab", 1)).isEqualTo("_ab");
  }

  @Test void javaKeyword() {
    NameAllocator nameAllocator = new NameAllocator();
    assertThat(nameAllocator.newName("public", 1)).isEqualTo("public_");
    assertThat(nameAllocator.get(1)).isEqualTo("public_");
  }

  @Test void tagReuseForbidden() {
    NameAllocator nameAllocator = new NameAllocator();
    nameAllocator.newName("foo", 1);
    Exception expected = assertThrows(IllegalArgumentException.class, () -> nameAllocator.newName("bar", 1));
    assertThat(expected).hasMessageThat().isEqualTo("tag 1 cannot be used for both 'foo' and 'bar'");
  }

  @Test void useBeforeAllocateForbidden() {
    NameAllocator nameAllocator = new NameAllocator();
    Exception expected = assertThrows(IllegalArgumentException.class, () -> nameAllocator.get(1));
    assertThat(expected).hasMessageThat().isEqualTo("unknown tag: 1");
  }

  @Test void cloneUsage() {
    NameAllocator outterAllocator = new NameAllocator();
    outterAllocator.newName("foo", 1);

    NameAllocator innerAllocator1 = outterAllocator.clone();
    assertThat(innerAllocator1.newName("bar", 2)).isEqualTo("bar");
    assertThat(innerAllocator1.newName("foo", 3)).isEqualTo("foo_");

    NameAllocator innerAllocator2 = outterAllocator.clone();
    assertThat(innerAllocator2.newName("foo", 2)).isEqualTo("foo_");
    assertThat(innerAllocator2.newName("bar", 3)).isEqualTo("bar");
  }
}
