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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.javapoet.Util.checkNotNull;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;

/** A generated annotation on a declaration. */
public final class AnnotationSpec {
  public final TypeName type;
  public final Map<String, List<CodeBlock>> members;

  private AnnotationSpec(Builder builder) {
    this.type = builder.type;
    this.members = Util.immutableMultimap(builder.members);
  }

  void emit(CodeWriter codeWriter, boolean inline) throws IOException {
    String whitespace = inline ? "" : "\n";
    String memberSeparator = inline ? ", " : ",\n";
    if (members.isEmpty()) {
      // @Singleton
      codeWriter.emit("@$T", type);
    } else if (members.size() == 1 && members.containsKey("value")) {
      // @Named("foo")
      codeWriter.emit("@$T(", type);
      emitAnnotationValues(codeWriter, whitespace, memberSeparator, members.get("value"));
      codeWriter.emit(")");
    } else {
      // Inline:
      //   @Column(name = "updated_at", nullable = false)
      //
      // Not inline:
      //   @Column(
      //       name = "updated_at",
      //       nullable = false
      //   )
      codeWriter.emit("@$T(" + whitespace, type);
      codeWriter.indent(2);
      for (Iterator<Map.Entry<String, List<CodeBlock>>> i
          = members.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry<String, List<CodeBlock>> entry = i.next();
        codeWriter.emit("$L = ", entry.getKey());
        emitAnnotationValues(codeWriter, whitespace, memberSeparator, entry.getValue());
        if (i.hasNext()) codeWriter.emit(memberSeparator);
      }
      codeWriter.unindent(2);
      codeWriter.emit(whitespace + ")");
    }
  }

  private void emitAnnotationValues(CodeWriter codeWriter, String whitespace,
      String memberSeparator, List<CodeBlock> values) throws IOException {
    if (values.size() == 1) {
      codeWriter.indent(2);
      codeWriter.emit(values.get(0));
      codeWriter.unindent(2);
      return;
    }

    codeWriter.emit("{" + whitespace);
    codeWriter.indent(2);
    boolean first = true;
    for (CodeBlock codeBlock : values) {
      if (!first) codeWriter.emit(memberSeparator);
      codeWriter.emit(codeBlock);
      first = false;
    }
    codeWriter.unindent(2);
    codeWriter.emit(whitespace + "}");
  }

  public static AnnotationSpec get(AnnotationMirror annotation) {
    TypeElement element = (TypeElement) annotation.getAnnotationType().asElement();
    AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get(element));
    Visitor visitor = new Visitor(builder);
    for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
      String name = executableElement.getSimpleName().toString();
      AnnotationValue value = annotation.getElementValues().get(executableElement);
      value.accept(visitor, new Entry(name, value));
    }
    return builder.build();
  }

  public static Builder builder(ClassName type) {
    checkNotNull(type, "type == null");
    return new Builder(type);
  }

  public static Builder builder(Class<?> type) {
    return builder(ClassName.get(type));
  }

  public Builder toBuilder() {
    Builder builder = new Builder(type);
    for (Map.Entry<String, List<CodeBlock>> entry : members.entrySet()) {
      builder.members.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return builder;
  }

  @Override public boolean equals(Object o) {
    return o instanceof AnnotationSpec
        && ((AnnotationSpec) o).type.equals(type)
        && ((AnnotationSpec) o).members.equals(members);
  }

  @Override public int hashCode() {
    return type.hashCode() + 37 * members.hashCode();
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      codeWriter.emit("$L", this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static final class Builder {
    private final TypeName type;
    private final Map<String, List<CodeBlock>> members = new LinkedHashMap<>();

    private Builder(TypeName type) {
      this.type = type;
    }

    public Builder addMember(String name, String format, Object... args) {
      return addMember(name, CodeBlock.builder().add(format, args).build());
    }

    public Builder addMember(String name, CodeBlock codeBlock) {
      List<CodeBlock> values = members.get(name);
      if (values == null) {
        values = new ArrayList<>();
        members.put(name, values);
      }
      values.add(codeBlock);
      return this;
    }

    public AnnotationSpec build() {
      return new AnnotationSpec(this);
    }
  }

  private static class Entry {
    final String name;
    final AnnotationValue value;

    Entry(String name, AnnotationValue value) {
      this.name = name;
      this.value = value;
    }
  }

  /**
   * Annotation value visitor adding members to the given builder instance.
   */
  private static class Visitor extends SimpleAnnotationValueVisitor7<Builder, Entry> {
    final Builder builder;

    Visitor(Builder builder) {
      super(builder);
      this.builder = builder;
    }

    @Override protected Builder defaultAction(Object o, Entry entry) {
      return builder.addMember(entry.name, "$L", entry.value);
    }

    @Override public Builder visitAnnotation(AnnotationMirror a, Entry entry) {
      return builder.addMember(entry.name, "$L", get(a));
    }

    @Override public Builder visitEnumConstant(VariableElement c, Entry entry) {
      return builder.addMember(entry.name, "$T.$L", c.asType(), c.getSimpleName());
    }

    @Override public Builder visitType(TypeMirror t, Entry entry) {
      return builder.addMember(entry.name, "$T.class", t);
    }

    @Override public Builder visitArray(List<? extends AnnotationValue> values, Entry entry) {
      for (AnnotationValue value : values) {
        value.accept(this, new Entry(entry.name, value));
      }
      return builder;
    }
  }
}
