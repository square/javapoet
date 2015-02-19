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
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MethodSpecTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
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

  @SuppressWarnings("unused") // Used via mirror API.
  abstract static class Everything {
    @Deprecated protected abstract <T extends Runnable & Closeable> Runnable everything(
        @Nullable String thing, List<? extends T> things) throws IOException, SecurityException;
  }

  @SuppressWarnings("unused") // Used via mirror API.
  abstract static class HasAnnotation {
    @Override public abstract String toString();
  }

  @Test public void overrideEverything() {
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = MethodSpec.overriding(methodElement).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "protected <T extends java.lang.Runnable & java.io.Closeable> "
        + "java.lang.Runnable everything(java.lang.String arg0, java.util.List<? extends T> arg1) "
        + "throws java.io.IOException, java.lang.SecurityException {\n"
        + "}\n");
    // TODO see TODOs in MethodSpec.override
    //assertThat(method.toString()).isEqualTo(""
    //    + "@java.lang.Override\n"
    //    + "@java.lang.Deprecated\n"
    //    + "protected <T extends java.lang.Runnable & java.io.Closeable> "
    //    + "java.lang.Runnable everything("
    //    + "@com.squareup.javapoet.MethodSpecTest.Nullable java.lang.String arg0, "
    //    + "java.util.List<? extends T> arg1) "
    //    + "throws java.io.IOException, java.lang.SecurityException {\n"
    //    + "}\n");
  }

  @Ignore // TODO see TODOs in MethodSpec.override
  @Test public void overrideDoesNotCopyOverrideAnnotation() {
    TypeElement classElement = getElement(Everything.class);
    ExecutableElement methodElement = getOnlyElement(methodsIn(classElement.getEnclosedElements()));

    MethodSpec method = MethodSpec.overriding(methodElement).build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {"
        + "}");
  }

  @Test public void overrideInvalidModifiers() {
    ExecutableElement method = mock(ExecutableElement.class);
    when(method.getModifiers()).thenReturn(ImmutableSet.of(Modifier.FINAL));
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
}
