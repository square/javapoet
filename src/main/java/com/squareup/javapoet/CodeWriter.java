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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/**
 * Converts a {@link JavaFile} to a string suitable to both human- and javac-consumption. This
 * honors imports, indentation, and deferred variable names.
 */
final class CodeWriter {
  private final String indent;
  private final Appendable out;
  private int indentLevel;

  private boolean javadoc = false;
  private boolean comment = false;
  private String packageName;
  private final List<TypeSpec> typeSpecStack = new ArrayList<>();
  private final Map<ClassName, String> importedTypes;
  private final Set<ClassName> importableTypes = new LinkedHashSet<>();
  private final Set<String> referencedNames = new LinkedHashSet<>();
  private boolean trailingNewline;

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  int statementLine = -1;

  CodeWriter(Appendable out) {
    this(out, "  ");
  }

  public CodeWriter(Appendable out, String indent) {
    this(out, indent, Collections.<ClassName, String>emptyMap());
  }

  public CodeWriter(Appendable out, String indent, Map<ClassName, String> importedTypes) {
    this.out = checkNotNull(out, "out == null");
    this.indent = checkNotNull(indent, "indent == null");
    this.importedTypes = checkNotNull(importedTypes, "importedTypes == null");
  }

  public Map<ClassName, String> importedTypes() {
    return importedTypes;
  }

  public CodeWriter indent() {
    return indent(1);
  }

  public CodeWriter indent(int levels) {
    indentLevel += levels;
    return this;
  }

  public CodeWriter unindent() {
    return unindent(1);
  }

  public CodeWriter unindent(int levels) {
    checkArgument(indentLevel - levels >= 0, "cannot unindent %s from %s", levels, indentLevel);
    indentLevel -= levels;
    return this;
  }

  public CodeWriter pushPackage(String packageName) {
    checkState(this.packageName == null, "package already set: %s", this.packageName);
    this.packageName = checkNotNull(packageName, "packageName == null");
    return this;
  }

  public CodeWriter popPackage() {
    checkState(this.packageName != null, "package already set: %s", this.packageName);
    this.packageName = null;
    return this;
  }

  public CodeWriter pushType(TypeSpec type) {
    this.typeSpecStack.add(type);
    return this;
  }

  public CodeWriter popType() {
    this.typeSpecStack.remove(typeSpecStack.size() - 1);
    return this;
  }

  public void emitComment(CodeBlock codeBlock) throws IOException {
    trailingNewline = true; // Force the '//' prefix for the comment.
    comment = true;
    try {
      emit(codeBlock);
      emit("\n");
    } finally {
      comment = false;
    }
  }

  public void emitJavadoc(CodeBlock javadocCodeBlock) throws IOException {
    if (javadocCodeBlock.isEmpty()) return;

    emit("/**\n");
    javadoc = true;
    try {
      emit(javadocCodeBlock);
    } finally {
      javadoc = false;
    }
    emit(" */\n");
  }

  public void emitAnnotations(List<AnnotationSpec> annotations, boolean inline) throws IOException {
    for (AnnotationSpec annotationSpec : annotations) {
      annotationSpec.emit(this, inline);
      emit(inline ? " " : "\n");
    }
  }

  /**
   * Emits {@code modifiers} in the standard order. Modifiers in {@code implicitModifiers} will not
   * be emitted.
   */
  public void emitModifiers(Set<Modifier> modifiers, Set<Modifier> implicitModifiers)
      throws IOException {
    if (modifiers.isEmpty()) return;
    for (Modifier modifier : EnumSet.copyOf(modifiers)) {
      if (implicitModifiers.contains(modifier)) continue;
      emitAndIndent(modifier.name().toLowerCase(Locale.US));
      emitAndIndent(" ");
    }
  }

  public void emitModifiers(Set<Modifier> modifiers) throws IOException {
    emitModifiers(modifiers, Collections.<Modifier>emptySet());
  }

  /**
   * Emit type variables with their bounds. This should only be used when declaring type variables;
   * everywhere else bounds are omitted.
   */
  public void emitTypeVariables(List<TypeVariableName> typeVariables) throws IOException {
    if (typeVariables.isEmpty()) return;

    emit("<");
    boolean firstTypeVariable = true;
    for (TypeVariableName typeVariable : typeVariables) {
      if (!firstTypeVariable) emit(", ");
      emit("$L", typeVariable.name);
      boolean firstBound = true;
      for (TypeName bound : typeVariable.bounds) {
        emit(firstBound ? " extends $T" : " & $T", bound);
        firstBound = false;
      }
      firstTypeVariable = false;
    }
    emit(">");
  }

  public CodeWriter emit(String format, Object... args) throws IOException {
    return emit(CodeBlock.builder().add(format, args).build());
  }

