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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * A fragment of a .java file, potentially containing declarations, statements, and documentation.
 * Code blocks are not necessarily well-formed Java code, and are not validated. This class assumes
 * javac will check correctness later!
 *
 * <p>Code blocks support placeholders like {@link java.text.Format}. Where {@link String#format}
 * uses percent {@code %} to reference target values, this class uses dollar sign {@code $} and has
 * its own set of permitted placeholders:
 *
 * <ul>
 *   <li>{@code $L} emits a <em>literal</em> value with no escaping. Arguments for literals may be
 *       strings, primitives, {@linkplain TypeSpec type declarations}, {@linkplain AnnotationSpec
 *       annotations} and even other code blocks.
 *   <li>{@code $N} emits a <em>name</em>, using name collision avoidance where necessary. Arguments
 *       for names may be strings (actually any {@linkplain CharSequence character sequence}),
 *       {@linkplain ParameterSpec parameters}, {@linkplain FieldSpec fields}, {@linkplain
 *       MethodSpec methods}, and {@linkplain TypeSpec types}.
 *   <li>{@code $S} escapes the value as a <em>string</em>, wraps it with double quotes, and emits
 *       that. For example, {@code 6" sandwich} is emitted {@code "6\" sandwich"}.
 *   <li>{@code T} emits a <em>type</em> reference. Types will be imported if possible. Arguments
 *       for types may be {@linkplain Class classes}, {@linkplain javax.lang.model.type.TypeMirror
,*       type mirrors}, and {@linkplain javax.lang.model.element.Element elements}.
 *   <li>{@code $$} emits a dollar sign.
 *   <li>{@code $&gt;} increases the indentation level.
 *   <li>{@code $&lt;} decreases the indentation level.
 * </ul>
 */
public final class CodeBlock {
  /** A heterogeneous list containing string literals and value placeholders. */
  final List<String> formatParts;
  final List<Object> args;

  private CodeBlock(Builder builder) {
    this.formatParts = Collections.unmodifiableList(new ArrayList<>(builder.formatParts));
    this.args = Collections.unmodifiableList(new ArrayList<>(builder.args));
  }

  public boolean isEmpty() {
    return formatParts.isEmpty();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    final List<String> formatParts = new ArrayList<>();
    final List<Object> args = new ArrayList<>();

    private Builder() {
    }

    public Builder add(String format, Object... args) {
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
            case '>':
            case '<':
              nextP = p + 2;
              break;

            default:
              throw new IllegalArgumentException("invalid format string: " + format);
          }
        }

        formatParts.add(format.substring(p, nextP));
      }

      checkArgument(args.length == expectedArgsLength,
          "expected %s args for %s but was %s", expectedArgsLength, format, args.length);

      Collections.addAll(this.args, args);
      return this;
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * Shouldn't contain braces or newline characters.
     */
    public Builder beginControlFlow(String controlFlow, Object... args) {
      add(controlFlow + " {\n", args);
      indent();
      return this;
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    public Builder nextControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} ", args);
      add(controlFlow, args);
      add("{\n", args);
      indent();
      return this;
    }

    public Builder endControlFlow() {
      unindent();
      add("}\n");
      return this;
    }

    /**
     * @param controlFlow the optional control flow construct and its code, such as
     *     "while(foo == 20)". Only used for "do/while" control flows.
     */
    public Builder endControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} " + controlFlow + ";\n", args);
      return this;
    }

    public Builder addStatement(String format, Object... args) {
      return add(format + ";\n", args);
    }

    public Builder add(CodeBlock codeBlock) {
      formatParts.addAll(codeBlock.formatParts);
      args.addAll(codeBlock.args);
      return this;
    }

    public Builder indent() {
      this.formatParts.add("$>");
      return this;
    }

    public Builder unindent() {
      this.formatParts.add("$<");
      return this;
    }

    public CodeBlock build() {
      return new CodeBlock(this);
    }
  }
}
