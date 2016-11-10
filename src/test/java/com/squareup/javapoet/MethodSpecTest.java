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

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MethodSpecTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private Elements elements;
  private Types types;

  @Before public void setUp() {
    elements = compilation.getElements();
    types = compilation.getTypes();
  }

  private TypeElement getElement(Class<?> clazz) {
    return elements.getTypeElement(clazz.getCanonicalName());
  }

  private ExecutableElement findFirst(Collection<ExecutableElement> elements, String name) {
    for (ExecutableElement executableElement : elements) {
      if (executableElement.getSimpleName().toString().equals(name)) {
        return executableElement;
      }
    }
    throw new IllegalArgumentException(name + " not found in " + elements);
  }

  @Test public void nullAnnotationsAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addAnnotations(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("annotationSpecs == null");
    }
  }

  @Test public void nullTypeVariablesAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addTypeVariables(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("typeVariables == null");
    }
  }

  @Test public void nullParametersAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addParameters(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("parameterSpecs == null");
    }
  }

  @Test public void nullExceptionsAddition() {
    try {
      MethodSpec.methodBuilder("doSomething").addExceptions(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("exceptions == null");
    }
  }

  @Target(ElementType.PARAMETER)
  @interface Nullable {
  }

  abstract static class Everything {
    @Deprecated protected abstract <T extends Runnable & Closeable> Runnable everything(
        @Nullable String thing, List<? extends T> things) throws IOException, SecurityException;
  }

  abstract static class HasAnnotation {
    @Override public abstract String toString();
  }

  interface ExtendsOthers extends Callable<Integer>, Comparable<Long> {
  }
  
  interface ExtendsIterableWithDefaultMethods extends Iterable<Object> {
  }

  @Test public void overrideEverything() {
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    MethodSpec method = MethodSpec.overriding(methodElement).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "protected <T extends java.lang.Runnable & java.io.Closeable> java.lang.Runnable "
        + "everything(java.lang.String arg0,\n"
        + "    java.util.List<? extends T> arg1) throws java.io.IOException, "
        + "java.lang.SecurityException {\n"
        + "}\n");
  }

  @Test public void overrideDoesNotCopyOverrideAnnotation() {
    TypeElement classElement = getElement(HasAnnotation.class);
    ExecutableElement exec = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    MethodSpec method = MethodSpec.overriding(exec).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {\n"
        + "}\n");
  }

  @Test public void overrideDoesNotCopyDefaultModifier() {
    TypeElement classElement = getElement(ExtendsIterableWithDefaultMethods.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    ExecutableElement exec = findFirst(methods, "iterator");
    assume().that(Util.DEFAULT).isNotNull();
    exec = findFirst(methods, "spliterator");
    MethodSpec method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.util.Spliterator<java.lang.Object> spliterator() {\n"
        + "}\n");
  }

  @Test public void overrideExtendsOthersWorksWithActualTypeParameters() {
    TypeElement classElement = getElement(ExtendsOthers.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));
    ExecutableElement exec = findFirst(methods, "call");
    MethodSpec method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.Integer call() throws java.lang.Exception {\n"
        + "}\n");
    exec = findFirst(methods, "compareTo");
    method = MethodSpec.overriding(exec, classType, types).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public int compareTo(java.lang.Long arg0) {\n"
        + "}\n");
  }

  @Test public void overrideInvalidModifiers() {
    ExecutableElement method = mock(ExecutableElement.class);
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.FINAL));
    Element element = mock(Element.class);
    when(element.asType()).thenReturn(mock(DeclaredType.class));
    when(method.getEnclosingElement()).thenReturn(element);
    try {
      MethodSpec.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [final]");
    }
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.PRIVATE));
    try {
      MethodSpec.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [private]");
    }
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.STATIC));
    try {
      MethodSpec.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [static]");
    }
  }

  @Test public void equalsAndHashCode() {
    MethodSpec a = MethodSpec.constructorBuilder().build();
    MethodSpec b = MethodSpec.constructorBuilder().build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = MethodSpec.methodBuilder("taco").build();
    b = MethodSpec.methodBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));
    a = MethodSpec.overriding(methodElement).build();
    b = MethodSpec.overriding(methodElement).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test public void duplicateExceptionsIgnored() {
    ClassName ioException = ClassName.get(IOException.class);
    ClassName timeoutException = ClassName.get(TimeoutException.class);
    MethodSpec methodSpec = MethodSpec.methodBuilder("duplicateExceptions")
      .addException(ioException)
      .addException(timeoutException)
      .addException(timeoutException)
      .addException(ioException)
      .build();
    assertThat(methodSpec.exceptions).isEqualTo(Arrays.asList(ioException, timeoutException));
    assertThat(methodSpec.toBuilder().addException(ioException).build().exceptions)
      .isEqualTo(Arrays.asList(ioException, timeoutException));
  }

}
