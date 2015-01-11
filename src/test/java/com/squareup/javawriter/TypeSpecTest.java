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
package com.squareup.javawriter;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.List;
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
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder("extendsObject")
            .type(Types.parameterizedType(List.class, Types.subtypeOf(Object.class)))
            .build())
        .addField(FieldSpec.builder("extendsSerializable")
            .type(Types.parameterizedType(ClassName.fromClass(List.class),
                Types.subtypeOf(Serializable.class)))
            .build())
        .addField(FieldSpec.builder("superString")
            .type(Types.parameterizedType(ClassName.fromClass(List.class),
                Types.supertypeOf(String.class)))
            .build())
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
    ClassName foo = ClassName.create(tacosPackage, "Foo");
    ClassName bar = ClassName.create(tacosPackage, "Bar");
    ClassName thingThang = ClassName.create(
        tacosPackage, ImmutableList.of("Thing"), "Thang");
    ParameterizedType thingThangOfFooBar
        = Types.parameterizedType(thingThang, foo, bar);
    ClassName thung = ClassName.create(tacosPackage, "Thung");
    ClassName simpleThung = ClassName.create(tacosPackage, "SimpleThung");
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
        .addField(FieldSpec.builder("NAME")
            .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.FINAL)
            .type(thingThangOfFooBar)
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
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "two")
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
                .build())
            .addParameter(ParameterSpec.builder(String.class, "three")
                .addAnnotation(
                    AnnotationSpec.builder(ClassName.create(tacosPackage, "Pong"))
                        .addMember("value", "$S", "pong")
                        .build())
                .build())
            .addParameter(ParameterSpec.builder(String.class, "four")
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
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
    ClassName observable = ClassName.create(tacosPackage, "Observable");
    ClassName fooBar = ClassName.create(tacosPackage, "FooBar");
    ClassName thing = ClassName.create(tacosPackage, "Thing");
    ClassName things = ClassName.create(tacosPackage, "Things");
    ClassName map = ClassName.create("java.util", "Map");
    ClassName string = ClassName.create("java.lang", "String");
    ClassName headers = ClassName.create(tacosPackage, "Headers");
    ClassName post = ClassName.create(tacosPackage, "POST");
    ClassName body = ClassName.create(tacosPackage, "Body");
    ClassName queryMap = ClassName.create(tacosPackage, "QueryMap");
    ClassName header = ClassName.create(tacosPackage, "Header");
    TypeSpec service = TypeSpec.interfaceBuilder("Service")
        .addMethod(MethodSpec.methodBuilder("fooBar")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(AnnotationSpec.builder(headers)
                .addMember("value", "{\n$S,\n$S\n}",
                    "Accept: application/json", "User-Agent: foobar")
                .build())
            .addAnnotation(AnnotationSpec.builder(post)
                .addMember("value", "$S", "/foo/bar")
                .build())
            .returns(Types.parameterizedType(observable, fooBar))
            .addParameter(ParameterSpec.builder(Types.parameterizedType(things, thing),
                "things")
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
        + "      })\n"
        + "  @POST(\"/foo/bar\")\n"
        + "  Observable<FooBar> fooBar(@Body Things<Thing> things, @QueryMap(encodeValues = false) "
        + "Map<String, String> query, @Header(\"Authorization\") String authorization);\n"
        + "}\n");
  }

  @Test public void annotatedField() throws Exception {
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addField(FieldSpec.builder("thing")
            .addAnnotation(AnnotationSpec.builder(ClassName.create(tacosPackage, "JsonAdapter"))
                .addMember("value", "$T.class", ClassName.create(tacosPackage, "Foo"))
                .build())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(String.class)
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
    ClassName someType = ClassName.create(tacosPackage, "SomeType");
    TypeSpec taco = TypeSpec.classBuilder("Foo")
        .addAnnotation(AnnotationSpec.builder(ClassName.create(tacosPackage, "Something"))
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
        .addField(FieldSpec.builder("handPosition")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(String.class)
            .build())
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
            .addException(ClassName.create(tacosPackage, "SourCreamException"))
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
    ClassName location = ClassName.create(tacosPackage, "Location");
    TypeSpec typeSpec = TypeSpec.classBuilder("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(Types.parameterizedType(ClassName.fromClass(Comparable.class), p))
        .addField(FieldSpec.builder("label")
            .type(t)
            .build())
        .addField(FieldSpec.builder("x")
            .type(p)
            .build())
        .addField(FieldSpec.builder("y")
            .type(p)
            .build())
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
    ClassName taco = ClassName.create(tacosPackage, "Taco");
    ClassName food = ClassName.create("com.squareup.tacos", "Food");
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
    ClassName taco = ClassName.create(tacosPackage, "Taco");
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
    ClassName taco = ClassName.create(tacosPackage, ImmutableList.of("Combo"), "Taco");
    ClassName topping = ClassName.create(
        tacosPackage, ImmutableList.of("Combo", "Taco"), "Topping");
    ClassName chips = ClassName.create(tacosPackage, ImmutableList.of("Combo"), "Chips");
    ClassName sauce = ClassName.create(tacosPackage, ImmutableList.of("Combo"), "Sauce");
    TypeSpec typeSpec = TypeSpec.classBuilder("Combo")
        .addField(FieldSpec.of(taco, "taco"))
        .addField(FieldSpec.of(chips, "chips"))
        .addType(TypeSpec.classBuilder(taco.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(FieldSpec.of(Types.parameterizedType(List.class, topping), "toppings"))
            .addField(FieldSpec.of(sauce, "sauce"))
            .addType(TypeSpec.enumBuilder(topping.simpleName())
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(TypeSpec.classBuilder(chips.simpleName())
            .addModifiers(Modifier.STATIC)
            .addField(FieldSpec.of(topping, "topping"))
            .addField(FieldSpec.of(sauce, "dippingSauce"))
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
    FieldSpec internalTop = FieldSpec.of(ClassName.create(tacosPackage, "Top"), "internalTop");
    FieldSpec internalBottom = FieldSpec.of(ClassName.create(
        tacosPackage, ImmutableList.of("Top", "Middle"), "Bottom"), "internalBottom");
    FieldSpec externalTop = FieldSpec.of(
        ClassName.create(donutsPackage, "Top"), "externalTop");
    FieldSpec externalBottom = FieldSpec.of(
        ClassName.create(donutsPackage, "Bottom"), "externalBottom");
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
        .addField(FieldSpec.of(int[].class, "ints"))
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  int[] ints;\n"
        + "}\n");
  }

  private String toString(TypeSpec typeSpec) {
    return new JavaFile.Builder()
        .packageName(tacosPackage)
        .typeSpec(typeSpec)
        .build()
        .toString();
  }
}
