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

import com.google.common.collect.ImmutableMap;
import com.google.testing.compile.CompilationRule;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(JUnit4.class)
public final class TypeSpecTest {
  private final String tacosPackage = "com.squareup.tacos";
  private static final String donutsPackage = "com.squareup.donuts";

  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private boolean isJava8() {
    return Util.DEFAULT != null;
  }

  @Test public void basic() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(String.class)
            .addCode("return $S;\n", "taco")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @Override\n"
        + "  public final String toString() {\n"
        + "    return \"taco\";\n"
        + "  }\n"
        + "}\n");
    Assert.assertEquals(472949424, taco.hashCode()); // update expected number if source changes
  }

  @Test public void interestingTypes() throws Exception {
    TypeName listOfAny = ParameterizedTypeName.get(
        ClassName.get(List.class), WildcardTypeName.subtypeOf(Object.class));
    TypeName listOfExtends = ParameterizedTypeName.get(
        ClassName.get(List.class), WildcardTypeName.subtypeOf(Serializable.class));
    TypeName listOfSuper = ParameterizedTypeName.get(ClassName.get(List.class),
        WildcardTypeName.supertypeOf(String.class));
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(listOfAny, "extendsObject")
        .addField(listOfExtends, "extendsSerializable")
        .addField(listOfSuper, "superString")
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Taco {\n"
        + "  List<?> extendsObject;\n"
        + "\n"
        + "  List<? extends Serializable> extendsSerializable;\n"
        + "\n"
        + "  List<? super String> superString;\n"
        + "}\n");
  }

  @Test public void anonymousInnerClass() throws Exception {
    ClassName foo = ClassName.get(tacosPackage, "Foo");
    ClassName bar = ClassName.get(tacosPackage, "Bar");
    ClassName thingThang = ClassName.get(tacosPackage, "Thing", "Thang");
    TypeName thingThangOfFooBar = ParameterizedTypeName.get(thingThang, foo, bar);
    ClassName thung = ClassName.get(tacosPackage, "Thung");
    ClassName simpleThung = ClassName.get(tacosPackage, "SimpleThung");
    TypeName thungOfSuperBar = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(bar));
    TypeName thungOfSuperFoo = ParameterizedTypeName.get(thung, WildcardTypeName.supertypeOf(foo));
    TypeName simpleThungOfBar = ParameterizedTypeName.get(simpleThung, bar);

    ParameterSpec thungParameter = ParameterSpec.builder(thungOfSuperFoo, "thung")
        .addModifiers(Modifier.FINAL)
        .build();
    TypeSpec aSimpleThung = TypeSpec.anonymousClassBuilder("$N", thungParameter)
        .superclass(simpleThungOfBar)
        .addMethod(MethodSpec.methodBuilder("doSomething")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(bar, "bar")
            .addCode("/* code snippets */\n")
            .build())
        .build();
    TypeSpec aThingThang = TypeSpec.anonymousClassBuilder("")
        .superclass(thingThangOfFooBar)
        .addMethod(MethodSpec.methodBuilder("call")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(thungOfSuperBar)
            .addParameter(thungParameter)
            .addCode("return $L;\n", aSimpleThung)
            .build())
        .build();
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(thingThangOfFooBar, "NAME")
            .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.FINAL)
            .initializer("$L", aThingThang)
            .build())
        .build();

    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "class Taco {\n"
        + "  static final Thing.Thang<Foo, Bar> NAME = new Thing.Thang<Foo, Bar>() {\n"
        + "    @Override\n"
        + "    public Thung<? super Bar> call(final Thung<? super Foo> thung) {\n"
        + "      return new SimpleThung<Bar>(thung) {\n"
        + "        @Override\n"
        + "        public void doSomething(Bar bar) {\n"
        + "          /* code snippets */\n"
        + "        }\n"
        + "      };\n"
        + "    }\n"
        + "  };\n"
        + "}\n");
  }

  @Test public void annotatedParameters() throws Exception {
    TypeSpec service = TypeSpec.classBuilder("Foo")
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(long.class, "id")
            .addParameter(ParameterSpec.builder(String.class, "one")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "two")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "three")
                .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "Pong"))
                    .addMember("value", "$S", "pong")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder(String.class, "four")
                .addAnnotation(ClassName.get(tacosPackage, "Ping"))
                .build())
            .addCode("/* code snippets */\n")
            .build())
        .build();

    assertThat(toString(service)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Foo {\n"
        + "  public Foo(long id, @Ping String one, @Ping String two, @Pong(\"pong\") String three, "
        + "@Ping String four) {\n"
        + "    /* code snippets */\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void retrofitStyleInterface() throws Exception {
    ClassName observable = ClassName.get(tacosPackage, "Observable");
    ClassName fooBar = ClassName.get(tacosPackage, "FooBar");
    ClassName thing = ClassName.get(tacosPackage, "Thing");
    ClassName things = ClassName.get(tacosPackage, "Things");
    ClassName map = ClassName.get("java.util", "Map");
    ClassName string = ClassName.get("java.lang", "String");
    ClassName headers = ClassName.get(tacosPackage, "Headers");
    ClassName post = ClassName.get(tacosPackage, "POST");
    ClassName body = ClassName.get(tacosPackage, "Body");
    ClassName queryMap = ClassName.get(tacosPackage, "QueryMap");
    ClassName header = ClassName.get(tacosPackage, "Header");
    TypeSpec service = TypeSpec.interfaceBuilder("Service")
        .addMethod(MethodSpec.methodBuilder("fooBar")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(headers)
                .addMember("value", "$S", "Accept: application/json")
                .addMember("value", "$S", "User-Agent: foobar")
                .build())
            .addAnnotation(AnnotationSpec.builder(post)
                .addMember("value", "$S", "/foo/bar")
                .build())
            .returns(ParameterizedTypeName.get(observable, fooBar))
            .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(things, thing), "things")
                .addAnnotation(body)
                .build())
            .addParameter(ParameterSpec.builder(
                ParameterizedTypeName.get(map, string, string), "query")
                .addAnnotation(AnnotationSpec.builder(queryMap)
                    .addMember("encodeValues", "false")
                    .build())
                .build())
            .addParameter(ParameterSpec.builder(string, "authorization")
                .addAnnotation(AnnotationSpec.builder(header)
                    .addMember("value", "$S", "Authorization")
                    .build())
                .build())
            .build())
        .build();

    assertThat(toString(service)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "interface Service {\n"
        + "  @Headers({\n"
        + "      \"Accept: application/json\",\n"
        + "      \"User-Agent: foobar\"\n"
        + "  })\n"
        + "  @POST(\"/foo/bar\")\n"
        + "  Observable<FooBar> fooBar(@Body Things<Thing> things, @QueryMap(encodeValues = false) "
        + "Map<String, String> query, @Header(\"Authorization\") String authorization);\n"
        + "}\n");
  }

  @Test public void annotatedField() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "thing", Modifier.PRIVATE, Modifier.FINAL)
            .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "JsonAdapter"))
                .addMember("value", "$T.class", ClassName.get(tacosPackage, "Foo"))
                .build())
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @JsonAdapter(Foo.class)\n"
        + "  private final String thing;\n"
        + "}\n");
  }

  @Test public void annotatedClass() throws Exception {
    ClassName someType = ClassName.get(tacosPackage, "SomeType");
    TypeSpec taco = TypeSpec.classBuilder("Foo")
        .addAnnotation(AnnotationSpec.builder(ClassName.get(tacosPackage, "Something"))
            .addMember("hi", "$T.$N", someType, "FIELD")
            .addMember("hey", "$L", 12)
            .addMember("hello", "$S", "goodbye")
            .build())
        .addModifiers(Modifier.PUBLIC)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@Something(\n"
        + "    hi = SomeType.FIELD,\n"
        + "    hey = 12,\n"
        + "    hello = \"goodbye\"\n"
        + ")\n"
        + "public class Foo {\n"
        + "}\n");
  }

  @Test public void enumWithSubclassing() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addJavadoc("Avalanche!\n")
            .build())
        .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder("$S", "flat")
            .addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addCode("return $S;\n", "paper airplane!")
                .build())
            .build())
        .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder("$S", "peace sign")
            .build())
        .addField(String.class, "handPosition", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(String.class, "handPosition")
            .addCode("this.handPosition = handPosition;\n")
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addCode("this($S);\n", "fist")
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "public enum Roshambo {\n"
        + "  /**\n"
        + "   * Avalanche!\n"
        + "   */\n"
        + "  ROCK,\n"
        + "\n"
        + "  PAPER(\"flat\") {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"paper airplane!\";\n"
        + "    }\n"
        + "  },\n"
        + "\n"
        + "  SCISSORS(\"peace sign\");\n"
        + "\n"
        + "  private final String handPosition;\n"
        + "\n"
        + "  Roshambo(String handPosition) {\n"
        + "    this.handPosition = handPosition;\n"
        + "  }\n"
        + "\n"
        + "  Roshambo() {\n"
        + "    this(\"fist\");\n"
        + "  }\n"
        + "}\n");
  }

  /** https://github.com/square/javapoet/issues/193 */
  @Test public void enumsMayDefineAbstractMethods() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Tortilla")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("CORN", TypeSpec.anonymousClassBuilder("")
            .addMethod(MethodSpec.methodBuilder("fold")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .build())
            .build())
        .addMethod(MethodSpec.methodBuilder("fold")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "public enum Tortilla {\n"
        + "  CORN {\n"
        + "    @Override\n"
        + "    public void fold() {\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  public abstract void fold();\n"
        + "}\n");
  }

  @Test public void enumConstantsRequired() throws Exception {
    try {
      TypeSpec.enumBuilder("Roshambo")
        .build();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void onlyEnumsMayHaveEnumConstants() throws Exception {
    try {
      TypeSpec.classBuilder("Roshambo")
        .addEnumConstant("ROCK")
        .build();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void enumWithMembersButNoConstructorCall() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addEnumConstant("SPOCK", TypeSpec.anonymousClassBuilder("")
            .addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addCode("return $S;\n", "west side")
                .build())
            .build())
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "enum Roshambo {\n"
        + "  SPOCK {\n"
        + "    @Override\n"
        + "    public String toString() {\n"
        + "      return \"west side\";\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  /** https://github.com/square/javapoet/issues/253 */
  @Test public void enumWithAnnotatedValues() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("")
            .addAnnotation(Deprecated.class)
            .build())
        .addEnumConstant("PAPER")
        .addEnumConstant("SCISSORS")
        .build();
    assertThat(toString(roshambo)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "\n"
        + "public enum Roshambo {\n"
        + "  @Deprecated\n"
        + "  ROCK,\n"
        + "\n"
        + "  PAPER,\n"
        + "\n"
        + "  SCISSORS\n"
        + "}\n");
  }

  @Test public void methodThrows() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addModifiers(Modifier.ABSTRACT)
        .addMethod(MethodSpec.methodBuilder("throwOne")
            .addException(IOException.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("throwTwo")
            .addException(IOException.class)
            .addException(ClassName.get(tacosPackage, "SourCreamException"))
            .build())
        .addMethod(MethodSpec.methodBuilder("abstractThrow")
            .addModifiers(Modifier.ABSTRACT)
            .addException(IOException.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("nativeThrow")
            .addModifiers(Modifier.NATIVE)
            .addException(IOException.class)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.IOException;\n"
        + "\n"
        + "abstract class Taco {\n"
        + "  void throwOne() throws IOException {\n"
        + "  }\n"
        + "\n"
        + "  void throwTwo() throws IOException, SourCreamException {\n"
        + "  }\n"
        + "\n"
        + "  abstract void abstractThrow() throws IOException;\n"
        + "\n"
        + "  native void nativeThrow() throws IOException;\n"
        + "}\n");
  }

  @Test public void typeVariables() throws Exception {
    TypeVariableName t = TypeVariableName.get("T");
    TypeVariableName p = TypeVariableName.get("P", Number.class);
    ClassName location = ClassName.get(tacosPackage, "Location");
    TypeSpec typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), p))
        .addField(t, "label")
        .addField(p, "x")
        .addField(p, "y")
        .addMethod(MethodSpec.methodBuilder("compareTo")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(p, "p")
            .addCode("return 0;\n")
            .build())
        .addMethod(MethodSpec.methodBuilder("of")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(t)
            .addTypeVariable(p)
            .returns(ParameterizedTypeName.get(location, t, p))
            .addParameter(t, "label")
            .addParameter(p, "x")
            .addParameter(p, "y")
            .addCode("throw new $T($S);\n", UnsupportedOperationException.class, "TODO")
            .build())
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Comparable;\n"
        + "import java.lang.Number;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.UnsupportedOperationException;\n"
        + "\n"
        + "class Location<T, P extends Number> implements Comparable<P> {\n"
        + "  T label;\n"
        + "\n"
        + "  P x;\n"
        + "\n"
        + "  P y;\n"
        + "\n"
        + "  @Override\n"
        + "  public int compareTo(P p) {\n"
        + "    return 0;\n"
        + "  }\n"
        + "\n"
        + "  public static <T, P extends Number> Location<T, P> of(T label, P x, P y) {\n"
        + "    throw new UnsupportedOperationException(\"TODO\");\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void classImplementsExtends() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Taco");
    ClassName food = ClassName.get("com.squareup.tacos", "Food");
    TypeSpec typeSpec = TypeSpec.classBuilder("Taco")
        .addModifiers(Modifier.ABSTRACT)
        .superclass(ParameterizedTypeName.get(ClassName.get(AbstractSet.class), food))
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), taco))
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Comparable;\n"
        + "import java.util.AbstractSet;\n"
        + "\n"
        + "abstract class Taco extends AbstractSet<Food> "
        + "implements Serializable, Comparable<Taco> {\n"
        + "}\n");
  }

  @Test public void enumImplements() throws Exception {
    TypeSpec typeSpec = TypeSpec.enumBuilder("Food")
        .addSuperinterface(Serializable.class)
        .addSuperinterface(Cloneable.class)
        .addEnumConstant("LEAN_GROUND_BEEF")
        .addEnumConstant("SHREDDED_CHEESE")
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Cloneable;\n"
        + "\n"
        + "enum Food implements Serializable, Cloneable {\n"
        + "  LEAN_GROUND_BEEF,\n"
        + "\n"
        + "  SHREDDED_CHEESE\n"
        + "}\n");
  }

  @Test public void interfaceExtends() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Taco");
    TypeSpec typeSpec = TypeSpec.interfaceBuilder("Taco")
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Comparable.class), taco))
        .build();
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.lang.Comparable;\n"
        + "\n"
        + "interface Taco extends Serializable, Comparable<Taco> {\n"
        + "}\n");
  }

  @Test public void nestedClasses() throws Exception {
    ClassName taco = ClassName.get(tacosPackage, "Combo", "Taco");
    ClassName topping = ClassName.get(tacosPackage, "Combo", "Taco", "Topping");
    ClassName chips = ClassName.get(tacosPackage, "Combo", "Chips");
    ClassName sauce = ClassName.get(tacosPackage, "Combo", "Sauce");
    TypeSpec typeSpec = TypeSpec.classBuilder("Combo")
        .addField(taco, "taco")
        .addField(chips, "chips")
        .addType(TypeSpec.classBuilder(taco.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(ParameterizedTypeName.get(ClassName.get(List.class), topping), "toppings")
            .addField(sauce, "sauce")
            .addType(TypeSpec.enumBuilder(topping.simpleName())
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(TypeSpec.classBuilder(chips.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(topping, "topping")
            .addField(sauce, "dippingSauce")
            .build())
        .addType(TypeSpec.enumBuilder(sauce.simpleName())
            .addEnumConstant("SOUR_CREAM")
            .addEnumConstant("SALSA")
            .addEnumConstant("QUESO")
            .addEnumConstant("MILD")
            .addEnumConstant("FIRE")
            .build())
        .build();

    assertThat(toString(typeSpec)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Combo {\n"
        + "  Taco taco;\n"
        + "\n"
        + "  Chips chips;\n"
        + "\n"
        + "  static class Taco {\n"
        + "    List<Topping> toppings;\n"
        + "\n"
        + "    Sauce sauce;\n"
        + "\n"
        + "    enum Topping {\n"
        + "      SHREDDED_CHEESE,\n"
        + "\n"
        + "      LEAN_GROUND_BEEF\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  static class Chips {\n"
        + "    Taco.Topping topping;\n"
        + "\n"
        + "    Sauce dippingSauce;\n"
        + "  }\n"
        + "\n"
        + "  enum Sauce {\n"
        + "    SOUR_CREAM,\n"
        + "\n"
        + "    SALSA,\n"
        + "\n"
        + "    QUESO,\n"
        + "\n"
        + "    MILD,\n"
        + "\n"
        + "    FIRE\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void annotation() throws Exception {
    TypeSpec annotation = TypeSpec.annotationBuilder("MyAnnotation")
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .defaultValue("$L", 0)
            .returns(int.class)
            .build())
        .build();

    assertThat(toString(annotation)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "public @interface MyAnnotation {\n"
        + "  int test() default 0;\n"
        + "}\n"
    );
  }

  @Test public void innerAnnotationInAnnotationDeclaration() throws Exception {
    TypeSpec bar = TypeSpec.annotationBuilder("Bar")
        .addMethod(MethodSpec.methodBuilder("value")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .defaultValue("@$T", Deprecated.class)
            .returns(Deprecated.class)
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "\n"
        + "@interface Bar {\n"
        + "  Deprecated value() default @Deprecated;\n"
        + "}\n"
    );
  }

  @Test
  public void classCannotHaveDefaultValueForMethod() throws Exception {
    try {
      TypeSpec.classBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PUBLIC)
              .defaultValue("0")
              .returns(int.class)
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {}
  }

  @Test
  public void classCannotHaveDefaultMethods() throws Exception {
    assumeTrue(isJava8());
    try {
      TypeSpec.classBuilder("Tacos")
          .addMethod(MethodSpec.methodBuilder("test")
              .addModifiers(Modifier.PUBLIC, Modifier.valueOf("DEFAULT"))
              .returns(int.class)
              .addCode(CodeBlock.builder().addStatement("return 0").build())
              .build())
          .build();
      fail();
    } catch (IllegalStateException expected) {}
  }

  @Test
  public void interfaceStaticMethods() throws Exception {
    TypeSpec bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
            + "package com.squareup.tacos;\n"
            + "\n"
            + "interface Tacos {\n"
            + "  static int test() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "}\n"
    );
  }

  @Test
  public void interfaceDefaultMethods() throws Exception {
    assumeTrue(isJava8());
    TypeSpec bar = TypeSpec.interfaceBuilder("Tacos")
        .addMethod(MethodSpec.methodBuilder("test")
            .addModifiers(Modifier.PUBLIC, Modifier.valueOf("DEFAULT"))
            .returns(int.class)
            .addCode(CodeBlock.builder().addStatement("return 0").build())
            .build())
        .build();

    assertThat(toString(bar)).isEqualTo(""
            + "package com.squareup.tacos;\n"
            + "\n"
            + "interface Tacos {\n"
            + "  default int test() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "}\n"
    );
  }

  @Test public void referencedAndDeclaredSimpleNamesConflict() throws Exception {
    FieldSpec internalTop = FieldSpec.builder(
        ClassName.get(tacosPackage, "Top"), "internalTop").build();
    FieldSpec internalBottom = FieldSpec.builder(
        ClassName.get(tacosPackage, "Top", "Middle", "Bottom"), "internalBottom").build();
    FieldSpec externalTop = FieldSpec.builder(
        ClassName.get(donutsPackage, "Top"), "externalTop").build();
    FieldSpec externalBottom = FieldSpec.builder(
        ClassName.get(donutsPackage, "Bottom"), "externalBottom").build();
    TypeSpec top = TypeSpec.classBuilder("Top")
        .addField(internalTop)
        .addField(internalBottom)
        .addField(externalTop)
        .addField(externalBottom)
        .addType(TypeSpec.classBuilder("Middle")
            .addField(internalTop)
            .addField(internalBottom)
            .addField(externalTop)
            .addField(externalBottom)
            .addType(TypeSpec.classBuilder("Bottom")
                .addField(internalTop)
                .addField(internalBottom)
                .addField(externalTop)
                .addField(externalBottom)
                .build())
            .build())
        .build();
    assertThat(toString(top)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.donuts.Bottom;\n"
        + "\n"
        + "class Top {\n"
        + "  Top internalTop;\n"
        + "\n"
        + "  Middle.Bottom internalBottom;\n"
        + "\n"
        + "  com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "  Bottom externalBottom;\n"
        + "\n"
        + "  class Middle {\n"
        + "    Top internalTop;\n"
        + "\n"
        + "    Bottom internalBottom;\n"
        + "\n"
        + "    com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "    com.squareup.donuts.Bottom externalBottom;\n"
        + "\n"
        + "    class Bottom {\n"
        + "      Top internalTop;\n"
        + "\n"
        + "      Bottom internalBottom;\n"
        + "\n"
        + "      com.squareup.donuts.Top externalTop;\n"
        + "\n"
        + "      com.squareup.donuts.Bottom externalBottom;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void simpleNamesConflictInThisAndOtherPackage() throws Exception {
    FieldSpec internalOther = FieldSpec.builder(
        ClassName.get(tacosPackage, "Other"), "internalOther").build();
    FieldSpec externalOther = FieldSpec.builder(
        ClassName.get(donutsPackage, "Other"), "externalOther").build();
    TypeSpec gen = TypeSpec.classBuilder("Gen")
        .addField(internalOther)
        .addField(externalOther)
        .build();
    assertThat(toString(gen)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Gen {\n"
        + "  Other internalOther;\n"
        + "\n"
        + "  com.squareup.donuts.Other externalOther;\n"
        + "}\n");
  }

  @Test public void originatingElementsIncludesThoseOfNestedTypes() {
    Element outerElement = Mockito.mock(Element.class);
    Element innerElement = Mockito.mock(Element.class);
    TypeSpec outer = TypeSpec.classBuilder("Outer")
        .addOriginatingElement(outerElement)
        .addType(TypeSpec.classBuilder("Inner")
            .addOriginatingElement(innerElement)
            .build())
        .build();
    assertThat(outer.originatingElements).containsExactly(outerElement, innerElement);
  }

  @Test public void intersectionType() {
    TypeVariableName typeVariable = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("getComparator")
            .addTypeVariable(typeVariable)
            .returns(typeVariable)
            .addCode("return null;\n")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.util.Comparator;\n"
        + "\n"
        + "class Taco {\n"
        + "  <T extends Comparator & Serializable> T getComparator() {\n"
        + "    return null;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void arrayType() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(int[].class, "ints")
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  int[] ints;\n"
        + "}\n");
  }

  @Test public void javadoc() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addJavadoc("A hard or soft tortilla, loosely folded and filled with whatever {@link \n")
        .addJavadoc("{@link $T random} tex-mex stuff we could find in the pantry.\n", Random.class)
        .addField(FieldSpec.builder(boolean.class, "soft")
            .addJavadoc("True for a soft flour tortilla; false for a crunchy corn tortilla.\n")
            .build())
        .addMethod(MethodSpec.methodBuilder("refold")
            .addJavadoc("Folds the back of this taco to reduce sauce leakage.\n"
                + "\n"
                + "<p>For {@link $T#KOREAN}, the front may also be folded.\n", Locale.class)
            .addParameter(Locale.class, "locale")
            .build())
        .build();
    // Mentioning a type in Javadoc will not cause an import to be added (java.util.Random here),
    // but the short name will be used if it's already imported (java.util.Locale here).
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Locale;\n"
        + "\n"
        + "/**\n"
        + " * A hard or soft tortilla, loosely folded and filled with whatever {@link \n"
        + " * {@link java.util.Random random} tex-mex stuff we could find in the pantry.\n"
        + " */\n"
        + "class Taco {\n"
        + "  /**\n"
        + "   * True for a soft flour tortilla; false for a crunchy corn tortilla.\n"
        + "   */\n"
        + "  boolean soft;\n"
        + "\n"
        + "  /**\n"
        + "   * Folds the back of this taco to reduce sauce leakage.\n"
        + "   *\n"
        + "   * <p>For {@link Locale#KOREAN}, the front may also be folded.\n"
        + "   */\n"
        + "  void refold(Locale locale) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void annotationsInAnnotations() throws Exception {
    ClassName beef = ClassName.get(tacosPackage, "Beef");
    ClassName chicken = ClassName.get(tacosPackage, "Chicken");
    ClassName option = ClassName.get(tacosPackage, "Option");
    ClassName mealDeal = ClassName.get(tacosPackage, "MealDeal");
    TypeSpec menu = TypeSpec.classBuilder("Menu")
        .addAnnotation(AnnotationSpec.builder(mealDeal)
            .addMember("price", "$L", 500)
            .addMember("options", "$L", AnnotationSpec.builder(option)
                .addMember("name", "$S", "taco")
                .addMember("meat", "$T.class", beef)
                .build())
            .addMember("options", "$L", AnnotationSpec.builder(option)
                .addMember("name", "$S", "quesadilla")
                .addMember("meat", "$T.class", chicken)
                .build())
            .build())
        .build();
    assertThat(toString(menu)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "@MealDeal(\n"
        + "    price = 500,\n"
        + "    options = {\n"
        + "        @Option(name = \"taco\", meat = Beef.class),\n"
        + "        @Option(name = \"quesadilla\", meat = Chicken.class)\n"
        + "    }\n"
        + ")\n"
        + "class Menu {\n"
        + "}\n");
  }

  @Test public void varargs() throws Exception {
    TypeSpec taqueria = TypeSpec.classBuilder("Taqueria")
        .addMethod(MethodSpec.methodBuilder("prepare")
            .addParameter(int.class, "workers")
            .addParameter(Runnable[].class, "jobs")
            .varargs()
            .build())
        .build();
    assertThat(toString(taqueria)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Runnable;\n"
        + "\n"
        + "class Taqueria {\n"
        + "  void prepare(int workers, Runnable... jobs) {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void codeBlocks() throws Exception {
    CodeBlock ifBlock = CodeBlock.builder()
        .beginControlFlow("if (!a.equals(b))")
        .addStatement("return i")
        .endControlFlow()
        .build();
    CodeBlock methodBody = CodeBlock.builder()
        .addStatement("$T size = $T.min(listA.size(), listB.size())", int.class, Math.class)
        .beginControlFlow("for ($T i = 0; i < size; i++)", int.class)
        .addStatement("$T $N = $N.get(i)", String.class, "a", "listA")
        .addStatement("$T $N = $N.get(i)", String.class, "b", "listB")
        .add("$L", ifBlock)
        .endControlFlow()
        .addStatement("return size")
        .build();
    CodeBlock fieldBlock = CodeBlock.builder()
        .add("$>$>")
        .add("\n$T.<$T, $T>builder()$>$>", ImmutableMap.class, String.class, String.class)
        .add("\n.add($S, $S)", '\'', "&#39;")
        .add("\n.add($S, $S)", '&', "&amp;")
        .add("\n.add($S, $S)", '<', "&lt;")
        .add("\n.add($S, $S)", '>', "&gt;")
        .add("\n.build()$<$<")
        .add("$<$<")
        .build();
    FieldSpec escapeHtml = FieldSpec.builder(ParameterizedTypeName.get(
        Map.class, String.class, String.class), "ESCAPE_HTML")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer(fieldBlock)
        .build();
    TypeSpec util = TypeSpec.classBuilder("Util")
        .addField(escapeHtml)
        .addMethod(MethodSpec.methodBuilder("commonPrefixLength")
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(List.class, String.class), "listA")
            .addParameter(ParameterizedTypeName.get(List.class, String.class), "listB")
            .addCode(methodBody)
            .build())
        .build();
    assertThat(toString(util)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.google.common.collect.ImmutableMap;\n"
        + "import java.lang.Math;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "class Util {\n"
        + "  private static final Map<String, String> ESCAPE_HTML = \n"
        + "      ImmutableMap.<String, String>builder()\n"
        + "          .add(\"\'\", \"&#39;\")\n"
        + "          .add(\"&\", \"&amp;\")\n"
        + "          .add(\"<\", \"&lt;\")\n"
        + "          .add(\">\", \"&gt;\")\n"
        + "          .build();\n"
        + "\n"
        + "  int commonPrefixLength(List<String> listA, List<String> listB) {\n"
        + "    int size = Math.min(listA.size(), listB.size());\n"
        + "    for (int i = 0; i < size; i++) {\n"
        + "      String a = listA.get(i);\n"
        + "      String b = listB.get(i);\n"
        + "      if (!a.equals(b)) {\n"
        + "        return i;\n"
        + "      }\n"
        + "    }\n"
        + "    return size;\n"
        + "  }\n"
        + "}\n");
  }
  
  @Test public void indexedElseIf() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("choices")
            .beginControlFlow("if ($1L != null || $1L == $2L)", "taco", "otherTaco")
            .addStatement("$T.out.println($S)", System.class, "only one taco? NOO!")
            .nextControlFlow("else if ($1L.$3L && $2L.$3L)", "taco", "otherTaco", "isSupreme()")
            .addStatement("$T.out.println($S)", System.class, "taco heaven")
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void choices() {\n"
        + "    if (taco != null || taco == otherTaco) {\n"
        + "      System.out.println(\"only one taco? NOO!\");\n"
        + "    } else if (taco.isSupreme() && otherTaco.isSupreme()) {\n"
        + "      System.out.println(\"taco heaven\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void elseIf() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("choices")
            .beginControlFlow("if (5 < 4) ")
            .addStatement("$T.out.println($S)", System.class, "wat")
            .nextControlFlow("else if (5 < 6)")
            .addStatement("$T.out.println($S)", System.class, "hello")
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void choices() {\n"
        + "    if (5 < 4)  {\n"
        + "      System.out.println(\"wat\");\n"
        + "    } else if (5 < 6) {\n"
        + "      System.out.println(\"hello\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void doWhile() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("loopForever")
            .beginControlFlow("do")
            .addStatement("$T.out.println($S)", System.class, "hello")
            .endControlFlow("while (5 < 6)")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void loopForever() {\n"
        + "    do {\n"
        + "      System.out.println(\"hello\");\n"
        + "    } while (5 < 6);\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void inlineIndent() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("inlineIndent")
            .addCode("if (3 < 4) {\n$>$T.out.println($S);\n$<}\n", System.class, "hello")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class Taco {\n"
        + "  void inlineIndent() {\n"
        + "    if (3 < 4) {\n"
        + "      System.out.println(\"hello\");\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultModifiersForInterfaceMembers() throws Exception {
    TypeSpec taco = TypeSpec.interfaceBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "SHELL")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "crunchy corn")
            .build())
        .addMethod(MethodSpec.methodBuilder("fold")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .addType(TypeSpec.classBuilder("Topping")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "interface Taco {\n"
        + "  String SHELL = \"crunchy corn\";\n"
        + "\n"
        + "  void fold();\n"
        + "\n"
        + "  class Topping {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultModifiersForMemberInterfacesAndEnums() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addType(TypeSpec.classBuilder("Meat")
            .addModifiers(Modifier.STATIC)
            .build())
        .addType(TypeSpec.interfaceBuilder("Tortilla")
            .addModifiers(Modifier.STATIC)
            .build())
        .addType(TypeSpec.enumBuilder("Topping")
            .addModifiers(Modifier.STATIC)
            .addEnumConstant("SALSA")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  static class Meat {\n"
        + "  }\n"
        + "\n"
        + "  interface Tortilla {\n"
        + "  }\n"
        + "\n"
        + "  enum Topping {\n"
        + "    SALSA\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void membersOrdering() throws Exception {
    // Hand out names in reverse-alphabetical order to defend against unexpected sorting.
    TypeSpec taco = TypeSpec.classBuilder("Members")
        .addType(TypeSpec.classBuilder("Z").build())
        .addType(TypeSpec.classBuilder("Y").build())
        .addField(String.class, "X", Modifier.STATIC)
        .addField(String.class, "W")
        .addField(String.class, "V", Modifier.STATIC)
        .addField(String.class, "U")
        .addMethod(MethodSpec.methodBuilder("T").addModifiers(Modifier.STATIC).build())
        .addMethod(MethodSpec.methodBuilder("S").build())
        .addMethod(MethodSpec.methodBuilder("R").addModifiers(Modifier.STATIC).build())
        .addMethod(MethodSpec.methodBuilder("Q").build())
        .addMethod(MethodSpec.constructorBuilder().addParameter(int.class, "p").build())
        .addMethod(MethodSpec.constructorBuilder().addParameter(long.class, "o").build())
        .build();
    // Static fields, instance fields, constructors, methods, classes.
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Members {\n"
        + "  static String X;\n"
        + "\n"
        + "  static String V;\n"
        + "\n"
        + "  String W;\n"
        + "\n"
        + "  String U;\n"
        + "\n"
        + "  Members(int p) {\n"
        + "  }\n"
        + "\n"
        + "  Members(long o) {\n"
        + "  }\n"
        + "\n"
        + "  static void T() {\n"
        + "  }\n"
        + "\n"
        + "  void S() {\n"
        + "  }\n"
        + "\n"
        + "  static void R() {\n"
        + "  }\n"
        + "\n"
        + "  void Q() {\n"
        + "  }\n"
        + "\n"
        + "  class Z {\n"
        + "  }\n"
        + "\n"
        + "  class Y {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void nativeMethods() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("nativeInt")
            .addModifiers(Modifier.NATIVE)
            .returns(int.class)
            .build())
        // GWT JSNI
        .addMethod(MethodSpec.methodBuilder("alert")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.NATIVE)
            .addParameter(String.class, "msg")
            .addCode(CodeBlock.builder()
                .add(" /*-{\n")
                .indent()
                .addStatement("$$wnd.alert(msg)")
                .unindent()
                .add("}-*/")
                .build())
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  native int nativeInt();\n"
        + "\n"
        + "  public static native void alert(String msg) /*-{\n"
        + "    $wnd.alert(msg);\n"
        + "  }-*/;\n"
        + "}\n");
  }

  @Test public void nullStringLiteral() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "NULL")
            .initializer("$S", (Object) null)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  String NULL = null;\n"
        + "}\n");
  }

  @Test public void annotationToString() throws Exception {
    AnnotationSpec annotation = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "unused")
        .build();
    assertThat(annotation.toString()).isEqualTo("@java.lang.SuppressWarnings(\"unused\")");
  }

  @Test public void codeBlockToString() throws Exception {
    CodeBlock codeBlock = CodeBlock.builder()
        .addStatement("$T $N = $S.substring(0, 3)", String.class, "s", "taco")
        .build();
    assertThat(codeBlock.toString()).isEqualTo("java.lang.String s = \"taco\".substring(0, 3);\n");
  }

  @Test public void fieldToString() throws Exception {
    FieldSpec field = FieldSpec.builder(String.class, "s", Modifier.FINAL)
        .initializer("$S.substring(0, 3)", "taco")
        .build();
    assertThat(field.toString())
        .isEqualTo("final java.lang.String s = \"taco\".substring(0, 3);\n");
  }

  @Test public void methodToString() throws Exception {
    MethodSpec method = MethodSpec.methodBuilder("toString")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return $S", "taco")
        .build();
    assertThat(method.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public java.lang.String toString() {\n"
        + "  return \"taco\";\n"
        + "}\n");
  }

  @Test public void constructorToString() throws Exception {
    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(tacosPackage, "Taco"), "taco")
        .addStatement("this.$N = $N", "taco", "taco")
        .build();
    assertThat(constructor.toString()).isEqualTo(""
        + "public Constructor(com.squareup.tacos.Taco taco) {\n"
        + "  this.taco = taco;\n"
        + "}\n");
  }

  @Test public void parameterToString() throws Exception {
    ParameterSpec parameter = ParameterSpec.builder(ClassName.get(tacosPackage, "Taco"), "taco")
        .addModifiers(Modifier.FINAL)
        .addAnnotation(ClassName.get("javax.annotation", "Nullable"))
        .build();
    assertThat(parameter.toString())
        .isEqualTo("@javax.annotation.Nullable final com.squareup.tacos.Taco taco");
  }

  @Test public void classToString() throws Exception {
    TypeSpec type = TypeSpec.classBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void anonymousClassToString() throws Exception {
    TypeSpec type = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(Runnable.class)
        .addMethod(MethodSpec.methodBuilder("run")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .build())
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "new java.lang.Runnable() {\n"
        + "  @java.lang.Override\n"
        + "  public void run() {\n"
        + "  }\n"
        + "}");
  }

  @Test public void interfaceClassToString() throws Exception {
    TypeSpec type = TypeSpec.interfaceBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "interface Taco {\n"
        + "}\n");
  }

  @Test public void annotationDeclarationToString() throws Exception {
    TypeSpec type = TypeSpec.annotationBuilder("Taco")
        .build();
    assertThat(type.toString()).isEqualTo(""
        + "@interface Taco {\n"
        + "}\n");
  }

  private String toString(TypeSpec typeSpec) {
    return JavaFile.builder(tacosPackage, typeSpec).build().toString();
  }

  @Test public void multilineStatement() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S\n+ $S\n+ $S\n+ $S\n+ $S",
                "Taco(", "beef,", "lettuce,", "cheese", ")")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return \"Taco(\"\n"
        + "        + \"beef,\"\n"
        + "        + \"lettuce,\"\n"
        + "        + \"cheese\"\n"
        + "        + \")\";\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void multilineStatementWithAnonymousClass() throws Exception {
    TypeName stringComparator = ParameterizedTypeName.get(Comparator.class, String.class);
    TypeName listOfString = ParameterizedTypeName.get(List.class, String.class);
    TypeSpec prefixComparator = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(stringComparator)
        .addMethod(MethodSpec.methodBuilder("compare")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(String.class, "a")
            .addParameter(String.class, "b")
            .addStatement("return a.substring(0, length)\n"
                + ".compareTo(b.substring(0, length))")
            .build())
        .build();
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("comparePrefix")
            .returns(stringComparator)
            .addParameter(int.class, "length", Modifier.FINAL)
            .addStatement("return $L", prefixComparator)
            .build())
        .addMethod(MethodSpec.methodBuilder("sortPrefix")
            .addParameter(listOfString, "list")
            .addParameter(int.class, "length", Modifier.FINAL)
            .addStatement("$T.sort(\nlist,\n$L)", Collections.class, prefixComparator)
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "import java.util.Collections;\n"
        + "import java.util.Comparator;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Taco {\n"
        + "  Comparator<String> comparePrefix(final int length) {\n"
        + "    return new Comparator<String>() {\n"
        + "      @Override\n"
        + "      public int compare(String a, String b) {\n"
        + "        return a.substring(0, length)\n"
        + "            .compareTo(b.substring(0, length));\n"
        + "      }\n"
        + "    };\n"
        + "  }\n"
        + "\n"
        + "  void sortPrefix(List<String> list, final int length) {\n"
        + "    Collections.sort(\n"
        + "        list,\n"
        + "        new Comparator<String>() {\n"
        + "          @Override\n"
        + "          public int compare(String a, String b) {\n"
        + "            return a.substring(0, length)\n"
        + "                .compareTo(b.substring(0, length));\n"
        + "          }\n"
        + "        });\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void multilineStrings() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder(String.class, "toppings")
            .initializer("$S", "shell\nbeef\nlettuce\ncheese\n")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  String toppings = \"shell\\n\"\n"
        + "      + \"beef\\n\"\n"
        + "      + \"lettuce\\n\"\n"
        + "      + \"cheese\\n\";\n"
        + "}\n");
  }

  @Test public void doubleFieldInitialization() {
    try {
      FieldSpec.builder(String.class, "listA")
          .initializer("foo")
          .initializer("bar")
          .build();
      fail();
    } catch (IllegalStateException expected) {}

    try {
      FieldSpec.builder(String.class, "listA")
          .initializer(CodeBlock.builder().add("foo").build())
          .initializer(CodeBlock.builder().add("bar").build())
          .build();
      fail();
    } catch (IllegalStateException expected) {}
  }

  @Test public void nullAnnotationsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addAnnotations(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("annotationSpecs == null");
    }
  }

  @Test public void multipleAnnotationAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotations(Arrays.asList(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .build(),
            AnnotationSpec.builder(Deprecated.class).build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Deprecated;\n"
        + "import java.lang.SuppressWarnings;\n"
        + "\n"
        + "@SuppressWarnings(\"unchecked\")\n"
        + "@Deprecated\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void nullFieldsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addFields(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("fieldSpecs == null");
    }
  }

  @Test public void multipleFieldAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addFields(Arrays.asList(
            FieldSpec.builder(int.class, "ANSWER", Modifier.STATIC, Modifier.FINAL).build(),
            FieldSpec.builder(BigDecimal.class, "price", Modifier.PRIVATE).build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.math.BigDecimal;\n"
        + "\n"
        + "class Taco {\n"
        + "  static final int ANSWER;\n"
        + "\n"
        + "  private BigDecimal price;\n"
        + "}\n");
  }

  @Test public void nullMethodsAddition() {
    try {
      TypeSpec.classBuilder("Taco").addMethods(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("methodSpecs == null");
    }
  }

  @Test public void multipleMethodAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethods(Arrays.asList(
            MethodSpec.methodBuilder("getAnswer")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addStatement("return $L", 42)
                .build(),
            MethodSpec.methodBuilder("getRandomQuantity")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addJavadoc("chosen by fair dice roll ;)")
                .addStatement("return $L", 4)
                .build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  public static int getAnswer() {\n"
        + "    return 42;\n"
        + "  }\n"
        + "\n"
        + "  /**\n"
        + "   * chosen by fair dice roll ;) */\n"
        + "  public int getRandomQuantity() {\n"
        + "    return 4;\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void nullSuperinterfacesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addSuperinterfaces(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("superinterfaces == null");
    }
  }

  @Test public void multipleSuperinterfaceAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addSuperinterfaces(Arrays.asList(
            TypeName.get(Serializable.class),
            TypeName.get(EventListener.class)))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.Serializable;\n"
        + "import java.util.EventListener;\n"
        + "\n"
        + "class Taco implements Serializable, EventListener {\n"
        + "}\n");
  }

  @Test public void nullTypeVariablesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addTypeVariables(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("typeVariables == null");
    }
  }

  @Test public void multipleTypeVariableAddition() {
    TypeSpec location = TypeSpec.classBuilder("Location")
        .addTypeVariables(Arrays.asList(
            TypeVariableName.get("T"),
            TypeVariableName.get("P", Number.class)))
        .build();
    assertThat(toString(location)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Number;\n"
        + "\n"
        + "class Location<T, P extends Number> {\n"
        + "}\n");
  }

  @Test public void nullTypesAddition() {
    try {
      TypeSpec.classBuilder("Taco").addTypes(null);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("typeSpecs == null");
    }
  }

  @Test public void multipleTypeAddition() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addTypes(Arrays.asList(
            TypeSpec.classBuilder("Topping").build(),
            TypeSpec.classBuilder("Sauce").build()))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  class Topping {\n"
        + "  }\n"
        + "\n"
        + "  class Sauce {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void tryCatch() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("addTopping")
            .addParameter(ClassName.get("com.squareup.tacos", "Topping"), "topping")
            .beginControlFlow("try")
            .addCode("/* do something tricky with the topping */\n")
            .nextControlFlow("catch ($T e)",
                ClassName.get("com.squareup.tacos", "IllegalToppingException"))
            .endControlFlow()
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  void addTopping(Topping topping) {\n"
        + "    try {\n"
        + "      /* do something tricky with the topping */\n"
        + "    } catch (IllegalToppingException e) {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void ifElse() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(
            MethodSpec.methodBuilder("isDelicious")
                .addParameter(TypeName.INT, "count")
                .returns(TypeName.BOOLEAN)
                .beginControlFlow("if (count > 0)")
                .addStatement("return true")
                .nextControlFlow("else")
                .addStatement("return false")
                .endControlFlow()
                .build()
        )
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  boolean isDelicious(int count) {\n"
        + "    if (count > 0) {\n"
        + "      return true;\n"
        + "    } else {\n"
        + "      return false;\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void literalFromAnything() {
    Object value = new Object() {
      @Override public String toString() {
        return "foo";
      }
    };
    assertThat(codeBlock("$L", value).toString()).isEqualTo("foo");
  }

  @Test public void nameFromCharSequence() {
    assertThat(codeBlock("$N", "text").toString()).isEqualTo("text");
  }

  @Test public void nameFromField() {
    FieldSpec field = FieldSpec.builder(String.class, "field").build();
    assertThat(codeBlock("$N", field).toString()).isEqualTo("field");
  }

  @Test public void nameFromParameter() {
    ParameterSpec parameter = ParameterSpec.builder(String.class, "parameter").build();
    assertThat(codeBlock("$N", parameter).toString()).isEqualTo("parameter");
  }

  @Test public void nameFromMethod() {
    MethodSpec method = MethodSpec.methodBuilder("method")
        .addModifiers(Modifier.ABSTRACT)
        .returns(String.class)
        .build();
    assertThat(codeBlock("$N", method).toString()).isEqualTo("method");
  }

  @Test public void nameFromType() {
    TypeSpec type = TypeSpec.classBuilder("Type").build();
    assertThat(codeBlock("$N", type).toString()).isEqualTo("Type");
  }

  @Test public void nameFromUnsupportedType() {
    try {
      CodeBlock.builder().add("$N", String.class);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("expected name but was " + String.class);
    }
  }

  @Test public void stringFromAnything() {
    Object value = new Object() {
      @Override public String toString() {
        return "foo";
      }
    };
    assertThat(codeBlock("$S", value).toString()).isEqualTo("\"foo\"");
  }

  @Test public void stringFromNull() {
    assertThat(codeBlock("$S", new Object[] { null }).toString()).isEqualTo("null");
  }

  @Test public void typeFromTypeName() {
    TypeName typeName = TypeName.get(String.class);
    assertThat(codeBlock("$T", typeName).toString()).isEqualTo("java.lang.String");
  }

  @Test public void typeFromTypeMirror() {
    TypeMirror mirror = getElement(String.class).asType();
    assertThat(codeBlock("$T", mirror).toString()).isEqualTo("java.lang.String");
  }

  @Test public void typeFromTypeElement() {
    TypeElement element = getElement(String.class);
    assertThat(codeBlock("$T", element).toString()).isEqualTo("java.lang.String");
  }

  @Test public void typeFromReflectType() {
    assertThat(codeBlock("$T", String.class).toString()).isEqualTo("java.lang.String");
  }

  @Test public void typeFromUnsupportedType() {
    try {
      CodeBlock.builder().add("$T", "java.lang.String");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("expected type but was java.lang.String");
    }
  }

  @Test public void tooFewArguments() {
    try {
      CodeBlock.builder().add("$S");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("index 1 for '$S' not in range (received 0 arguments)");
    }
  }

  @Test public void unusedArgumentsRelative() {
    try {
      CodeBlock.builder().add("$L $L", "a", "b", "c");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("unused arguments: expected 2, received 3");
    }
  }

  @Test public void unusedArgumentsIndexed() {
    try {
      CodeBlock.builder().add("$1L $2L", "a", "b", "c");
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("unused arguments: expected 2, received 3");
    }
  }

  @Test public void invalidSuperClass() {
    try {
      TypeSpec.classBuilder("foo")
          .superclass(ClassName.get(List.class))
          .superclass(ClassName.get(Map.class));
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      TypeSpec.classBuilder("foo")
          .superclass(TypeName.INT);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void staticCodeBlock() {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
            .addField(String.class, "mFoo", Modifier.PRIVATE)
            .addField(String.class, "FOO", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .addStaticBlock(CodeBlock.builder()
                    .addStatement("FOO = $S", "FOO")
                    .build())
            .addMethod(MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addCode("return FOO;\n")
                    .build())
            .build();
      assertThat(toString(taco)).isEqualTo(""
            + "package com.squareup.tacos;\n"
            + "\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "\n"
            + "class Taco {\n"
            + "  private static final String FOO;\n\n"
            + "  static {\n"
            + "    FOO = \"FOO\";\n"
            + "  }\n\n"
            + "  private String mFoo;\n\n"
            + "  @Override\n"
            + "  public String toString() {\n"
            + "    return FOO;\n"
            + "  }\n"
            + "}\n");
  }

  @Test public void equalsAndHashCode() {
    TypeSpec a = TypeSpec.interfaceBuilder("taco").build();
    TypeSpec b = TypeSpec.interfaceBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.classBuilder("taco").build();
    b = TypeSpec.classBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build();
    b = TypeSpec.enumBuilder("taco").addEnumConstant("SALSA").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = TypeSpec.annotationBuilder("taco").build();
    b = TypeSpec.annotationBuilder("taco").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  private CodeBlock codeBlock(String format, Object... args) {
    return CodeBlock.builder()
        .add(format, args)
        .build();
  }
}
