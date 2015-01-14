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

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Converts a {@link JavaFile} to a string suitable to both human- and javac-consumption. This
 * honors imports, indentation, and deferred variable names.
 */
final class CodeWriter {
  private final String indent = "  ";
  private final Appendable out;
  private int indentLevel;

  private boolean javadoc = false;
  private boolean comment = false;
  private String packageName;
  private final List<TypeSpec> typeSpecStack = new ArrayList<>();
  private final ImmutableMap<ClassName, String> importedTypes;
  private final Set<ClassName> importableTypes = new LinkedHashSet<>();
  private boolean trailingNewline;

  public CodeWriter(Appendable out) {
    this(out, ImmutableMap.<ClassName, String>of());
  }

  public CodeWriter(Appendable out, ImmutableMap<ClassName, String> importedTypes) {
    this.out = checkNotNull(out);
    this.importedTypes = checkNotNull(importedTypes);
  }

  public ImmutableMap<ClassName, String> importedTypes() {
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
    checkArgument(indentLevel - levels >= 0);
    indentLevel -= levels;
    return this;
  }

  public CodeWriter pushPackage(String packageName) {
    checkState(this.packageName == null);
    this.packageName = checkNotNull(packageName);
    return this;
  }

  public CodeWriter popPackage() {
    checkState(this.packageName != null);
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

  public void emitComment(Snippet snippet) throws IOException {
    trailingNewline = true; // Force the '//' prefix for the comment.
    comment = true;
    try {
      emit(snippet);
      emit("\n");
    } finally {
      comment = false;
    }
  }

  public void emitJavadoc(ImmutableList<Snippet> javadocSnippets) throws IOException {
    if (javadocSnippets.isEmpty()) return;

    emit("/**\n");
    javadoc = true;
    try {
      for (Snippet snippet : javadocSnippets) {
        emit(snippet);
      }
    } finally {
      javadoc = false;
    }
    emit(" */\n");
  }

  public void emitAnnotations(ImmutableList<AnnotationSpec> annotations, boolean inline)
      throws IOException {
    for (AnnotationSpec annotationSpec : annotations) {
      annotationSpec.emit(this, inline);
      emit(inline ? " " : "\n");
    }
  }

  /**
   * Emits {@code modifiers} in the standard order. Modifiers in {@code implicitModifiers} will not
   * be emitted.
   */
  public void emitModifiers(ImmutableSet<Modifier> modifiers,
      ImmutableSet<Modifier> implicitModifiers) throws IOException {
    if (!modifiers.isEmpty()) {
      for (Modifier modifier : EnumSet.copyOf(modifiers)) {
        if (implicitModifiers.contains(modifier)) continue;
        emitAndIndent(Ascii.toLowerCase(modifier.name()));
        emitAndIndent(" ");
      }
    }
  }

  public void emitModifiers(ImmutableSet<Modifier> modifiers) throws IOException {
    emitModifiers(modifiers, ImmutableSet.<Modifier>of());
  }

  /**
   * Emit type variables with their bounds. This should only be used when declaring type variables;
   * everywhere else bounds are omitted.
   */
  public void emitTypeVariables(ImmutableList<TypeVariable<?>> typeVariables) throws IOException {
    if (typeVariables.isEmpty()) return;

    emit("<");
    boolean firstTypeVariable = true;
    for (TypeVariable<?> typeVariable : typeVariables) {
      if (!firstTypeVariable) emit(", ");
      emit("$L", typeVariable.getName());
      boolean firstBound = true;
      for (Type bound : typeVariable.getBounds()) {
        if (isObject(bound)) continue;
        emit(firstBound ? " extends $T" : " & $T", bound);
        firstBound = false;
      }
      firstTypeVariable = false;
    }
    emit(">");
  }

  public CodeWriter emit(String format, Object... args) throws IOException {
    return emit(new Snippet(format, args));
  }

  public CodeWriter emit(Snippet snippet) throws IOException {
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
          emitAndIndent(stringLiteral(arg));
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

  private void emitLiteral(Object o) throws IOException {
    if (o instanceof TypeSpec) {
      TypeSpec typeSpec = (TypeSpec) o;
      typeSpec.emit(this, null);
    } else if (o instanceof AnnotationSpec) {
      AnnotationSpec annotationSpec = (AnnotationSpec) o;
      annotationSpec.emit(this, true);
    } else {
      emitAndIndent(String.valueOf(o));
    }
  }

  private CodeWriter emitType(Object arg) throws IOException {
    Type type = toType(arg);

    if (type instanceof Class<?>) {
      Class<?> classType = (Class<?>) type;
      if (classType.isPrimitive()) return emit(classType.getName());
      if (classType.isArray()) return emit("$T[]", classType.getComponentType());
      return emitType(ClassName.get(classType));

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      emitType(parameterizedType.getRawType());
      emitAndIndent("<");
      boolean firstParameter = true;
      for (Type parameter : parameterizedType.getActualTypeArguments()) {
        if (!firstParameter) emitAndIndent(", ");
        emitType(parameter);
        firstParameter = false;
      }
      emitAndIndent(">");
      return this;

    } else if (type instanceof WildcardType) {
      WildcardType wildcardName = (WildcardType) type;
      Type[] extendsBounds = wildcardName.getUpperBounds();
      Type[] superBounds = wildcardName.getLowerBounds();
      if (superBounds.length == 1) {
        return emit("? super $T", superBounds[0]);
      }
      checkArgument(extendsBounds.length == 1);
      return isObject(extendsBounds[0])
          ? emit("?")
          : emit("? extends $T", extendsBounds[0]);

    } else if (type instanceof TypeVariable<?>) {
      return emitAndIndent(((TypeVariable) type).getName());

    } else if (type instanceof ClassName) {
      return emitAndIndent(lookupName((ClassName) type));

    } else if (type instanceof GenericArrayType) {
      return emit("$T[]", ((GenericArrayType) type).getGenericComponentType());

    } else if (type instanceof IntersectionType) {
      boolean firstBound = true;
      for (Type bound : ((IntersectionType) type).getBounds()) {
        if (!firstBound) emit(" & ");
        emit("$T", bound);
        firstBound = false;
      }
      return this;
    }

    throw new UnsupportedOperationException("unexpected type: " + arg);
  }

  private boolean isObject(Type bound) {
    return bound == Object.class || bound.equals(ClassName.OBJECT);
  }

  /**
   * Returns the best name to identify {@code className} with in the current context. This uses the
   * available imports and the current scope to find the shortest name available. It does not honor
   * names visible due to inheritance.
   */
  private String lookupName(ClassName className) {
    // Different package than current? Just look for an import.
    if (!className.packageName().equals(packageName)) {
      if (conflictsWithLocalName(className)) {
        return className.toString(); // A local name conflicts? Use the fully-qualified name.
      }

      String importedName = importedTypes.get(className);
      if (importedName != null) {
        if (!javadoc) importableTypes.add(className);
        return importedName;
      }

      // If the target class wasn't imported, perhaps its enclosing class was. Try that.
      ClassName enclosingClassName = className.enclosingClassName();
      if (enclosingClassName != null) {
        return lookupName(enclosingClassName) + "." + className.simpleName();
      }

      // Fall back to the fully-qualified name. Mark the type as importable for a future pass.
      if (!javadoc) importableTypes.add(className);
      return className.toString();
    }

    // Look for the longest common prefix, which we can omit.
    ImmutableList<String> classNames = className.simpleNames();
    int prefixLength = commonPrefixLength(classNames);
    if (prefixLength == classNames.size()) {
      return className.simpleName(); // Special case: a class referring to itself!
    }

    return Joiner.on('.').join(classNames.subList(prefixLength, classNames.size()));
  }

  /**
   * Returns true if {@code className} conflicts with a visible class name in the current scope and
   * cannot be referred to by its short name.
   */
  private boolean conflictsWithLocalName(ClassName className) {
    for (TypeSpec typeSpec : typeSpecStack) {
      if (Objects.equals(typeSpec.name, className.simpleName())) return true;
      for (TypeSpec visibleChild : typeSpec.typeSpecs) {
        if (Objects.equals(visibleChild.name, className.simpleName())) return true;
      }
    }
    return false;
  }

  /**
   * Returns the common prefix of {@code classNames} and the current nesting scope. For example,
   * suppose the current scope is {@code AbstractMap.SimpleEntry}. This will return 0 for {@code
   * List}, 1 for {@code AbstractMap}, 1 for {@code AbstractMap.SimpleImmutableEntry}, and 2 for
   * {@code AbstractMap.SimpleEntry} itself.
   */
  private int commonPrefixLength(ImmutableList<String> classNames) {
    int size = Math.min(classNames.size(), typeSpecStack.size());
    for (int i = 0; i < size; i++) {
      String a = classNames.get(i);
      String b = typeSpecStack.get(i).name;
      if (!a.equals(b)) return i;
    }
    return size;
  }

  /**
   * Emits {@code s} with indentation as required. It's important that all code that writes to
   * {@link #out} does it through here, since we emit indentation lazily in order to avoid
   * unnecessary trailing whitespace.
   */
  private CodeWriter emitAndIndent(String s) throws IOException {
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

  private Type toType(Object arg) {
    if (arg instanceof Type) return (Type) arg;
    throw new IllegalArgumentException("expected type but was " + arg);
  }

  private void emitName(Object o) throws IOException {
    emitAndIndent(toName(o));
  }

  private String toName(Object o) {
    if (o instanceof String) return (String) o;
    if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
    if (o instanceof FieldSpec) return ((FieldSpec) o).name;
    if (o instanceof MethodSpec) return ((MethodSpec) o).name;
    if (o instanceof TypeSpec) return ((TypeSpec) o).name;
    throw new IllegalArgumentException("expected name but was " + o);
  }

  /**
   * Returns the types that should have been imported for this code. If there were any simple name
   * collisions, that type's first use is imported.
   */
  ImmutableMap<ClassName, String> suggestedImports() {
    // Find the simple names that can be imported, and the classes that they target.
    Map<String, ClassName> simpleNameToType = new LinkedHashMap<>();
    for (Type type : importableTypes) {
      if (!(type instanceof ClassName)) continue;
      ClassName className = (ClassName) type;
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

  /** Returns the string literal representing {@code data}, including wrapping quotes. */
  static String stringLiteral(String value) {
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
