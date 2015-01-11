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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/** A generated field declaration. */
public final class FieldSpec {
  public final Name name;
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final Type type;
  public final Snippet initializer;

  private FieldSpec(Builder builder) {
    this.name = checkNotNull(builder.name);
    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.type = checkNotNull(builder.type);
    this.initializer = builder.initializer;
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, ImmutableSet<Modifier> implicitModifiers) {
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);
    codeWriter.emit("$T $L", type, name);
    if (initializer != null) {
      codeWriter.emit(" = ");
      codeWriter.emit(initializer);
    }
    codeWriter.emit(";\n");
  }

  public static Builder builder(String name) {
    return builder(new Name(name));
  }

  public static Builder builder(Name name) {
    return new Builder(name);
  }

  public static FieldSpec of(Type type, String name, Modifier... modifiers) {
    return new Builder(new Name(name))
        .type(type)
        .addModifiers(modifiers)
        .build();
  }

  public static final class Builder {
    private final Name name;

    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private Type type;
    private Snippet initializer;

    private Builder(Name name) {
      this.name = name;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Type annotation) {
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

    public Builder initializer(String format, Object... args) {
      this.initializer = new Snippet(format, args);
      return this;
    }

    public FieldSpec build() {
      return new FieldSpec(this);
    }
  }
}
