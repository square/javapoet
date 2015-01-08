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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final Type type;
  public final ClassName name;
  public final TypeName supertype;
  public final Snippet anonymousTypeArguments;
  public final ImmutableList<FieldSpec> fieldSpecs;
  public final ImmutableList<MethodSpec> methodSpecs;

  private TypeSpec(Builder builder) {
    checkArgument(builder.name != null ^ builder.anonymousTypeArguments != null,
        "types must have either a name or anonymous type arguments");
    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.type = checkNotNull(builder.type);
    this.name = builder.name;
    this.supertype = builder.supertype;
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.fieldSpecs = ImmutableList.copyOf(builder.fieldSpecs);
    this.methodSpecs = ImmutableList.copyOf(builder.methodSpecs);
  }

  void emit(CodeWriter codeWriter) {
    if (anonymousTypeArguments != null) {
      codeWriter.emit("new $T(", supertype);
      codeWriter.emit(anonymousTypeArguments);
      codeWriter.emit(") {\n");
    } else {
      codeWriter.emitAnnotations(annotations, false);
      codeWriter.emitModifiers(modifiers);
      codeWriter.emit("class $L {\n", name.simpleName());
    }

    codeWriter.indent();

    boolean firstMember = true;
    for (FieldSpec fieldSpec : fieldSpecs) {
      if (!firstMember) codeWriter.emit("\n");
      fieldSpec.emit(codeWriter);
      firstMember = false;
    }
    for (MethodSpec methodSpec : methodSpecs) {
      if (!firstMember) codeWriter.emit("\n");
      methodSpec.emit(codeWriter);
      firstMember = false;
    }

    codeWriter.unindent();
    codeWriter.emit(anonymousTypeArguments != null ? "}" : "}\n");
  }

  public static enum Type {
    CLASS, INTERFACE, ENUM
  }

  public static final class Builder {
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private Type type = Type.CLASS;
    private ClassName name;
    private TypeName supertype = ClassName.fromClass(Object.class);
    private Snippet anonymousTypeArguments;
    private List<FieldSpec> fieldSpecs = new ArrayList<>();
    private List<MethodSpec> methodSpecs = new ArrayList<>();

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Class<? extends Annotation> annotation) {
      this.annotations.add(AnnotationSpec.of(annotation));
      return this;
    }

    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder type(Type type) {
      this.type = type;
      return this;
    }

    public Builder name(ClassName name) {
      this.name = name;
      return this;
    }

    public Builder supertype(TypeName supertype) {
      this.supertype = supertype;
      return this;
    }

    public Builder anonymousTypeArguments(String format, Object... args) {
      this.anonymousTypeArguments = new Snippet(format, args);
      return this;
    }

    public Builder addMethod(MethodSpec methodSpec) {
      methodSpecs.add(methodSpec);
      return this;
    }

    public Builder addField(FieldSpec fieldSpec) {
      fieldSpecs.add(fieldSpec);
      return this;
    }

    public TypeSpec build() {
      return new TypeSpec(this);
    }
  }
}
