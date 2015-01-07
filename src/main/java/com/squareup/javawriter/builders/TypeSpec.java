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
import com.squareup.javawriter.ClassName;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final Type type;
  public final ClassName name;
  public final ImmutableList<MethodSpec> methodSpecs;

  private TypeSpec(Builder builder) {
    this.type = checkNotNull(builder.type);
    this.name = checkNotNull(builder.name);
    this.methodSpecs = ImmutableList.copyOf(builder.methodSpecs);
  }

  void emit(CodeWriter codeWriter) {
    codeWriter.emit("class $L {\n", name.simpleName()); // TODO(jwilson): modifiers.
    codeWriter.indent();

    boolean firstMethod = true;
    for (MethodSpec methodSpec : methodSpecs) {
      if (!firstMethod) codeWriter.emit("\n");
      methodSpec.emit(codeWriter);
      firstMethod = false;
    }

    codeWriter.unindent();
    codeWriter.emit("}\n");
  }

  public static enum Type {
    CLASS, INTERFACE, ENUM
  }

  public static final class Builder {
    private Type type = Type.CLASS;
    private ClassName name;
    private List<MethodSpec> methodSpecs = new ArrayList<>();

    public Builder type(Type type) {
      this.type = type;
      return this;
    }

    public Builder name(ClassName name) {
      this.name = name;
      return this;
    }

    public Builder addMethod(MethodSpec methodSpec) {
      methodSpecs.add(methodSpec);
      return this;
    }

    public TypeSpec build() {
      return new TypeSpec(this);
    }
  }
}
