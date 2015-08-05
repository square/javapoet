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

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.Util.checkArgument;

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
 *   <li>{@code $T} emits a <em>type</em> reference. Types will be imported if possible. Arguments
 *       for types may be {@linkplain Class classes}, {@linkplain javax.lang.model.type.TypeMirror
,*       type mirrors}, and {@linkplain javax.lang.model.element.Element elements}.
 *   <li>{@code $$} emits a dollar sign.
 *   <li>{@code $&gt;} increases the indentation level.
 *   <li>{@code $&lt;} decreases the indentation level.
 *   <li>{@code $[} begins a statement. For multiline statements, every line after the first line
 *       is double-indented.
 *   <li>{@code $]} ends a statement.
 * </ul>
 */
public final class CodeBlock {
  /** A heterogeneous list containing string literals and value placeholders. */
  final List<String> formatParts;
  final List<Object> args;

  private CodeBlock(Builder builder) {
    this.formatParts = Util.immutableList(builder.formatParts);
    this.args = Util.immutableList(builder.args);
  }

  public boolean isEmpty() {
    return formatParts.isEmpty();
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      new CodeWriter(out).emit(this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.formatParts.addAll(formatParts);
    builder.args.addAll(args);
    return builder;
  }

  public static final class Builder {
    final List<String> formatParts = new ArrayList<>();
    final List<Object> args = new ArrayList<>();

    private Builder() {
    }

    public Builder add(String format, Object... args) {
      boolean hasRelative = false;
      boolean hasIndexed = false;
      int parameterCount = 0;

      for (int p = 0; p < format.length(); ) {
        if (format.charAt(p) != '$') {
          int nextP = format.indexOf('$', p + 1);
          if (nextP == -1) nextP = format.length();
          formatParts.add(format.substring(p, nextP));
          p = nextP;
          continue;
        }

        p++; // '$'.

        // Consume zero or more digits, leaving 'c' as the first non-digit char after the '$'.
        int indexStart = p;
        char c;
        do {
          checkArgument(p < format.length(), "dangling format characters in '%s'", format);
          c = format.charAt(p++);
        } while (c >= '0' && c <= '9');
        int indexEnd = p - 1;

        // If 'c' doesn't take an argument, we're done.
        if (c == '$' || c == '>' || c == '<' || c == '[' || c == ']') {
          checkArgument(indexStart == indexEnd, "$$, $>, $<, $[ and $] may not have an index");
          formatParts.add("$" + c);
          continue;
        }

        // Find either the indexed argument, or the relative argument. (0-based).
        int index;
        if (indexStart < indexEnd) {
          index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1;
          hasIndexed = true;
        } else {
          index = parameterCount;
          hasRelative = true;
        }
        parameterCount++;

        checkArgument(index >= 0 && index < args.length,
            "index %d for '%s' not in range (received %s arguments)",
            index + 1, format.substring(indexStart - 1, indexEnd + 1), args.length);
        checkArgument(!hasIndexed || !hasRelative, "cannot mix indexed and positional parameters");

        switch (c) {
          case 'N':
            this.args.add(argToName(args[index]));
            break;
          case 'L':
            this.args.add(argToLiteral(args[index]));
            break;
          case 'S':
            this.args.add(argToString(args[index]));
            break;
          case 'T':
            this.args.add(argToType(args[index]));
            break;
          default:
            throw new IllegalArgumentException(
                String.format("invalid format string: '%s'", format));
        }

        formatParts.add("$" + c);
      }

      checkArgument(parameterCount >= args.length,
          "unused arguments: expected %s, received %s", parameterCount, args.length);
      return this;
    }

    private String argToName(Object o) {
      if (o instanceof CharSequence) return o.toString();
      if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
      if (o instanceof FieldSpec) return ((FieldSpec) o).name;
      if (o instanceof MethodSpec) return ((MethodSpec) o).name;
      if (o instanceof TypeSpec) return ((TypeSpec) o).name;
      throw new IllegalArgumentException("expected name but was " + o);
    }

    private Object argToLiteral(Object o) {
      return o;
    }

    private String argToString(Object o) {
      return o != null ? String.valueOf(o) : null;
    }

    private TypeName argToType(Object o) {
      if (o instanceof TypeName) return (TypeName) o;
      if (o instanceof TypeMirror) return TypeName.get((TypeMirror) o);
      if (o instanceof Element) return TypeName.get(((Element) o).asType());
      if (o instanceof Type) return TypeName.get((Type) o);
      throw new IllegalArgumentException("expected type but was " + o);
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
      add("} " + controlFlow + " {\n", args);
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
      add("$[");
      add(format, args);
      add(";\n$]");
      return this;
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
