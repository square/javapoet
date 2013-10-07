// Copyright 2013 Square, Inc.
package com.squareup.javawriter;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.SYNCHRONIZED;
import static javax.lang.model.element.Modifier.TRANSIENT;
import static javax.lang.model.element.Modifier.VOLATILE;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;

/** A utility class which aids in generating Java source files. */
public class JavaWriter implements Closeable {
  private static final Pattern TYPE_PATTERN = Pattern.compile("(?:[\\w$]+\\.)*([\\w\\.*$]+)");
  private static final int MAX_SINGLE_LINE_ATTRIBUTES = 3;
  private static final String INDENT = "  ";

  /** Map fully qualified type names to their short names. */
  private final Map<String, String> importedTypes = new LinkedHashMap<String, String>();

  private String packagePrefix;
  private final List<Scope> scopes = new ArrayList<Scope>();
  private final Writer out;
  private boolean isCompressingTypes = true;
  private String indent = INDENT;

  /**
   * @param out the stream to which Java source will be written. This should be a buffered stream.
   */
  public JavaWriter(Writer out) {
    this.out = out;
  }

  public void setCompressingTypes(boolean isCompressingTypes) {
    this.isCompressingTypes = isCompressingTypes;
  }

  public boolean isCompressingTypes() {
    return isCompressingTypes;
  }

  public void setIndent(String indent) {
    this.indent = indent;
  }

  public String getIndent() {
    return indent;
  }

  /** Emit a package declaration and empty line. */
  public JavaWriter emitPackage(String packageName) throws IOException {
    if (this.packagePrefix != null) {
      throw new IllegalStateException();
    }
    if (packageName.isEmpty()) {
      this.packagePrefix = "";
    } else {
      out.write("package ");
      out.write(packageName);
      out.write(";\n\n");
      this.packagePrefix = packageName + ".";
    }
    return this;
  }

  /**
   * Emit an import for each {@code type} provided. For the duration of the file, all references to
   * these classes will be automatically shortened.
   */
  public JavaWriter emitImports(String... types) throws IOException {
    return emitImports(Arrays.asList(types));
  }

