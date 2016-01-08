/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.squareup.javapoet;

import static java.lang.reflect.Modifier.isStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
public final class MemberRef implements Comparable<MemberRef> {
  /**
   * Defines all well-known member flavours, like {@code ENUM}, {@code FIELD} and {@code METHOD},
   * that can be refered to.
   */
  public enum Kind {
    ENUM, FIELD, METHOD
  }

  /** Simple getter using JavaPoet-model types only. */
  public static MemberRef get(Kind kind, ClassName type, String name, boolean statik,
      TypeName... typeArguments) {
    Util.checkNotNull(kind, "kind == null");
    Util.checkNotNull(type, "type == null");
    Util.checkNotNull(name, "name == null");
    Util.checkNotNull(typeArguments, "typeArguments == null");
    if (kind != Kind.METHOD) {
      Util.checkArgument(typeArguments.length == 0, "MemberRef %s mustn't have type args!", kind);
    }
    return new MemberRef(kind, type, name, statik, Arrays.asList(typeArguments));
  }

  public static MemberRef get(Enum<?> constant) {
    Util.checkNotNull(constant, "constant == null");
    ClassName type = ClassName.get(constant.getDeclaringClass());
    String name = constant.name();
    return get(Kind.ENUM, type, name, true);
  }

  public static MemberRef get(Field field) {
    Util.checkNotNull(field, "field == null");
    ClassName type = ClassName.get(field.getDeclaringClass());
    String name = field.getName();
    boolean statik = isStatic(field.getModifiers());
    return get(Kind.FIELD, type, name, statik);
  }

  public static MemberRef get(Method method, Type... types) {
    Util.checkNotNull(method, "method == null");
    Util.checkNotNull(types, "types == null");
    ClassName type = ClassName.get(method.getDeclaringClass());
    String name = method.getName();
    boolean statik = isStatic(method.getModifiers());
    return get(Kind.METHOD, type, name, statik, TypeName.list(types).toArray(new TypeName[0]));
  }

  public static MemberRef get(VariableElement variable) {
    Util.checkNotNull(variable, "variable == null");
    ClassName type = ClassName.get((TypeElement) variable.getEnclosingElement());
    String name = variable.getSimpleName().toString();
    boolean statik = variable.getModifiers().contains(Modifier.STATIC);
    if (variable.getKind() == ElementKind.ENUM_CONSTANT) return get(Kind.FIELD, type, name, statik);
    if (variable.getKind() == ElementKind.FIELD) return get(Kind.FIELD, type, name, statik);
    throw new IllegalArgumentException("unsupported element kind: " + variable.getKind());
  }

  public static MemberRef get(ExecutableElement executable, TypeMirror... types) {
    Util.checkNotNull(executable, "executable == null");
    Util.checkNotNull(types, "types == null");
    ClassName type = ClassName.get((TypeElement) executable.getEnclosingElement());
    String name = executable.getSimpleName().toString();
    boolean statik = executable.getModifiers().contains(Modifier.STATIC);
    return get(Kind.METHOD, type, name, statik, TypeName.list(types).toArray(new TypeName[0]));
  }

  public final Kind kind;
  public final ClassName type;
  public final String name;
  public final boolean isStatic;
  public final List<TypeName> typeArguments;

  MemberRef(Kind kind, ClassName type, String name, boolean statik) {
    this(kind, type, name, statik, Collections.<TypeName>emptyList());
  }

  MemberRef(Kind kind, ClassName type, String name, boolean statik, List<TypeName> typeArguments) {
    this.kind = kind;
    this.type = type;
    this.name = name;
    this.isStatic = statik;
    this.typeArguments = Util.immutableList(typeArguments);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    MemberRef r = (MemberRef) o;
    if (isStatic != r.isStatic) return false;
    if (!kind.equals(r.kind)) return false;
    if (!type.equals(r.type)) return false;
    if (!typeArguments.equals(r.typeArguments)) return false;
    return name.equals(r.name);
  }

  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isStatic ? 1731 : 1233);
    result = prime * result + kind.hashCode();
    result = prime * result + name.hashCode();
    result = prime * result + type.hashCode();
    result = prime * result + typeArguments.hashCode();
    return result;
  }

  @Override public String toString() {
    return type.canonicalName + "." + name;
  }

  @Override public int compareTo(MemberRef r) {
    return toString().compareTo(r.toString());
  }

  void emit(CodeWriter codeWriter) throws IOException {
    if (isStatic && !codeWriter.isStaticImported(this)) {
      codeWriter.emit("$T.", type);
    }
    if (kind == Kind.METHOD) {
      emitTypeArguments(codeWriter);
    }
    codeWriter.emit("$L", name);
  }

  void emitTypeArguments(CodeWriter codeWriter) throws IOException {
    if (typeArguments.isEmpty()) return;
    codeWriter.emit("<");
    codeWriter.emit("$T", typeArguments.get(0));
    for (int index = 1; index < typeArguments.size(); index++) {
      codeWriter.emit(", $T", typeArguments.get(index));
    }
    codeWriter.emit(">");
  }
}
