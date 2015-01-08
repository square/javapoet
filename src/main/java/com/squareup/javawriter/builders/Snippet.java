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
package com.squareup.javawriter.builders;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * A deferred format string. Unlike {@link java.text.Format} which uses percent {@code %} to escape
 * placeholders, this uses {@code $}, and has its own set of permitted placeholders:
 *
 * <ul>
 *   <li>{@code $L} emits the <em>literal</em> value with no escaping.
 *   <li>{@code $N} emits a <em>name</em>, using name collision avoidance where necessary.
 *   <li>{@code $S} escapes the value as a <em>string</em>, wraps it with double quotes, and emits
 *       that.
 *   <li>{@code $T} emits a <em>type</em> reference. Types will be imported if possible.
 *   <li>{@code $$} emits a dollar sign.
 * </ul>
 */
final class Snippet {
  /** A heterogeneous list containing string literals and value placeholders. */
  final ImmutableList<String> formatParts;
  final ImmutableList<Object> args;

  public Snippet(String format, Object[] args) {
    ImmutableList.Builder<String> formatPartsBuilder = ImmutableList.builder();
    int expectedArgsLength = 0;
    for (int p = 0, nextP; p < format.length(); p = nextP) {
      if (format.charAt(p) != '$') {
        nextP = format.indexOf('$', p + 1);
        if (nextP == -1) nextP = format.length();
      } else {
        checkState(p + 1 < format.length(), "dangling $ in format string %s", format);
        switch (format.charAt(p + 1)) {
          case 'L':
          case 'N':
          case 'S':
          case 'T':
            expectedArgsLength++;
            // Fall through.
          case '$':
            nextP = p + 2;
            break;

          default:
            throw new IllegalArgumentException("invalid format string: " + format);
        }
      }

      formatPartsBuilder.add(format.substring(p, nextP));
    }

    checkArgument(args.length == expectedArgsLength,
        "expected %s args but was %s", expectedArgsLength, args);

    this.formatParts = formatPartsBuilder.build();
    this.args = ImmutableList.copyOf(args);
  }
}
