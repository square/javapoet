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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** A generated parameter declaration. */
public final class ParameterSpec {
  public final String name;
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final Type type;

  private ParameterSpec(Builder builder) {
    this.name = checkNotNull(builder.name, "name == null");
    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.type = checkNotNull(builder.type, "type == null");
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, boolean varargs) throws IOException {
    codeWriter.emitAnnotations(annotations, true);
    codeWriter.emitModifiers(modifiers);
    if (varargs) {
      codeWriter.emit("$T... $L", Types.arrayComponent(type), name);
    } else {
      codeWriter.emit("$T $L", type, name);
    }
  }

  public static Builder builder(Type type, String name, Modifier... modifiers) {
    return new Builder(type, name)
        .addModifiers(modifiers);
  }

  public static final class Builder {
    private final Type type;
    private final String name;

    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();

    private Builder(Type type, String name) {
      checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
      this.type = type;
      this.name = name;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Type annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public ParameterSpec build() {
      return new ParameterSpec(this);
    }
  }
}
