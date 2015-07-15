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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/** A generated constructor or method declaration. */
public final class MethodSpec {
  static final String CONSTRUCTOR = "<init>";
  static final ClassName OVERRIDE = ClassName.get(Override.class);

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
  public final CodeBlock defaultValue;

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
    this.defaultValue = builder.defaultValue;
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

    if (defaultValue != null && !defaultValue.isEmpty()) {
      codeWriter.emit(" default ");
      codeWriter.emit(defaultValue);
    }

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

  /**
   * Returns a new method spec builder that overrides {@code method}.
   *
   * <p>This will copy its visibility modifiers, type parameters, return type, name, parameters, and
   * throws declarations. An {@link Override} annotation will be added.
   */
  public static Builder overriding(ExecutableElement method) {
    checkNotNull(method, "method == null");

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)
        || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    methodBuilder.addAnnotation(OVERRIDE);
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
      if (annotationSpec.type.equals(OVERRIDE)) continue;
      methodBuilder.addAnnotation(annotationSpec);
    }

    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    methodBuilder.returns(TypeName.get(method.getReturnType()));

    List<? extends VariableElement> parameters = method.getParameters();
    for (VariableElement parameter : parameters) {
      TypeName type = TypeName.get(parameter.asType());
      String name = parameter.getSimpleName().toString();
      Set<Modifier> parameterModifiers = parameter.getModifiers();
      ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
          .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
      for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
        parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
      }
      methodBuilder.addParameter(parameterBuilder.build());
    }
    methodBuilder.varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    return methodBuilder;
  }

  /**
   * Returns a new method spec builder that overrides {@code method} as a member of {@code
   * enclosing}. This will resolve type parameters: for example overriding {@link
   * Comparable#compareTo} in a type that implements {@code Comparable<Movie>}, the {@code T}
   * parameter will be resolved to {@code Movie}.
   *
   * <p>This will copy its visibility modifiers, type parameters, return type, name, parameters, and
   * throws declarations. An {@link Override} annotation will be added.
   */
  public static Builder overriding(
      ExecutableElement method, DeclaredType enclosing, Types types) {
    ExecutableType executableType = (ExecutableType) types.asMemberOf(enclosing, method);
    List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
    TypeMirror resolvedReturnType = executableType.getReturnType();

    Builder builder = overriding(method);
    builder.returns(TypeName.get(resolvedReturnType));
    for (int i = 0, size = builder.parameters.size(); i < size; i++) {
      ParameterSpec parameter = builder.parameters.get(i);
      TypeName type = TypeName.get(resolvedParameterTypes.get(i));
      builder.parameters.set(i, parameter.toBuilder(type, parameter.name).build());
    }

    return builder;
  }

  public Builder toBuilder() {
    Builder builder = new Builder(name);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.typeVariables.addAll(typeVariables);
    builder.returnType = returnType;
    builder.parameters.addAll(parameters);
    builder.exceptions.addAll(exceptions);
    builder.code.add(code);
    builder.varargs = varargs;
    builder.defaultValue = defaultValue;
    return builder;
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
    private CodeBlock defaultValue;

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
        this.modifiers.add(modifier);
      }
      return this;
    }

    public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
      checkArgument(typeVariables != null, "typeVariables == null");
      for (TypeVariableName typeVariable : typeVariables) {
        this.typeVariables.add(typeVariable);
      }
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

    public Builder addParameters(Iterable<ParameterSpec> parameterSpecs) {
      checkArgument(parameterSpecs != null, "parameterSpecs == null");
      for (ParameterSpec parameterSpec : parameterSpecs) {
        this.parameters.add(parameterSpec);
      }
      return this;
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
      return varargs(true);
    }

    public Builder varargs(boolean varargs) {
      this.varargs = varargs;
      return this;
    }

    public Builder addExceptions(Iterable<? extends TypeName> exceptions) {
      checkArgument(exceptions != null, "exceptions == null");
      for (TypeName exception : exceptions) {
        this.exceptions.add(exception);
      }
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

    public Builder defaultValue(String format, Object... args) {
      return defaultValue(CodeBlock.builder().add(format, args).build());
    }

    public Builder defaultValue(CodeBlock codeBlock) {
      checkState(this.defaultValue == null, "defaultValue was already set");
      this.defaultValue = checkNotNull(codeBlock, "codeBlock == null");
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
