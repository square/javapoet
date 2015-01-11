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
package com.squareup.javawriter;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.EnumSet;
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
  private final StringBuilder out;
  private int indentLevel;

  private String packageName;
  private final List<TypeSpec> typeSpecStack = new ArrayList<>();
  private final ImmutableMap<ClassName, String> importedTypes;
  private final Set<ClassName> importableTypes = new LinkedHashSet<>();

  public CodeWriter(StringBuilder out) {
    this(out, ImmutableMap.<ClassName, String>of());
  }

  public CodeWriter(StringBuilder out, ImmutableMap<ClassName, String> importedTypes) {
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

  /**
   * Emit type variables with their bounds. This should only be used when declaring type variables;
   * everywhere else bounds are omitted.
   */
  public void emitTypeVariables(ImmutableList<TypeVariable<?>> typeVariables) {
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
      typeSpec.emit(this, null);
    } else {
      emitAndIndent(String.valueOf(o));
    }
  }

  private CodeWriter emitType(Object arg) {
    Type type = toType(arg);

    if (type instanceof Class<?>) {
      Class<?> classType = (Class<?>) type;
      if (classType.isPrimitive()) {
        if (boolean.class == classType) return emit("boolean");
        if (byte.class == classType) return emit("byte");
        if (short.class == classType) return emit("short");
        if (int.class == classType) return emit("int");
        if (long.class == classType) return emit("long");
        if (char.class == classType) return emit("char");
        if (float.class == classType) return emit("float");
        if (double.class == classType) return emit("double");
        if (void.class == classType) return emit("void");
      } else if (classType.isArray()) {
        return emit("$T[]", classType.getComponentType());
      } else {
        return emitType(ClassName.fromClass(classType));
      }
    }

    if (type instanceof ParameterizedType) {
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
    } else if (type instanceof WildcardType) {
      WildcardType wildcardName = (WildcardType) type;
      Type[] extendsBounds = wildcardName.getUpperBounds();
      Type[] superBounds = wildcardName.getLowerBounds();
      if (superBounds.length == 1) {
        emit("? super $T", superBounds[0]);
      } else if (extendsBounds.length == 1 && !isObject(extendsBounds[0])) {
        emit("? extends $T", extendsBounds[0]);
      } else {
        emit("?");
      }
    } else if (type instanceof TypeVariable<?>) {
      emit("$L", ((TypeVariable) type).getName());
    } else if (type instanceof ClassName) {
      emitAndIndent(lookupName((ClassName) type));
    } else if (type instanceof GenericArrayType) {
      emit("$T[]", ((GenericArrayType) type).getGenericComponentType());
    } else if (type instanceof IntersectionType) {
      boolean firstBound = true;
      for (Type bound : ((IntersectionType) type).getBounds()) {
        if (!firstBound) emit(" & ");
        emit("$T", bound);
        firstBound = false;
      }
    } else {
      throw new UnsupportedOperationException("unexpected type: " + arg);
    }
    return this;
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
        importableTypes.add(className);
        return importedName;
      }

      // If the target class wasn't imported, perhaps its enclosing class was. Try that.
      ClassName enclosingClassName = className.enclosingClassName().orNull();
      if (enclosingClassName != null) {
        return lookupName(enclosingClassName) + "." + className.simpleName();
      }

      // Fall back to the fully-qualified name. Mark the type as importable for a future pass.
      importableTypes.add(className);
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

  private Type toType(Object arg) {
    if (arg instanceof Type) return (Type) arg;
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
}
