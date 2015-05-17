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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;

public final class JavaxLangModelPoetTest {

  @Rule
  public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private ExecutableElement findFirst(Collection<ExecutableElement> elements, String name) {
    for (ExecutableElement executableElement : elements) {
      if (executableElement.getSimpleName().toString().equals(name))
        return executableElement;
    }
    throw new IllegalArgumentException(name + " not found in " + elements);
  }

  @Target(ElementType.PARAMETER)
  @interface Nullable {
  }

  abstract static class Everything {
    @Deprecated
    protected abstract <T extends Runnable & Closeable> Runnable everything(@Nullable String thing,
        List<? extends T> things) throws IOException, SecurityException;
  }

  abstract static class HasAnnotation {
    @Override
    public abstract String toString();
  }

  interface ExtendsOthers extends Callable<Integer>, Comparable<Long> {
    // empty on purpose
  }

  @Test
  public void buildEverything() {
    JavaxLangModelPoet poet = new JavaxLangModelPoet(compilation.getElements(), compilation.getTypes());
    TypeElement classElement = getElement(Everything.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = poet.builder(methodElement, classType).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Deprecated\n"
        + "protected abstract <T extends java.lang.Runnable & java.io.Closeable> "
        + "java.lang.Runnable everything("
        + "@com.squareup.javapoet.JavaxLangModelPoetTest.Nullable java.lang.String arg0, "
        + "java.util.List<? extends T> arg1) "
        + "throws java.io.IOException, java.lang.SecurityException;\n");
  }

  @Test
  public void buildHasAnnotation() {
    JavaxLangModelPoet poet = new JavaxLangModelPoet(compilation.getElements(), compilation.getTypes());
    TypeElement classElement = getElement(HasAnnotation.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = poet.builder(methodElement, classType).build();
    assertThat(method.toString()).isEqualTo("public abstract java.lang.String toString();\n");
  }

  @Test
  public void buildExtendsOthersWorksWithActualTypeParameters() {
    Elements elements = compilation.getElements();
    JavaxLangModelPoet poet = new JavaxLangModelPoet(elements, compilation.getTypes());
    TypeElement classElement = getElement(ExtendsOthers.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));

    MethodSpec method = poet.builder(findFirst(methods, "call"), classType).build();
    assertThat(method.toString()).isEqualTo(
        "public abstract java.lang.Integer call() throws java.lang.Exception;\n");

    method = poet.builder(findFirst(methods, "compareTo"), classType).build();
    assertThat(method.toString())
        .isEqualTo("public abstract int compareTo(java.lang.Long arg0);\n");
  }

  @Test
  public void overrideEverything() {
    JavaxLangModelPoet poet = new JavaxLangModelPoet(compilation.getElements(), compilation.getTypes());
    TypeElement classElement = getElement(Everything.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = poet.overriding(methodElement, classType).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "@java.lang.Deprecated\n"
        + "protected <T extends java.lang.Runnable & java.io.Closeable> "
        + "java.lang.Runnable everything("
        + "@com.squareup.javapoet.JavaxLangModelPoetTest.Nullable java.lang.String arg0, "
        + "java.util.List<? extends T> arg1) "
        + "throws java.io.IOException, java.lang.SecurityException {\n"
        + "}\n");
  }

  @Test
  public void overrideDoesNotCopyOverrideAnnotation() {
    JavaxLangModelPoet poet = new JavaxLangModelPoet(compilation.getElements(), compilation.getTypes());
    TypeElement classElement = getElement(HasAnnotation.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = poet.overriding(methodElement, classType).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {\n"
        + "}\n");
  }

  @Test
  public void overrideExtendsOthersWorksWithActualTypeParameters() {
    Elements elements = compilation.getElements();
    JavaxLangModelPoet poet = new JavaxLangModelPoet(elements, compilation.getTypes());
    TypeElement classElement = getElement(ExtendsOthers.class);
    DeclaredType classType = (DeclaredType) classElement.asType();
    List<ExecutableElement> methods = methodsIn(elements.getAllMembers(classElement));

    MethodSpec method = poet.overriding(findFirst(methods, "call"), classType).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.Integer call() throws java.lang.Exception {\n"
        + "}\n");

    method = poet.overriding(findFirst(methods, "compareTo"), classType).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public int compareTo(java.lang.Long arg0) {\n"
        + "}\n");
  }

  @Test
  public void overrideInvalidModifiers() {
    JavaxLangModelPoet poet = new JavaxLangModelPoet(compilation.getElements(), compilation.getTypes());
    ExecutableElement method = mock(ExecutableElement.class);
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.FINAL));
    Element element = mock(Element.class);
    when(element.asType()).thenReturn(mock(DeclaredType.class));
    when(method.getEnclosingElement()).thenReturn(element);
    try {
      poet.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [final]");
    }
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.PRIVATE));
    try {
      poet.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [private]");
    }
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.STATIC));
    try {
      poet.overriding(method);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [static]");
    }
  }

}