  /**
   * Emit an import for each {@code type} in the provided {@code Collection}. For the duration of
   * the file, all references to these classes will be automatically shortened.
   */
  public JavaWriter emitImports(Collection<String> types) throws IOException {
    for (String type : new TreeSet<String>(types)) {
      Matcher matcher = TYPE_PATTERN.matcher(type);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(type);
      }
      if (importedTypes.put(type, matcher.group(1)) != null) {
        throw new IllegalArgumentException(type);
      }
      out.write("import ");
      out.write(type);
      out.write(";\n");
    }
    return this;
  }

  /**
   * Emit a static import for each {@code type} provided. For the duration of the file,
   * all references to these classes will be automatically shortened.
   */
  public JavaWriter emitStaticImports(String... types) throws IOException {
    return emitStaticImports(Arrays.asList(types));
  }

  /**
   * Emit a static import for each {@code type} in the provided {@code Collection}. For the
   * duration of the file, all references to these classes will be automatically shortened.
   */
  public JavaWriter emitStaticImports(Collection<String> types) throws IOException {
    for (String type : new TreeSet<String>(types)) {
      Matcher matcher = TYPE_PATTERN.matcher(type);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(type);
      }
      if (importedTypes.put(type, matcher.group(1)) != null) {
        throw new IllegalArgumentException(type);
      }
      out.write("import static ");
      out.write(type);
      out.write(";\n");
    }
    return this;
  }

  /**
   * Emits a name like {@code java.lang.String} or {@code java.util.List<java.lang.String>},
   * compressing it with imports if possible. Type compression will only be enabled if
   * {@link #isCompressingTypes} is true.
   */
  private JavaWriter emitCompressedType(String type) throws IOException {
    if (isCompressingTypes) {
      out.write(compressType(type));
    } else {
      out.write(type);
    }
    return this;
  }

  /** Try to compress a fully-qualified class name to only the class name. */
  public String compressType(String type) {
    StringBuilder sb = new StringBuilder();
    if (this.packagePrefix == null) {
      throw new IllegalStateException();
    }

    Matcher m = TYPE_PATTERN.matcher(type);
    int pos = 0;
    while (true) {
      boolean found = m.find(pos);

      // Copy non-matching characters like "<".
      int typeStart = found ? m.start() : type.length();
      sb.append(type, pos, typeStart);

      if (!found) {
        break;
      }

      // Copy a single class name, shortening it if possible.
      String name = m.group(0);
      String imported = importedTypes.get(name);
      if (imported != null) {
        sb.append(imported);
      } else if (isClassInPackage(name)) {
        String compressed = name.substring(packagePrefix.length());
        if (isAmbiguous(compressed)) {
          sb.append(name);
        } else {
          sb.append(compressed);
        }
      } else if (name.startsWith("java.lang.")) {
        sb.append(name.substring("java.lang.".length()));
      } else {
        sb.append(name);
      }
      pos = m.end();
    }
    return sb.toString();
  }

  private boolean isClassInPackage(String name) {
    if (name.startsWith(packagePrefix)) {
      if (name.indexOf('.', packagePrefix.length()) == -1) {
        return true;
      }
      int index = name.indexOf('.');
      if (name.substring(index + 1, index + 2).matches("[A-Z]")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the imports contain a class with same simple name as {@code compressed}.
   *
   * @param compressed simple name of the type
   */
  private boolean isAmbiguous(String compressed) {
    return importedTypes.values().contains(compressed);
  }

  /**
   * Emits an initializer declaration.
   *
   * @param isStatic true if it should be an static initializer, false for an instance initializer.
   */
  public JavaWriter beginInitializer(boolean isStatic) throws IOException {
    indent();
    if (isStatic) {
      out.write("static");
      out.write(" {\n");
    } else {
      out.write("{\n");
    }
    pushScope(Scope.INITIALIZER);
    return this;
  }

  /** Ends the current initializer declaration. */
  public JavaWriter endInitializer() throws IOException {
    popScope(Scope.INITIALIZER);
    indent();
    out.write("}\n");
    return this;
  }

 /**
  * Emits a type declaration.
  *
  * @param kind such as "class", "interface" or "enum".
  */
  public JavaWriter beginType(String type, String kind) throws IOException {
    return beginType(type, kind, EnumSet.noneOf(Modifier.class), null);
  }

  /**
   * @deprecated Use {@link #beginType(String, String, Set)}
   */
  @Deprecated
  public JavaWriter beginType(String type, String kind, int modifiers) throws IOException {
    return beginType(type, kind, modifiersAsSet(modifiers), null);
  }

  /**
   * Emits a type declaration.
   *
   * @param kind such as "class", "interface" or "enum".
   */
  public JavaWriter beginType(String type, String kind, Set<Modifier> modifiers)
      throws IOException {
    return beginType(type, kind, modifiers, null);
  }

  /**
   * @deprecated Use {@link #beginType(String, String, Set, String, String...)}
   */
  @Deprecated
  public JavaWriter beginType(String type, String kind, int modifiers, String extendsType,
      String... implementsTypes) throws IOException {
    return beginType(type, kind, modifiersAsSet(modifiers), extendsType, implementsTypes);
  }

  /**
   * Emits a type declaration.
   *
   * @param kind such as "class", "interface" or "enum".
   * @param extendsType the class to extend, or null for no extends clause.
   */
  public JavaWriter beginType(String type, String kind, Set<Modifier> modifiers, String extendsType,
      String... implementsTypes) throws IOException {
    indent();
    emitModifiers(modifiers);
    out.write(kind);
    out.write(" ");
    emitCompressedType(type);
    if (extendsType != null) {
      out.write(" extends ");
      emitCompressedType(extendsType);
    }
    if (implementsTypes.length > 0) {
      out.write("\n");
      indent();
      out.write("    implements ");
      for (int i = 0; i < implementsTypes.length; i++) {
        if (i != 0) {
          out.write(", ");
        }
        emitCompressedType(implementsTypes[i]);
      }
    }
    out.write(" {\n");
    pushScope(Scope.TYPE_DECLARATION);
    return this;
  }

  /** Completes the current type declaration. */
  public JavaWriter endType() throws IOException {
    popScope(Scope.TYPE_DECLARATION);
    indent();
    out.write("}\n");
    return this;
  }

  /** Emits a field declaration. */
  public JavaWriter emitField(String type, String name) throws IOException {
    return emitField(type, name, EnumSet.noneOf(Modifier.class), null);
  }

  /**
   * @deprecated Use {@link #emitField(String, String, Set)}.
   */
  @Deprecated
  public JavaWriter emitField(String type, String name, int modifiers) throws IOException {
    return emitField(type, name, modifiersAsSet(modifiers), null);
  }

  /** Emits a field declaration. */
  public JavaWriter emitField(String type, String name, Set<Modifier> modifiers)
      throws IOException {
    return emitField(type, name, modifiers, null);
  }

  /**
   * @deprecated Use {@link #emitField(String, String, Set, String)}.
   */
  @Deprecated
  public JavaWriter emitField(String type, String name, int modifiers, String initialValue)
      throws IOException {
    return emitField(type, name, modifiersAsSet(modifiers), initialValue);
  }

  public JavaWriter emitField(String type, String name, Set<Modifier> modifiers,
      String initialValue) throws IOException {
    indent();
    emitModifiers(modifiers);
    emitCompressedType(type);
    out.write(" ");
    out.write(name);

    if (initialValue != null) {
      out.write(" = ");
      out.write(initialValue);
    }
    out.write(";\n");
    return this;
  }

  /**
   * @deprecated Use {@link #beginMethod(String, String, Set, String...)}.
   */
  @Deprecated
  public JavaWriter beginMethod(String returnType, String name, int modifiers, String... parameters)
      throws IOException {
    return beginMethod(returnType, name, modifiersAsSet(modifiers), Arrays.asList(parameters),
        null);
  }

  /**
   * Emit a method declaration.
   *
   * @param returnType the method's return type, or null for constructors.
   * @param name the method name, or the fully qualified class name for constructors.
   * @param modifiers the set of modifiers to be applied to the method
   * @param parameters alternating parameter types and names.
   */
  public JavaWriter beginMethod(String returnType, String name, Set<Modifier> modifiers,
      String... parameters) throws IOException {
    return beginMethod(returnType, name, modifiers, Arrays.asList(parameters), null);
  }

  /**
   * @deprecated Use {@link #beginMethod(String, String, Set, List, List)}.
   */
  @Deprecated
  public JavaWriter beginMethod(String returnType, String name, int modifiers,
      List<String> parameters, List<String> throwsTypes) throws IOException {
    return beginMethod(returnType, name, modifiersAsSet(modifiers), parameters, throwsTypes);
  }

  /**
   * Emit a method declaration.
   *
   * @param returnType the method's return type, or null for constructors.
   * @param name the method name, or the fully qualified class name for constructors.
   * @param modifiers the set of modifiers to be applied to the method
   * @param parameters alternating parameter types and names.
   * @param throwsTypes the classes to throw, or null for no throws clause.
   */
  public JavaWriter beginMethod(String returnType, String name, Set<Modifier> modifiers,
      List<String> parameters, List<String> throwsTypes) throws IOException {
    indent();
    emitModifiers(modifiers);
    if (returnType != null) {
      emitCompressedType(returnType);
      out.write(" ");
      out.write(name);
    } else {
      emitCompressedType(name);
    }
    out.write("(");
    if (parameters != null) {
      for (int p = 0; p < parameters.size();) {
        if (p != 0) {
          out.write(", ");
        }
        emitCompressedType(parameters.get(p++));
        out.write(" ");
        emitCompressedType(parameters.get(p++));
      }
    }
    out.write(")");
    if (throwsTypes != null && throwsTypes.size() > 0) {
      out.write("\n");
      indent();
      out.write("    throws ");
      for (int i = 0; i < throwsTypes.size(); i++) {
        if (i != 0) {
          out.write(", ");
        }
        emitCompressedType(throwsTypes.get(i));
      }
    }
    if (modifiers.contains(ABSTRACT)) {
      out.write(";\n");
      pushScope(Scope.ABSTRACT_METHOD);
    } else {
      out.write(" {\n");
      pushScope(Scope.NON_ABSTRACT_METHOD);
    }
    return this;
  }

  /** Emits some Javadoc comments with line separated by {@code \n}. */
  public JavaWriter emitJavadoc(String javadoc, Object... params) throws IOException {
    String formatted = String.format(javadoc, params);

    indent();
    out.write("/**\n");
    for (String line : formatted.split("\n")) {
      indent();
      out.write(" * ");
      out.write(line);
      out.write("\n");
    }
    indent();
    out.write(" */\n");
    return this;
  }

  /** Emits a single line comment. */
  public JavaWriter emitSingleLineComment(String comment, Object... args) throws IOException {
    indent();
    out.write("// ");
    out.write(String.format(comment, args));
    out.write("\n");
    return this;
  }

  public JavaWriter emitEmptyLine() throws IOException {
    out.write("\n");
    return this;
  }

  public JavaWriter emitEnumValue(String name) throws IOException {
    indent();
    out.write(name);
    out.write(",\n");
    return this;
  }

  /** Equivalent to {@code annotation(annotation, emptyMap())}. */
  public JavaWriter emitAnnotation(String annotation) throws IOException {
    return emitAnnotation(annotation, Collections.<String, Object>emptyMap());
  }

  /** Equivalent to {@code annotation(annotationType.getName(), emptyMap())}. */
  public JavaWriter emitAnnotation(Class<? extends Annotation> annotationType) throws IOException {
    return emitAnnotation(type(annotationType), Collections.<String, Object>emptyMap());
  }

  /**
   * Annotates the next element with {@code annotationType} and a {@code value}.
   *
   * @param value an object used as the default (value) parameter of the annotation. The value will
   *     be encoded using Object.toString(); use {@link #stringLiteral} for String values. Object
   *     arrays are written one element per line.
   */
  public JavaWriter emitAnnotation(Class<? extends Annotation> annotationType, Object value)
      throws IOException {
    return emitAnnotation(type(annotationType), value);
  }

  /**
   * Annotates the next element with {@code annotation} and a {@code value}.
   *
   * @param value an object used as the default (value) parameter of the annotation. The value will
   *     be encoded using Object.toString(); use {@link #stringLiteral} for String values. Object
   *     arrays are written one element per line.
   */
  public JavaWriter emitAnnotation(String annotation, Object value) throws IOException {
    indent();
    out.write("@");
    emitCompressedType(annotation);
    out.write("(");
    emitAnnotationValue(value);
    out.write(")");
    out.write("\n");
    return this;
  }

  /** Equivalent to {@code annotation(annotationType.getName(), attributes)}. */
  public JavaWriter emitAnnotation(Class<? extends Annotation> annotationType,
      Map<String, ?> attributes) throws IOException {
    return emitAnnotation(type(annotationType), attributes);
  }

  /**
   * Annotates the next element with {@code annotation} and {@code attributes}.
   *
   * @param attributes a map from annotation attribute names to their values. Values are encoded
   *     using Object.toString(); use {@link #stringLiteral} for String values. Object arrays are
   *     written one element per line.
   */
  public JavaWriter emitAnnotation(String annotation, Map<String, ?> attributes)
      throws IOException {
    indent();
    out.write("@");
    emitCompressedType(annotation);
    switch (attributes.size()) {
      case 0:
        break;
      case 1:
        Entry<String, ?> onlyEntry = attributes.entrySet().iterator().next();
        out.write("(");
        if (!"value".equals(onlyEntry.getKey())) {
          out.write(onlyEntry.getKey());
          out.write(" = ");
        }
        emitAnnotationValue(onlyEntry.getValue());
        out.write(")");
        break;
      default:
        boolean split = attributes.size() > MAX_SINGLE_LINE_ATTRIBUTES
            || containsArray(attributes.values());
        out.write("(");
        pushScope(Scope.ANNOTATION_ATTRIBUTE);
        String separator = split ? "\n" : "";
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
          out.write(separator);
          separator = split ? ",\n" : ", ";
          if (split) {
            indent();
          }
          out.write(entry.getKey());
          out.write(" = ");
          Object value = entry.getValue();
          emitAnnotationValue(value);
        }
        popScope(Scope.ANNOTATION_ATTRIBUTE);
        if (split) {
          out.write("\n");
          indent();
        }
        out.write(")");
        break;
    }
    out.write("\n");
    return this;
  }

  private boolean containsArray(Collection<?> values) {
    for (Object value : values) {
      if (value instanceof Object[]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Writes a single annotation value. If the value is an array, each element in the array will be
   * written to its own line.
   */
  private JavaWriter emitAnnotationValue(Object value) throws IOException {
    if (value instanceof Object[]) {
      out.write("{");
      boolean firstValue = true;
      pushScope(Scope.ANNOTATION_ARRAY_VALUE);
      for (Object o : ((Object[]) value)) {
        if (firstValue) {
          firstValue = false;
          out.write("\n");
        } else {
          out.write(",\n");
        }
        indent();
        out.write(o.toString());
      }
      popScope(Scope.ANNOTATION_ARRAY_VALUE);
      out.write("\n");
      indent();
      out.write("}");
    } else {
      out.write(value.toString());
    }
    return this;
  }

  /**
   * @param pattern a code pattern like "int i = %s". Newlines will be further indented. Should not
   *     contain trailing semicolon.
   */
  public JavaWriter emitStatement(String pattern, Object... args) throws IOException {
    checkInMethod();
    String[] lines = String.format(pattern, args).split("\n", -1);
    indent();
    out.write(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      out.write("\n");
      hangingIndent();
      out.write(lines[i]);
    }
    out.write(";\n");
    return this;
  }

  /**
   * @param controlFlow the control flow construct and its code, such as "if (foo == 5)". Shouldn't
   *     contain braces or newline characters.
   */
  public JavaWriter beginControlFlow(String controlFlow) throws IOException {
    checkInMethod();
    indent();
    out.write(controlFlow);
    out.write(" {\n");
    pushScope(Scope.CONTROL_FLOW);
    return this;
  }

  /**
   * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
   *     Shouldn't contain braces or newline characters.
   */
  public JavaWriter nextControlFlow(String controlFlow) throws IOException {
    popScope(Scope.CONTROL_FLOW);
    indent();
    pushScope(Scope.CONTROL_FLOW);
    out.write("} ");
    out.write(controlFlow);
    out.write(" {\n");
    return this;
  }

  public JavaWriter endControlFlow() throws IOException {
    return endControlFlow(null);
  }

  /**
   * @param controlFlow the optional control flow construct and its code, such as
   *     "while(foo == 20)". Only used for "do/while" control flows.
   */
  public JavaWriter endControlFlow(String controlFlow) throws IOException {
    popScope(Scope.CONTROL_FLOW);
    indent();
    if (controlFlow != null) {
      out.write("} ");
      out.write(controlFlow);
      out.write(";\n");
    } else {
      out.write("}\n");
    }
    return this;
  }

  /** Completes the current method declaration. */
  public JavaWriter endMethod() throws IOException {
    Scope popped = popScope();
    if (popped == Scope.NON_ABSTRACT_METHOD) {
      indent();
      out.write("}\n");
    } else if (popped != Scope.ABSTRACT_METHOD) {
      throw new IllegalStateException();
    }
    return this;
  }

  /** Returns the string literal representing {@code data}, including wrapping quotes. */
  public static String stringLiteral(String data) {
    StringBuilder result = new StringBuilder();
    result.append('"');
    for (int i = 0; i < data.length(); i++) {
      char c = data.charAt(i);
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
            result.append(String.format("\\u%04x", (int) c));
          } else {
            result.append(c);
          }
      }
    }
    result.append('"');
    return result.toString();
  }

  /** Build a string representation of a type and optionally its generic type arguments. */
  public static String type(Class<?> raw, String... parameters) {
    if (parameters.length == 0) {
      return raw.getCanonicalName();
    }
    if (raw.getTypeParameters().length != parameters.length) {
      throw new IllegalArgumentException();
    }
    StringBuilder result = new StringBuilder();
    result.append(raw.getCanonicalName());
    result.append("<");
    result.append(parameters[0]);
    for (int i = 1; i < parameters.length; i++) {
      result.append(", ");
      result.append(parameters[i]);
    }
    result.append(">");
    return result.toString();
  }

  @Override public void close() throws IOException {
    out.close();
  }

  /** Emits the modifiers to the writer. */
  private void emitModifiers(Set<Modifier> modifiers) throws IOException {
    // Use an EnumSet to ensure the proper ordering
    if (!(modifiers instanceof EnumSet)) {
      modifiers = EnumSet.copyOf(modifiers);
    }
    for (Modifier modifier : modifiers) {
      out.append(modifier.toString()).append(' ');
    }
  }

  /**
   * Returns a set of modifiers for an {@code int} encoded with the values in
   * {@link java.lang.reflect.Modifier}.
   */
  private static EnumSet<Modifier> modifiersAsSet(int modifiers) {
    EnumSet<Modifier> modifierSet = EnumSet.noneOf(Modifier.class);
    if ((modifiers & java.lang.reflect.Modifier.PUBLIC) != 0) {
      modifierSet.add(PUBLIC);
    }
    if ((modifiers & java.lang.reflect.Modifier.PRIVATE) != 0) {
      modifierSet.add(PRIVATE);
    }
    if ((modifiers & java.lang.reflect.Modifier.PROTECTED) != 0) {
      modifierSet.add(PROTECTED);
    }
    if ((modifiers & java.lang.reflect.Modifier.STATIC) != 0) {
      modifierSet.add(STATIC);
    }
    if ((modifiers & java.lang.reflect.Modifier.FINAL) != 0) {
      modifierSet.add(FINAL);
    }
    if ((modifiers & java.lang.reflect.Modifier.ABSTRACT) != 0) {
      modifierSet.add(ABSTRACT);
    }
    if ((modifiers & java.lang.reflect.Modifier.SYNCHRONIZED) != 0) {
      modifierSet.add(SYNCHRONIZED);
    }
    if ((modifiers & java.lang.reflect.Modifier.TRANSIENT) != 0) {
      modifierSet.add(TRANSIENT);
    }
    if ((modifiers & java.lang.reflect.Modifier.VOLATILE) != 0) {
      modifierSet.add(VOLATILE);
    }
    return modifierSet;
  }

  private void indent() throws IOException {
    for (int i = 0, count = scopes.size(); i < count; i++) {
      out.write(indent);
    }
  }

  private void hangingIndent() throws IOException {
    for (int i = 0, count = scopes.size() + 2; i < count; i++) {
      out.write(indent);
    }
  }

  private void checkInMethod() {
    Scope scope = peekScope();
    if (scope != Scope.NON_ABSTRACT_METHOD && scope != Scope.CONTROL_FLOW
        && scope != Scope.INITIALIZER) {
      throw new IllegalArgumentException();
    }
  }

  private void pushScope(Scope pushed) {
    scopes.add(pushed);
  }

  private Scope peekScope() {
    return scopes.get(scopes.size() - 1);
  }

  private Scope popScope() {
    return scopes.remove(scopes.size() - 1);
  }

  private void popScope(Scope expected) {
    if (scopes.remove(scopes.size() - 1) != expected) {
      throw new IllegalStateException();
    }
  }

  private enum Scope {
    TYPE_DECLARATION,
    ABSTRACT_METHOD,
    NON_ABSTRACT_METHOD,
    CONTROL_FLOW,
    ANNOTATION_ATTRIBUTE,
    ANNOTATION_ARRAY_VALUE,
    INITIALIZER
  }
}
