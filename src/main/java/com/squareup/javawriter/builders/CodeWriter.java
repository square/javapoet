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

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.StringLiteral;
import com.squareup.javawriter.TypeName;
import com.squareup.javawriter.TypeNames;
import com.squareup.javawriter.WildcardName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converts a {@link JavaFile} to a string suitable to both human- and javac-consumption. This
 * honors imports, indentation, and deferred variable names.
 */
final class CodeWriter {
  private final String indent = "  ";
  private final StringBuilder out;
  private final ImmutableMap<ClassName, String> importedTypes;
  private final LinkedHashSet<TypeName> emittedTypes = new LinkedHashSet<>();
  private final List<TypeName> visibleTypes = new ArrayList<>();
  private int indentLevel;

  public CodeWriter(StringBuilder out, ImmutableMap<ClassName, String> importedTypes) {
    this.out = checkNotNull(out);
    this.importedTypes = checkNotNull(importedTypes);
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
    checkArgument(indentLevel - levels >= 0);
    indentLevel -= levels;
    return this;
  }

  public CodeWriter pushVisibleType(TypeName typeName) {
    visibleTypes.add(typeName);
    return this;
  }

  public CodeWriter popVisibleType(TypeName typeName) {
    checkArgument(visibleTypes.remove(typeName));
    return this;
  }

  public void emitAnnotations(ImmutableList<AnnotationSpec> annotations, boolean inline) {
    for (AnnotationSpec annotationSpec : annotations) {
      annotationSpec.emit(this, inline);
    }
  }

  /**
   * Emits {@code modifiers} in the standard order. Modifiers in {@code implicitModifiers} will not
   * be emitted.
   */
  public void emitModifiers(
      ImmutableSet<Modifier> modifiers, ImmutableSet<Modifier> implicitModifiers) {
    if (!modifiers.isEmpty()) {
      for (Modifier modifier : EnumSet.copyOf(modifiers)) {
        if (implicitModifiers.contains(modifier)) continue;
        emitAndIndent(Ascii.toLowerCase(modifier.name()));
        emitAndIndent(" ");
      }
    }
  }

  public void emitModifiers(ImmutableSet<Modifier> modifiers) {
    emitModifiers(modifiers, ImmutableSet.<Modifier>of());
  }

  public CodeWriter emit(String format, Object... args) {
    return emit(new Snippet(format, args));
  }

  public CodeWriter emit(Snippet snippet) {
    int a = 0;
    for (String part : snippet.formatParts) {
      switch (part) {
        case "$L":
          emitLiteral(snippet.args.get(a++));
          break;

        case "$N":
          emitName(snippet.args.get(a++));
          break;

        case "$S":
          String arg = String.valueOf(snippet.args.get(a++));
          emitAndIndent(StringLiteral.forValue(arg).literal());
          break;

        case "$T":
          emitType(snippet.args.get(a++));
          break;

        case "$$":
          emitAndIndent("$");
          break;

        default:
          emitAndIndent(part);
          break;
      }
    }
    return this;
  }

  private void emitLiteral(Object o) {
    if (o instanceof TypeSpec) {
      TypeSpec typeSpec = (TypeSpec) o;
      typeSpec.emit(this);
    } else {
      emitAndIndent(String.valueOf(o));
    }
  }

  private void emitType(Object arg) {
    TypeName typeName = toTypeName(arg);
    emittedTypes.add(typeName);

    // TODO(jwilson): replace instanceof nonsense with polymorphism!
    if (typeName instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
      emitType(parameterizedTypeName.type());
      emitAndIndent("<");
      boolean firstParameter = true;
      for (TypeName parameter : parameterizedTypeName.parameters()) {
        if (!firstParameter) emitAndIndent(", ");
        emitType(parameter);
        firstParameter = false;
      }
      emitAndIndent(">");
    } else if (typeName instanceof WildcardName) {
      WildcardName wildcardName = (WildcardName) typeName;
      TypeName extendsBound = wildcardName.extendsBound();
      TypeName superBound = wildcardName.superBound();
      if (ClassName.fromClass(Object.class).equals(extendsBound)) {
        emit("?");
      } else if (extendsBound != null) {
        emit("? extends $T", extendsBound);
      } else if (superBound != null) {
        emit("? super $T", superBound);
      }
      // TODO(jwilson): special case ? for List<?>.
    } else {
      String shortName = !visibleTypes.contains(typeName)
          ? importedTypes.get(typeName)
          : null;
      emitAndIndent(shortName != null ? shortName : typeName.toString());
    }
  }

  /** Emits {@code s} with indentation as required. */
  private void emitAndIndent(String s) {
    boolean first = true;
    for (String line : s.split("\n", -1)) {
      if (!first) out.append('\n');
      first = false;
      if (line.isEmpty()) continue; // Don't indent empty lines.
      emitIndentationIfNecessary();
      out.append(line);
    }
  }

  private void emitIndentationIfNecessary() {
    // Only emit indentation immediately after a '\n' character.
    if (out.length() <= 0 || out.charAt(out.length() - 1) != '\n') return;

    for (int j = 0; j < indentLevel; j++) {
      out.append(indent);
    }
  }

  private TypeName toTypeName(Object arg) {
    if (arg instanceof TypeName) return (TypeName) arg;
    if (arg instanceof Class<?>) return TypeNames.forClass((Class<?>) arg);
    throw new IllegalArgumentException("Expected type but was " + arg);
  }

  private void emitName(Object o) {
    emitAndIndent(toName(o));
  }

  private String toName(Object o) {
    // TODO(jwilson): implement deferred naming so that `new Name("public")` yields "public_" etc.
    if (o instanceof String) return (String) o;
    if (o instanceof Name) return ((Name) o).seed;
    if (o instanceof ParameterSpec) return ((ParameterSpec) o).name.seed;
    if (o instanceof FieldSpec) return ((FieldSpec) o).name.seed;
    if (o instanceof MethodSpec) return ((MethodSpec) o).name.seed;
    throw new IllegalArgumentException("Expected name but was " + o);
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, that type's first use is imported.
   */
  ImmutableMap<ClassName, String> suggestedImports() {
    // Find the simple names that can be imported, and the classes that they target.
    Map<String, ClassName> simpleNameToType = new LinkedHashMap<>();
    for (TypeName typeName : emittedTypes) {
      if (!(typeName instanceof ClassName)) continue;
      ClassName className = (ClassName) typeName;
      if (simpleNameToType.containsKey(className.simpleName())) continue;
      simpleNameToType.put(className.simpleName(), className);
    }

    // Invert the map.
    ImmutableSortedMap.Builder<ClassName, String> typeToSimpleName
        = ImmutableSortedMap.naturalOrder();
    for (Map.Entry<String, ClassName> entry : simpleNameToType.entrySet()) {
      typeToSimpleName.put(entry.getValue(), entry.getKey());
    }

    // TODO(jwilson): omit imports from java.lang, unless their simple names is also present in the
    //     current class's package. (Yuck.)

    return typeToSimpleName.build();
  }
}
