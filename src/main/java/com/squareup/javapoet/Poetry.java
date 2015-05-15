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

import static com.squareup.javapoet.Util.checkNotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Provides utilities based on parsing elements and types from javax.lang.model instances.
 *
 * @author Christian Stein
 */
public class Poetry {

  protected final Elements elements;
  protected final Types types;

  public Poetry(Elements elements, Types types) {
    checkNotNull(elements, "elements==null");
    checkNotNull(types, "types==null");
    this.elements = elements;
    this.types = types;
  }

  /**
   * Create a method spec builder which overrides {@code method}.
   *
   * Same as {@code overriding(method, (DeclaredType) method.getEnclosingElement().asType())}.
   */
  public MethodSpec.Builder overriding(ExecutableElement method) {
    return overriding(method, (DeclaredType) method.getEnclosingElement().asType());
  }

  /**
   * Create a method spec builder which overrides {@code method} that is viewed as being a member of
   * the specified {@code containing} class or interface.
   *
   * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
   * throws declarations. An {@link Override} annotation will be added.
   */
  public MethodSpec.Builder overriding(ExecutableElement method, DeclaredType containing) {
    checkNotNull(method, "method == null");
    checkNotNull(containing, "containing == null");

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)
        || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    methodBuilder.addAnnotation(Override.class);
    TypeMirror overrideType = elements.getTypeElement(Override.class.getCanonicalName()).asType();
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      if (types.isSameType(mirror.getAnnotationType(), overrideType)) {
        continue;
      }
      methodBuilder.addAnnotation(AnnotationSpec.get(mirror));
    }

    modifiers = new LinkedHashSet<>(modifiers); // Local copy so we can remove.
    modifiers.remove(Modifier.ABSTRACT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    ExecutableType executableType = (ExecutableType) types.asMemberOf(containing, method);
    methodBuilder.returns(TypeName.get(executableType.getReturnType()));

    List<? extends VariableElement> parameters = method.getParameters();
    List<? extends TypeMirror> parameterTypes = executableType.getParameterTypes();
    for (int index = 0; index < parameters.size(); index++) {
      VariableElement parameter = parameters.get(index);
      TypeName type = TypeName.get(parameterTypes.get(index));
      String name = parameter.getSimpleName().toString();
      Modifier[] paramods = new Modifier[parameter.getModifiers().size()];
      parameter.getModifiers().toArray(paramods);
      ParameterSpec.Builder psb = ParameterSpec.builder(type, name, paramods);
      for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
        psb.addAnnotation(AnnotationSpec.get(mirror));
      }
      methodBuilder.addParameter(psb.build());
    }

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    if (method.isVarArgs()) {
      methodBuilder.varargs();
    }

    return methodBuilder;
  }

}
