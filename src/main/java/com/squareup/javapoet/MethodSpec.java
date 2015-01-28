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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/** A generated constructor or method declaration. */
public final class MethodSpec {
  static final String CONSTRUCTOR = "<init>";

  public final String name;
  public final CodeBlock javadoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final List<TypeVariableName> typeVariables;
  public final TypeName returnType;
  public final List<ParameterSpec> parameters;
  public final boolean varargs;
  public final List<TypeName> exceptions;
  public final CodeBlock code;

  private MethodSpec(Builder builder) {
    CodeBlock code = builder.code.build();
    checkArgument(code.isEmpty() || !builder.modifiers.contains(Modifier.ABSTRACT),
        "abstract method %s cannot have code", builder.name);
    checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
        "last parameter of varargs method %s must be an array", builder.name);

    this.name = checkNotNull(builder.name, "name == null");
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.typeVariables = Util.immutableList(builder.typeVariables);
    this.returnType = builder.returnType;
    this.parameters = Util.immutableList(builder.parameters);
    this.varargs = builder.varargs;
    this.exceptions = Util.immutableList(builder.exceptions);
    this.code = code;
  }

  private boolean lastParameterIsArray(List<ParameterSpec> parameters) {
    return !parameters.isEmpty()
        && TypeName.arrayComponent(parameters.get(parameters.size() - 1).type) != null;
  }

  void emit(CodeWriter codeWriter, String enclosingName, Set<Modifier> implicitModifiers)
      throws IOException {
    codeWriter.emitJavadoc(javadoc);
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);

    if (!typeVariables.isEmpty()) {
      codeWriter.emitTypeVariables(typeVariables);
      codeWriter.emit(" ");
    }

    if (isConstructor()) {
      codeWriter.emit("$L(", enclosingName);
    } else {
      codeWriter.emit("$T $L(", returnType, name);
    }

    boolean firstParameter = true;
    for (Iterator<ParameterSpec> i = parameters.iterator(); i.hasNext();) {
      ParameterSpec parameter = i.next();
      if (!firstParameter) codeWriter.emit(", ");
      parameter.emit(codeWriter, !i.hasNext() && varargs);
      firstParameter = false;
    }

    codeWriter.emit(")");
    if (!exceptions.isEmpty()) {
      codeWriter.emit(" throws");
      boolean firstException = true;
      for (TypeName exception : exceptions) {
        if (!firstException) codeWriter.emit(",");
        codeWriter.emit(" $T", exception);
        firstException = false;
      }
    }

    if (hasModifier(Modifier.ABSTRACT)) {
      codeWriter.emit(";\n");
    } else if (hasModifier(Modifier.NATIVE)) {
      // Code is allowed to support stuff like GWT JSNI.
      codeWriter.emit(code);
      codeWriter.emit(";\n");
    } else {
      codeWriter.emit(" {\n");

      codeWriter.indent();
      codeWriter.emit(code);
      codeWriter.unindent();

      codeWriter.emit("}\n");
    }
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  public boolean isConstructor() {
    return name.equals(CONSTRUCTOR);
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, "Constructor", Collections.<Modifier>emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder methodBuilder(String name) {
    return new Builder(name);
  }

  public static Builder constructorBuilder() {
    return new Builder(CONSTRUCTOR);
  }

  public static final class Builder {
    private final String name;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private List<TypeVariableName> typeVariables = new ArrayList<>();
    private TypeName returnType;
    private final List<ParameterSpec> parameters = new ArrayList<>();
    private final List<TypeName> exceptions = new ArrayList<>();
    private final CodeBlock.Builder code = CodeBlock.builder();
    private boolean varargs;

    private Builder(String name) {
      checkArgument(name.equals(CONSTRUCTOR) || SourceVersion.isName(name),
          "not a valid name: %s", name);
      this.name = name;
      this.returnType = name.equals(CONSTRUCTOR) ? null : TypeName.VOID;
    }

    public Builder addJavadoc(String format, Object... args) {
      javadoc.add(format, args);
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

    public Builder addTypeVariable(TypeVariableName typeVariable) {
      typeVariables.add(typeVariable);
      return this;
    }

    public Builder returns(TypeName returnType) {
      checkState(!name.equals(CONSTRUCTOR), "constructor cannot have return type.");
      this.returnType = returnType;
      return this;
    }

    public Builder returns(Type returnType) {
      return returns(TypeName.get(returnType));
    }

    public Builder addParameter(ParameterSpec parameterSpec) {
      this.parameters.add(parameterSpec);
      return this;
    }

    public Builder addParameter(TypeName type, String name, Modifier... modifiers) {
      return addParameter(ParameterSpec.builder(type, name, modifiers).build());
    }

    public Builder addParameter(Type type, String name, Modifier... modifiers) {
      return addParameter(TypeName.get(type), name, modifiers);
    }

    public Builder varargs() {
      this.varargs = true;
      return this;
    }

    public Builder addException(TypeName exception) {
      this.exceptions.add(exception);
      return this;
    }

    public Builder addException(Type exception) {
      return addException(TypeName.get(exception));
    }

    public Builder addCode(String format, Object... args) {
      code.add(format, args);
      return this;
    }

    public Builder addCode(CodeBlock codeBlock) {
      code.add(codeBlock);
      return this;
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * Shouldn't contain braces or newline characters.
     */
    public Builder beginControlFlow(String controlFlow, Object... args) {
      code.beginControlFlow(controlFlow, args);
      return this;
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    public Builder nextControlFlow(String controlFlow, Object... args) {
      code.nextControlFlow(controlFlow, args);
      return this;
    }

    public Builder endControlFlow() {
      code.endControlFlow();
      return this;
    }

    /**
     * @param controlFlow the optional control flow construct and its code, such as
     *     "while(foo == 20)". Only used for "do/while" control flows.
     */
    public Builder endControlFlow(String controlFlow, Object... args) {
      code.endControlFlow(controlFlow, args);
      return this;
    }

    public Builder addStatement(String format, Object... args) {
      code.addStatement(format, args);
      return this;
    }

    public MethodSpec build() {
      return new MethodSpec(this);
    }
  }
}
