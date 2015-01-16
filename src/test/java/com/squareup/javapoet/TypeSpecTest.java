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
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.junit.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class TypeSpecTest {
  private final String tacosPackage = "com.squareup.tacos";
  private static final String donutsPackage = "com.squareup.donuts";

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
  }

  @Test public void interestingTypes() throws Exception {
    ParameterizedType listOfAny = Types.parameterizedType(
        List.class, Types.subtypeOf(Object.class));
    ParameterizedType listOfExtends = Types.parameterizedType(
        List.class, Types.subtypeOf(Serializable.class));
    ParameterizedType listOfSuper = Types.parameterizedType(
        List.class, Types.supertypeOf(String.class));
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
    ParameterizedType thingThangOfFooBar
        = Types.parameterizedType(thingThang, foo, bar);
    ClassName thung = ClassName.get(tacosPackage, "Thung");
    ClassName simpleThung = ClassName.get(tacosPackage, "SimpleThung");
    ParameterizedType thungOfSuperBar
        = Types.parameterizedType(thung, Types.supertypeOf(bar));
    ParameterizedType thungOfSuperFoo
        = Types.parameterizedType(thung, Types.supertypeOf(foo));
    ParameterizedType simpleThungOfBar = Types.parameterizedType(simpleThung, bar);

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
            .returns(Types.parameterizedType(observable, fooBar))
            .addParameter(ParameterSpec.builder(Types.parameterizedType(things, thing), "things")
                .addAnnotation(body)
                .build())
            .addParameter(ParameterSpec.builder(
                Types.parameterizedType(map, string, string), "query")
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
        + "    hello = \"goodbye\",\n"
        + "    hey = 12,\n"
        + "    hi = SomeType.FIELD\n"
        + ")\n"
        + "public class Foo {\n"
        + "}\n");
  }

  @Test public void enumWithSubclassing() throws Exception {
    TypeSpec roshambo = TypeSpec.enumBuilder("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK")
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

  @Test public void methodThrows() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addMethod(MethodSpec.methodBuilder("throwOne")
            .addException(IOException.class)
            .build())
        .addMethod(MethodSpec.methodBuilder("throwTwo")
            .addException(IOException.class)
            .addException(ClassName.get(tacosPackage, "SourCreamException"))
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.io.IOException;\n"
        + "\n"
        + "class Taco {\n"
        + "  void throwOne() throws IOException {\n"
        + "  }\n"
        + "\n"
        + "  void throwTwo() throws IOException, SourCreamException {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void typeVariables() throws Exception {
    TypeVariable<?> t = Types.typeVariable("T");
    TypeVariable<?> p = Types.typeVariable("P", Number.class);
    ClassName location = ClassName.get(tacosPackage, "Location");
    TypeSpec typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(Types.parameterizedType(Comparable.class, p))
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
            .returns(Types.parameterizedType(location, t, p))
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
        .superclass(Types.parameterizedType(AbstractSet.class, food))
        .addSuperinterface(Serializable.class)
        .addSuperinterface(Types.parameterizedType(Comparable.class, taco))
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
        .addSuperinterface(Types.parameterizedType(Comparable.class, taco))
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
            .addField(Types.parameterizedType(List.class, topping), "toppings")
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
    TypeVariable<?> typeVariable = Types.typeVariable("T", Comparator.class, Serializable.class);
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
        + "    options = {\n"
        + "        @Option(meat = Beef.class, name = \"taco\"),\n"
        + "        @Option(meat = Chicken.class, name = \"quesadilla\")\n"
        + "    },\n"
        + "    price = 500\n"
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
    TypeSpec util = TypeSpec.classBuilder("Util")
        .addMethod(MethodSpec.methodBuilder("commonPrefixLength")
            .returns(int.class)
            .addParameter(Types.parameterizedType(List.class, String.class), "listA")
            .addParameter(Types.parameterizedType(List.class, String.class), "listB")
            .addCode(methodBody)
            .build())
        .build();
    assertThat(toString(util)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.lang.Math;\n"
        + "import java.lang.String;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Util {\n"
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
        + "    } else if (5 < 6){\n"
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

  private String toString(TypeSpec typeSpec) {
    return JavaFile.builder(tacosPackage, typeSpec).build().toString();
  }
}