  public CodeWriter emit(CodeBlock codeBlock) throws IOException {
    int a = 0;
    for (String part : codeBlock.formatParts) {
      switch (part) {
        case "$L":
          emitLiteral(codeBlock.args.get(a++));
          break;

        case "$N":
          emitAndIndent((String) codeBlock.args.get(a++));
          break;

        case "$S":
          String string = (String) codeBlock.args.get(a++);
          // Emit null as a literal null: no quotes.
          emitAndIndent(string != null
              ? stringLiteral(string)
              : "null");
          break;

        case "$T":
          TypeName typeName = (TypeName) codeBlock.args.get(a++);
          typeName.emit(this);
          break;

        case "$$":
          emitAndIndent("$");
          break;

        case "$>":
          indent();
          break;

        case "$<":
          unindent();
          break;

        case "$[":
          checkState(statementLine == -1, "statement enter $[ followed by statement enter $[");
          statementLine = 0;
          break;

        case "$]":
          checkState(statementLine != -1, "statement exit $] has no matching statement enter $[");
          if (statementLine > 0) {
            unindent(2); // End a multi-line statement. Decrease the indentation level.
          }
          statementLine = -1;
          break;

        default:
          emitAndIndent(part);
          break;
      }
    }
    return this;
  }

  private void emitLiteral(Object o) throws IOException {
    if (o instanceof TypeSpec) {
      TypeSpec typeSpec = (TypeSpec) o;
      typeSpec.emit(this, null, Collections.<Modifier>emptySet());
    } else if (o instanceof AnnotationSpec) {
      AnnotationSpec annotationSpec = (AnnotationSpec) o;
      annotationSpec.emit(this, true);
    } else if (o instanceof CodeBlock) {
      CodeBlock codeBlock = (CodeBlock) o;
      emit(codeBlock);
    } else {
      emitAndIndent(String.valueOf(o));
    }
  }

  /**
   * Returns the best name to identify {@code className} with in the current context. This uses the
   * available imports and the current scope to find the shortest name available. It does not honor
   * names visible due to inheritance.
   */
  String lookupName(ClassName className) {

    // Create a ClassName from the current stack.
    ClassName stackContext = null;
    for (TypeSpec typeSpec : typeSpecStack) {
      if (typeSpec.name == null) {
        // Anonymous classes don't have a name and aren't relevant for the context.
        continue;
      }
      if (stackContext == null) {
        stackContext = ClassName.get(packageName, typeSpec.name);
      } else {
        stackContext = stackContext.nestedClass(typeSpec.name);
      }
    }

    if (stackContext != null) {

      if (className.equals(stackContext)) {
        // Class is referring to itself, return it's simple name.
        return className.simpleName();
      }

      if (isNestedClass(className, stackContext)) {
        // Class is referring to an enclosing class, return it's simple name.
        return className.simpleName();
      }

      if (isNestedClass(stackContext, className)) {
        // Class is referring to a nested class, return it's qualified name from the nesting.
        return nestedName(stackContext, className);
      }

    }

    List<String> classNames = className.simpleNames();
    if (className.packageName().equals(packageName)) {
      // Determine the shortest unique reference.
      ClassName prefix = shortestUniqeReference(className);

      // Determine if the simple name has a name conflict.
      while (conflictsWithLocalName(prefix)) {
        prefix = prefix.enclosingClassName();
      }

      if (prefix.enclosingClassName() != null) {
        return nestedName(prefix.enclosingClassName(), className);
      } else {
        referencedNames.add(classNames.get(0));
        return Util.join(".", classNames);
      }

    } else {
      ClassName prefix = className;
      while (prefix != null && conflictsWithLocalName(prefix)) {
        prefix = prefix.enclosingClassName();
      }

      if (prefix == null) {
        // All names conflict, return the fully qualified name.
        return className.canonicalName;
      }

      String importedName = importedTypes.get(prefix);
      if (importedName != null) {
        if (!javadoc) importableTypes.add(prefix);
        referencedNames.add(importedName);

        if (prefix.enclosingClassName() != null) {
          return nestedName(prefix.enclosingClassName(), className);
        } else {
          return Util.join(".", classNames);
        }
      }

      // If the target class wasn't imported, perhaps its enclosing class was. Try that.
      ClassName enclosingClassName = prefix.enclosingClassName();
      if (enclosingClassName != null) {
        return lookupName(enclosingClassName) + "." + nestedName(enclosingClassName, className);
      }

      // Fall back to the fully-qualified name. Mark the type as importable for a future pass.
      if (!javadoc) importableTypes.add(prefix);
      return className.canonicalName;
    }
  }

  private static boolean isNestedClass(ClassName enclosingClass, ClassName nestedClass) {
    return nestedClass.canonicalName.startsWith(enclosingClass.canonicalName + ".");
  }

  private static String nestedName(ClassName enclosingClass, ClassName nestedClass) {
    return nestedClass.canonicalName.substring(enclosingClass.canonicalName.length() + 1);
  }

