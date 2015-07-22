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
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkState;

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
      List<Character> paramChars = new ArrayList<>();
      List<Integer> paramIndexes = new ArrayList<>();
      boolean autoIndexing = false;
      int i = 0;
      for (int p = 0, nextP; p < format.length(); p = nextP) {
        if (format.charAt(p) != '$') {
          nextP = format.indexOf('$', p + 1);
          if (nextP == -1) nextP = format.length();
          formatParts.add(format.substring(p, nextP));
        } else {
          checkState(p + 1 < format.length(), "dangling $ in format string %s", format);
          /* Rules:
           * Format must be in the form $[Numeric Index]{N, L, S, T}
           * OR
           * in the form ${$, >, <, [, ]}
           * Numeric index is ONE BASED (for consistency with java formatting)
           * Numeric indexing must refer to a format argument within the args array
           * Only progress iteration over the argument iterator if we do not hit an indexed entry
           */
          int countOfIndexCharacters = 0;
          while (isSimpleDigit(format.charAt(p + countOfIndexCharacters + 1))) {
            countOfIndexCharacters++;
            checkArgument(format.length() > p + countOfIndexCharacters + 1,
                "Dangling format characters '%s' in format string '%s'",
                format.substring(p), format);
          }
          if (!paramChars.isEmpty()) {
            if (autoIndexing) {
              char toCheck = format.charAt(p + 1);
              checkArgument(countOfIndexCharacters == 0,
                  "cannot mix indexed and positional parameters");
              checkArgument(isValidParameterChar(toCheck), "invalid format string: %s", format);

              if (!isNonIndexedChar(toCheck)) {
                paramChars.add(toCheck);
                paramIndexes.add(i);
                i++;
              }

              nextP = p + 2;
              formatParts.add("$" + toCheck);
            } else {
              char toCheck = format.charAt(p + countOfIndexCharacters + 1);
              if (countOfIndexCharacters == 0) {
                boolean valid = isValidParameterChar(toCheck);
                checkArgument(valid, "invalid format string: %s", format);
                checkArgument(isNonIndexedChar(toCheck),
                    "cannot mix indexed and positional paramters");
              } else {
                checkArgument(!isNonIndexedChar(toCheck),
                    "$$, $>, $<, $[ and $] may not have an index");
              }
              int argsIndex =
                  Integer.parseInt(format.substring(p + 1, p + 1 + countOfIndexCharacters));
              checkArgument(argsIndex <= args.length,
                  "Argument index %s in '%s' is larger than number of parameters",
                  argsIndex, format);
              checkArgument(argsIndex > 0,
                  "Argument index %s in '%s' is less than one, the minimum format index",
                  argsIndex, format);

              if (!isNonIndexedChar(toCheck)) {
                paramChars.add(toCheck);
                paramIndexes.add(argsIndex - 1);
              }

              nextP = p + countOfIndexCharacters + 2;
              formatParts.add("$" + toCheck);
            }
          } else {
            char toCheck = format.charAt(p + countOfIndexCharacters + 1);
            int index = -1;
            if (countOfIndexCharacters == 0) {
              boolean valid = isValidParameterChar(toCheck);
              checkArgument(valid, "invalid format string: %s", format);
              if (!isNonIndexedChar(toCheck)) {
                index = i;
                i++;
              }
              autoIndexing = true;
            } else {
              checkArgument(isValidParameterChar(toCheck), "invalid format string: %s", format);
              checkArgument(!isNonIndexedChar(toCheck),
                  "$$, $>, $<, $[ and $] may not have an index");
              autoIndexing = false;
              int argsIndex =
                  Integer.parseInt(format.substring(p + 1, p + 1 + countOfIndexCharacters));
              checkArgument(argsIndex <= args.length,
                  "Argument index %s in '%s' is larger than number of parameters",
                  argsIndex, format);
              checkArgument(argsIndex > 0,
                  "Argument index %s in '%s' is less than one, the minimum format index",
                  argsIndex, format);
              index = argsIndex - 1;
            }

            if (index > -1) {
              paramChars.add(toCheck);
              paramIndexes.add(index);
            }

            nextP = p + countOfIndexCharacters + 2;
            formatParts.add("$" + toCheck);
          }
        }
      }
      int max = -1;
      for (int j = 0; j < paramIndexes.size(); j++) {
        max = Math.max(max, paramIndexes.get(j));
      }
      checkArgument(max < args.length,
          "Not enough parameters were given; expected %s, got %s", max + 1, args.length);
      checkArgument(max == args.length - 1,
          "Too many parameters were given; expected %s, got %s", max + 1, args.length);

      Iterator<Character> iterChars = paramChars.iterator();
      Iterator<Integer> iterIndexes = paramIndexes.iterator();
      for (; iterChars.hasNext();) {
        char c = iterChars.next();
        int index = iterIndexes.next();
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
            throw new IllegalStateException("format char '" + c + "' was unexpected");
        }
      }
      return this;
    }

    private boolean isValidParameterChar(char toCheck) {
      switch (toCheck) {
        case 'N':
        case 'L':
        case 'S':
        case 'T':
        case '$':
        case '>':
        case '<':
        case '[':
        case ']':
          return true;
        default:
          return false;
      }
    }

    private boolean isNonIndexedChar(char toCheck) {
      switch (toCheck) {
        case '$':
        case '>':
        case '<':
        case '[':
        case ']':
          return true;
        default:
          return false;
      }
    }

    /**
     * A version of {@link Character#isDigit(char)} that only accepts '0'-'9'.
     */
    private boolean isSimpleDigit(char toCheck) {
      return toCheck >= '0' && toCheck <= '9';
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
