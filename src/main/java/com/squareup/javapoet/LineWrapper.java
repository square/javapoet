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

import java.io.IOException;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * Implements soft line wrapping on an appendable. To use, append characters using {@link #append}
 * or soft-wrapping spaces using {@link #wrappingSpace}.
 */
final class LineWrapper {
  private final Appendable out;
  private final String indent;
  private final int columnLimit;
  private boolean closed;

  /** Characters written since the last wrapping space that haven't yet been flushed. */
  private final StringBuilder buffer = new StringBuilder();

  /** The number of characters since the most recent newline. Includes both out and the buffer. */
  private int column = 0;

  /** -1 if we have no buffering; otherwise the number of spaces to write after wrapping. */
  private int indentLevel = -1;

  LineWrapper(Appendable out, String indent, int columnLimit) {
    checkNotNull(out, "out == null");
    this.out = out;
    this.indent = indent;
    this.columnLimit = columnLimit;
  }

  /** Emit {@code s}. This may be buffered to permit line wraps to be inserted. */
  void append(String s) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (indentLevel != -1) {
      int nextNewline = s.indexOf('\n');

      // If s doesn't cause the current line to cross the limit, buffer it and return. We'll decide
      // whether or not we have to wrap it later.
      if (nextNewline == -1 && column + s.length() <= columnLimit) {
        buffer.append(s);
        column += s.length();
        return;
      }

      // Wrap if appending s would overflow the current line.
      boolean wrap = nextNewline == -1 || column + nextNewline > columnLimit;
      flush(wrap);
    }

    out.append(s);
    int lastNewline = s.lastIndexOf('\n');
    column = lastNewline != -1
        ? s.length() - lastNewline - 1
        : column + s.length();
  }

  /** Emit either a space or a newline character. */
  void wrappingSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (this.indentLevel != -1) flush(false);
    this.column++;
    this.indentLevel = indentLevel;
  }

  /** Flush any outstanding text and forbid future writes to this line wrapper. */
  void close() throws IOException {
    if (indentLevel != -1) flush(false);
    closed = true;
  }

  /** Write the space followed by any buffered text that follows it. */
  private void flush(boolean wrap) throws IOException {
    if (wrap) {
      out.append('\n');
      for (int i = 0; i < indentLevel; i++) {
        out.append(indent);
      }
      column = indentLevel * indent.length();
      column += buffer.length();
    } else {
      out.append(' ');
    }
    out.append(buffer);
    buffer.delete(0, buffer.length());
    indentLevel = -1;
  }
}
