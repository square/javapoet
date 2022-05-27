/*
 * Copyright (C) 2014 Google, Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.junit.Assert.*;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.junit.Test;

public abstract class AbstractTypesTest {
  protected abstract Elements getElements();
  protected abstract Types getTypes();

  private TypeElement getElement(Class<?> clazz) {
    return getElements().getTypeElement(clazz.getCanonicalName());
  }

  private TypeMirror getMirror(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  @Test public void getBasicTypeMirror() {
    assertThat(TypeName.get(getMirror(Object.class)))
        .isEqualTo(ClassName.get(Object.class));
    assertThat(TypeName.get(getMirror(Charset.class)))
        .isEqualTo(ClassName.get(Charset.class));
    assertThat(TypeName.get(getMirror(AbstractTypesTest.class)))
        .isEqualTo(ClassName.get(AbstractTypesTest.class));
  }

  @Test public void getParameterizedTypeMirror() {
    DeclaredType setType =
        getTypes().getDeclaredType(getElement(Set.class), getMirror(Object.class));
    assertThat(TypeName.get(setType))
        .isEqualTo(ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.OBJECT));
  }

  @Test public void errorTypes() {
    JavaFileObject hasErrorTypes =
        JavaFileObjects.forSourceLines(
            "com.squareup.tacos.ErrorTypes",
            "package com.squareup.tacos;",
            "",
            "@SuppressWarnings(\"hook-into-compiler\")",
            "class ErrorTypes {",
            "  Tacos tacos;",
            "  Ingredients.Guacamole guacamole;",
            "}");
    Compilation compilation = javac().withProcessors(new AbstractProcessor() {
      @Override
      public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        TypeElement classFile =
            processingEnv.getElementUtils().getTypeElement("com.squareup.tacos.ErrorTypes");
        List<VariableElement> fields = fieldsIn(classFile.getEnclosedElements());
        ErrorType topLevel = (ErrorType) fields.get(0).asType();
        ErrorType member = (ErrorType) fields.get(1).asType();

        assertThat(TypeName.get(topLevel)).isEqualTo(ClassName.get("", "Tacos"));
        assertThat(TypeName.get(member)).isEqualTo(ClassName.get("Ingredients", "Guacamole"));
        return false;
      }

      @Override
      public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
      }
    }).compile(hasErrorTypes);

    assertThat(compilation).failed();
  }

  static class Parameterized<
      Simple,
      ExtendsClass extends Number,
      ExtendsInterface extends Runnable,
      ExtendsTypeVariable extends Simple,
      Intersection extends Number & Runnable,
      IntersectionOfInterfaces extends Runnable & Serializable> {}

  @Test public void getTypeVariableTypeMirror() {
    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();

    // Members of converted types use ClassName and not Class<?>.
    ClassName number = ClassName.get(Number.class);
    ClassName runnable = ClassName.get(Runnable.class);
    ClassName serializable = ClassName.get(Serializable.class);

    assertThat(TypeName.get(typeVariables.get(0).asType()))
        .isEqualTo(TypeVariableName.get("Simple"));
    assertThat(TypeName.get(typeVariables.get(1).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsClass", number));
    assertThat(TypeName.get(typeVariables.get(2).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsInterface", runnable));
    assertThat(TypeName.get(typeVariables.get(3).asType()))
        .isEqualTo(TypeVariableName.get("ExtendsTypeVariable", TypeVariableName.get("Simple")));
    assertThat(TypeName.get(typeVariables.get(4).asType()))
        .isEqualTo(TypeVariableName.get("Intersection", number, runnable));
    assertThat(TypeName.get(typeVariables.get(5).asType()))
        .isEqualTo(TypeVariableName.get("IntersectionOfInterfaces", runnable, serializable));
    assertThat(((TypeVariableName) TypeName.get(typeVariables.get(4).asType())).bounds)
        .containsExactly(number, runnable);
  }

  static class Recursive<T extends Map<List<T>, Set<T[]>>> {}

  @Test
  public void getTypeVariableTypeMirrorRecursive() {
    TypeMirror typeMirror = getElement(Recursive.class).asType();
    ParameterizedTypeName typeName = (ParameterizedTypeName) TypeName.get(typeMirror);
    String className = Recursive.class.getCanonicalName();
    assertThat(typeName.toString()).isEqualTo(className + "<T>");

    TypeVariableName typeVariableName = (TypeVariableName) typeName.typeArguments.get(0);

    try {
      typeVariableName.bounds.set(0, null);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    assertThat(typeVariableName.toString()).isEqualTo("T");
    assertThat(typeVariableName.bounds.toString())
        .isEqualTo("[java.util.Map<java.util.List<T>, java.util.Set<T[]>>]");
  }

  @Test public void getPrimitiveTypeMirror() {
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(TypeName.BOOLEAN);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(TypeName.BYTE);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(TypeName.SHORT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(TypeName.INT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(TypeName.LONG);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(TypeName.CHAR);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(TypeName.FLOAT);
    assertThat(TypeName.get(getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(TypeName.DOUBLE);
  }

  @Test public void getArrayTypeMirror() {
    assertThat(TypeName.get(getTypes().getArrayType(getMirror(Object.class))))
        .isEqualTo(ArrayTypeName.of(ClassName.OBJECT));
  }

  @Test public void getVoidTypeMirror() {
    assertThat(TypeName.get(getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(TypeName.VOID);
  }

  @Test public void getNullTypeMirror() {
    try {
      TypeName.get(getTypes().getNullType());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void parameterizedType() throws Exception {
    ParameterizedTypeName type = ParameterizedTypeName.get(Map.class, String.class, Long.class);
    assertThat(type.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Long>");
  }

  @Test public void arrayType() throws Exception {
    ArrayTypeName type = ArrayTypeName.of(String.class);
    assertThat(type.toString()).isEqualTo("java.lang.String[]");
  }

  @Test public void wildcardExtendsType() throws Exception {
    WildcardTypeName type = WildcardTypeName.subtypeOf(CharSequence.class);
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  @Test public void wildcardExtendsObject() throws Exception {
    WildcardTypeName type = WildcardTypeName.subtypeOf(Object.class);
    assertThat(type.toString()).isEqualTo("?");
  }

  @Test public void wildcardSuperType() throws Exception {
    WildcardTypeName type = WildcardTypeName.supertypeOf(String.class);
    assertThat(type.toString()).isEqualTo("? super java.lang.String");
  }

  @Test public void wildcardMirrorNoBounds() throws Exception {
    WildcardType wildcard = getTypes().getWildcardType(null, null);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("?");
  }

  @Test public void wildcardMirrorExtendsType() throws Exception {
    Types types = getTypes();
    Elements elements = getElements();
    TypeMirror charSequence = elements.getTypeElement(CharSequence.class.getName()).asType();
    WildcardType wildcard = types.getWildcardType(charSequence, null);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
  }

  @Test public void wildcardMirrorSuperType() throws Exception {
    Types types = getTypes();
    Elements elements = getElements();
    TypeMirror string = elements.getTypeElement(String.class.getName()).asType();
    WildcardType wildcard = types.getWildcardType(null, string);
    TypeName type = TypeName.get(wildcard);
    assertThat(type.toString()).isEqualTo("? super java.lang.String");
  }

  @Test public void typeVariable() throws Exception {
    TypeVariableName type = TypeVariableName.get("T", CharSequence.class);
    assertThat(type.toString()).isEqualTo("T"); // (Bounds are only emitted in declaration.)
  }

  @Test public void box() throws Exception {
    assertThat(TypeName.INT.box()).isEqualTo(ClassName.get(Integer.class));
    assertThat(TypeName.VOID.box()).isEqualTo(ClassName.get(Void.class));
    assertThat(ClassName.get(Integer.class).box()).isEqualTo(ClassName.get(Integer.class));
    assertThat(ClassName.get(Void.class).box()).isEqualTo(ClassName.get(Void.class));
    assertThat(TypeName.OBJECT.box()).isEqualTo(TypeName.OBJECT);
    assertThat(ClassName.get(String.class).box()).isEqualTo(ClassName.get(String.class));
  }

  @Test public void unbox() throws Exception {
    assertThat(TypeName.INT).isEqualTo(TypeName.INT.unbox());
    assertThat(TypeName.VOID).isEqualTo(TypeName.VOID.unbox());
    assertThat(ClassName.get(Integer.class).unbox()).isEqualTo(TypeName.INT.unbox());
    assertThat(ClassName.get(Void.class).unbox()).isEqualTo(TypeName.VOID.unbox());
    try {
      TypeName.OBJECT.unbox();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    try {
      ClassName.get(String.class).unbox();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }
}