  /**
   * Returns true if the simple name of {@code className} conflicts with a visible class name in
   * the current scope and cannot be referred to by its short name.
   */
  private boolean conflictsWithLocalName(ClassName className) {
    for (int i = typeSpecStack.size() - 1; i >= 0; i--) {
      TypeSpec typeSpec = typeSpecStack.get(i);
      if (Objects.equals(typeSpec.name, className.simpleName())) {
        ClassName context = ClassName.get(packageName, typeSpecStack.get(0).name);
        for (int j = 1; j <= i; j++) {
          context = context.nestedClass(typeSpecStack.get(j).name);
        }
        return !context.equals(className);
      }
      for (TypeSpec visibleChild : typeSpec.typeSpecs) {
        if (Objects.equals(visibleChild.name, className.simpleName())) {
          ClassName context = ClassName.get(packageName, typeSpecStack.get(0).name);
          for (int j = 1; j <= i; j++) {
            context = context.nestedClass(typeSpecStack.get(j).name);
          }
          context = context.nestedClass(visibleChild.name);
          return !context.equals(className);
        }
      }
    }
    return false;
  }

  /**
   * Returns the shortest unique reference of {@code className} in the current nesting scope. This
   * reference is only useful when {@code className} is a member of one of the parents in the
   * current nesting scope or another class in the same package. For example suppose the current
   * scope is {@code AbstractMap.SimpleEntry}. This will return <code>List</code> for {@code List},
   * <code>AbstractMap</code> for {@code AbstractMap} and
   * <code>AbstractMap.SimpleImmutableEntry</code> for {@code AbstractMap.SimpleImmutableEntry}.
   */
  private ClassName shortestUniqeReference(ClassName className) {
    List<String> classNames = className.simpleNames();
    ClassName prefix = ClassName.get(className.packageName(), classNames.get(0));
    if (typeSpecStack.isEmpty()) return prefix;
    if (!classNames.get(0).equals(typeSpecStack.get(0).name)) return prefix;
    int size = Math.min(classNames.size(), typeSpecStack.size());
    for (int i = 1; i < size; i++) {
      String a = classNames.get(i);
      String b = typeSpecStack.get(i).name;
      prefix = prefix.nestedClass(a);
      if (!a.equals(b)) {
         return prefix;
      }
    }
    return prefix;
  }

  /**
   * Emits {@code s} with indentation as required. It's important that all code that writes to
   * {@link #out} does it through here, since we emit indentation lazily in order to avoid
   * unnecessary trailing whitespace.
   */
  CodeWriter emitAndIndent(String s) throws IOException {
    boolean first = true;
    for (String line : s.split("\n", -1)) {
      // Emit a newline character. Make sure blank lines in Javadoc & comments look good.
      if (!first) {
        if ((javadoc || comment) && trailingNewline) {
          emitIndentation();
          out.append(javadoc ? " *" : "//");
        }
        out.append('\n');
        trailingNewline = true;
        if (statementLine != -1) {
          if (statementLine == 0) {
            indent(2); // Begin multiple-line statement. Increase the indentation level.
          }
          statementLine++;
        }
      }

      first = false;
      if (line.isEmpty()) continue; // Don't indent empty lines.

      // Emit indentation and comment prefix if necessary.
      if (trailingNewline) {
        emitIndentation();
        if (javadoc) {
          out.append(" * ");
        } else if (comment) {
          out.append("// ");
        }
      }

      out.append(line);
      trailingNewline = false;
    }
    return this;
  }

  private void emitIndentation() throws IOException {
    for (int j = 0; j < indentLevel; j++) {
      out.append(indent);
    }
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, that type's first use is imported.
   */
  Map<ClassName, String> suggestedImports() {
    // Find the simple names that can be imported, and the classes that they target.
    Map<String, ClassName> simpleNameToType = new LinkedHashMap<>();
    for (ClassName className : importableTypes) {
      if (referencedNames.contains(className.simpleName())) continue;
      if (simpleNameToType.containsKey(className.simpleName())) continue;
      simpleNameToType.put(className.simpleName(), className);
    }

    // Invert the map.
    TreeMap<ClassName, String> typeToSimpleName = new TreeMap<>();
    for (Map.Entry<String, ClassName> entry : simpleNameToType.entrySet()) {
      typeToSimpleName.put(entry.getValue(), entry.getKey());
    }

    // TODO(jwilson): omit imports from java.lang, unless their simple names is also present in the
    //     current class's package. (Yuck.)

    return typeToSimpleName;
  }

  /** Returns the string literal representing {@code data}, including wrapping quotes. */
  String stringLiteral(String value) {
    StringBuilder result = new StringBuilder();
    result.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          result.append("\\\"");
          break;
        case '\\':
          result.append("\\\\");
          break;
        case '\b':
          result.append("\\b");
          break;
        case '\t':
          result.append("\\t");
          break;
        case '\n':
          result.append("\\n");
          if (i + 1 < value.length()) {
            result.append("\"\n").append(indent).append(indent).append("+ \"");
          }
          break;
        case '\f':
          result.append("\\f");
          break;
        case '\r':
          result.append("\\r");
          break;
        default:
          if (Character.isISOControl(c)) {
            new Formatter(result).format("\\u%04x", (int) c);
          } else {
            result.append(c);
          }
      }
    }
    result.append('"');
    return result.toString();
  }
}
