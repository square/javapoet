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
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/** A generated parameter declaration. */
public final class ParameterSpec {
  public final String name;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final TypeName type;

  private ParameterSpec(Builder builder) {
    this.name = checkNotNull(builder.name, "name == null");
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.type = checkNotNull(builder.type, "type == null");
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, boolean varargs) throws IOException {
    codeWriter.emitAnnotations(annotations, true);
    codeWriter.emitModifiers(modifiers);
    if (varargs) {
      codeWriter.emit("$T... $L", TypeName.arrayComponent(type), name);
    } else {
      codeWriter.emit("$T $L", type, name);
    }
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, false);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder builder(TypeName type, String name, Modifier... modifiers) {
    checkNotNull(type, "type == null");
    checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
    return new Builder(type, name)
        .addModifiers(modifiers);
  }

  public static Builder builder(Type type, String name, Modifier... modifiers) {
    return builder(TypeName.get(type), name, modifiers);
  }

  public Builder toBuilder() {
    return toBuilder(type, name);
  }

  Builder toBuilder(TypeName type, String name) {
    Builder builder = new Builder(type, name);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    return builder;
  }

  public static final class Builder {
    private final TypeName type;
    private final String name;

    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();

    private Builder(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
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
