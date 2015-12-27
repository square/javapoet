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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Member reference class.
 *
 * Use {@code $R} in format strings to insert member references into your generated code.
 *
 * @author Christian Stein
 */
public abstract class MemberRef implements Comparable<MemberRef> {

  /**
   * Defines all well-known member flavours, like {@code ENUM}, {@code FIELD} and {@code METHOD},
   * that can be refered to.
   */
  public enum Kind { ENUM, FIELD, METHOD }

  /** Simple getter using JavaPoet-model types only. */
  public static MemberRef get(Kind kind, ClassName type, String name, Set<Modifier> modifiers,
      TypeName... typeArguments) {
    Util.checkNotNull(kind, "kind == null");
    Util.checkNotNull(type, "type == null");
    Util.checkNotNull(name, "name == null");
    Util.checkNotNull(modifiers, "modifiers == null");
    Util.checkNotNull(typeArguments, "typeArguments == types");
    switch (kind) {
    case ENUM:
      return new EnumRef(type, name, modifiers);
    case FIELD:
      return new FieldRef(type, name, modifiers);
    case METHOD:
      return new MethodRef(type, name, modifiers, Arrays.asList(typeArguments));
    default:
      throw new IllegalArgumentException("unknown kind: " + kind);
    }
  }

  public static MemberRef get(Enum<?> constant) {
    Util.checkNotNull(constant, "constant == null");
    ClassName type = ClassName.get(constant.getDeclaringClass());
    String name = constant.name();
    Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
    return new EnumRef(type, name, modifiers);
  }

  public static MemberRef get(Field field) {
    Util.checkNotNull(field, "field == null");
    ClassName type = ClassName.get(field.getDeclaringClass());
    String name = field.getName();
    Set<Modifier> modifiers = Util.getModifiers(field.getModifiers());
    return new FieldRef(type, name, modifiers);
  }

  public static MemberRef get(Method method, Type... types) {
    Util.checkNotNull(method, "method == null");
    Util.checkNotNull(types, "types == null");
    ClassName type = ClassName.get(method.getDeclaringClass());
    String name = method.getName();
    Set<Modifier> modifiers = Util.getModifiers(method.getModifiers());
    return new MethodRef(type, name, modifiers, TypeName.list(types));
  }

  public static MemberRef get(VariableElement variable) {
    Util.checkNotNull(variable, "variable == null");
    ClassName type = ClassName.get((TypeElement) variable.getEnclosingElement());
    String name = variable.getSimpleName().toString();
    Set<Modifier> modifiers = variable.getModifiers();
    if (variable.getKind() == ElementKind.ENUM_CONSTANT) return new EnumRef(type, name, modifiers);
    if (variable.getKind() == ElementKind.FIELD) return new FieldRef(type, name, modifiers);
    throw new IllegalArgumentException("unsupported element kind: " + variable.getKind());
  }

  public static MemberRef get(ExecutableElement executable, TypeMirror... types) {
    Util.checkNotNull(executable, "executable == null");
    Util.checkNotNull(types, "types == null");
    ClassName type = ClassName.get((TypeElement) executable.getEnclosingElement());
    String name = executable.getSimpleName().toString();
    return new MethodRef(type, name, executable.getModifiers(), TypeName.list(types));
  }

  public final Kind kind;
  public final ClassName type;
  public final String name;
  public final Set<Modifier> modifiers;
  public final boolean isStatic;

  private MemberRef(Kind kind, ClassName type, String name, Set<Modifier> modifiers) {
    this.kind = kind;
    this.type = type;
    this.name = name;
    this.modifiers = Util.immutableSet(modifiers);
    this.isStatic = this.modifiers.contains(Modifier.STATIC);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isStatic ? 1731 : 1233);
    result = prime * result + kind.hashCode();
    result = prime * result + name.hashCode();
    result = prime * result + type.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      new CodeWriter(out).emit("$R", this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  @Override public int compareTo(MemberRef o) {
    return name.compareTo(o.name);
  }

  abstract void emit(CodeWriter codeWriter) throws IOException;

  static final class EnumRef extends MemberRef {
    EnumRef(ClassName type, String name, Set<Modifier> modifiers) {
      super(Kind.ENUM, type, name, modifiers);
    }

    void emit(CodeWriter codeWriter) throws IOException {
      if (codeWriter.isStaticImported(this)) {
        codeWriter.emit("$L", name);
      } else {
        codeWriter.emit("$T.$L", type, name);
      }
    }
  }

  static final class FieldRef extends MemberRef {
    FieldRef(ClassName type, String name, Set<Modifier> modifiers) {
      super(Kind.FIELD, type, name, modifiers);
    }

    void emit(CodeWriter codeWriter) throws IOException {
      if (isStatic && !codeWriter.isStaticImported(this)) {
        codeWriter.emit("$T.", type);
      }
      codeWriter.emit("$L", name);
    }
  }

  static final class MethodRef extends MemberRef {
    final List<TypeName> typeArguments;

    MethodRef(ClassName type, String name, Set<Modifier> modifiers, List<TypeName> typeArguments) {
      super(Kind.METHOD, type, name, modifiers);
      this.typeArguments = Util.immutableList(typeArguments);
    }

    void emit(CodeWriter codeWriter) throws IOException {
      if (isStatic && !codeWriter.isStaticImported(this)) {
        codeWriter.emit("$T.", type);
      }
      emitTypeArguments(codeWriter);
      codeWriter.emit("$L", name);
    }

    private void emitTypeArguments(CodeWriter codeWriter) throws IOException {
      if (typeArguments.isEmpty()) return;
      codeWriter.emit("<");
      codeWriter.emit("$T", typeArguments.get(0));
      for (int index = 1; index < typeArguments.size(); index++) {
        codeWriter.emit(", $T", typeArguments.get(index));
      }
      codeWriter.emit(">");
    }
  }

}
