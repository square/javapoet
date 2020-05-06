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

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/** A generated parameter declaration. */
public final class ParameterSpec {
  public final String name;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final TypeName type;
  public final CodeBlock javadoc;
  public final boolean isLambda;

  private ParameterSpec(Builder builder) {
    this.name = checkNotNull(builder.name, "name == null");
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.isLambda = builder.isLambda;
    if (!isLambda)
      this.type = checkNotNull(builder.type, "type == null");
    else
      this.type = null;
    this.javadoc = builder.javadoc.build();

  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, boolean varargs) throws IOException {
    if (type != null) {
      codeWriter.emitAnnotations(annotations, true);
      codeWriter.emitModifiers(modifiers);
    }
    if (varargs) {
      TypeName.asArray(type).emit(codeWriter, true);
    } else {
      if (type != null)
        type.emit(codeWriter);
    }
    if (type != null)
     codeWriter.emit(" $L", name);
    else
      codeWriter.emit("$L", name);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, false);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static ParameterSpec get(VariableElement element) {
    checkArgument(element.getKind().equals(ElementKind.PARAMETER), "element is not a parameter");

    // Copy over any annotations from element.
    List<AnnotationSpec> annotations = element.getAnnotationMirrors()
        .stream()
        .map((mirror) -> AnnotationSpec.get(mirror))
        .collect(Collectors.toList());

    TypeName type = TypeName.get(element.asType());
    String name = element.getSimpleName().toString();
    return ParameterSpec.builder(type, name)
        .addModifiers(element.getModifiers())
        .addAnnotations(annotations)
        .build();
  }

  static List<ParameterSpec> parametersOf(ExecutableElement method) {
    List<ParameterSpec> result = new ArrayList<>();
    for (VariableElement parameter : method.getParameters()) {
      result.add(ParameterSpec.get(parameter));
    }
    return result;
  }
  public static Builder builder(TypeName type, String name, boolean lambda, Modifier... modifiers) {
    checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
    return new Builder(type, name, lambda);
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
    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private boolean isLambda = false;

    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();

    private Builder(TypeName type, String name, boolean isLambda) {
      this.type = type;
      this.name = name;
      this.isLambda = isLambda;
    }
    private Builder(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
      return this;
    }

    public Builder addJavadoc(CodeBlock block) {
      javadoc.add(block);
      return this;
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

    public Builder addModifiers(Iterable<Modifier> modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      for (Modifier modifier : modifiers) {
        if (!modifier.equals(Modifier.FINAL)) {
          throw new IllegalStateException("unexpected parameter modifier: " + modifier);
        }
        this.modifiers.add(modifier);
      }
      return this;
    }

    public ParameterSpec build() {
      return new ParameterSpec(this);
    }
  }
}
