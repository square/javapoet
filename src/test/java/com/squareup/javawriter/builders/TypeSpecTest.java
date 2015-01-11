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
package com.squareup.javawriter.builders;

import com.google.common.collect.ImmutableList;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.IntersectionTypeName;
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.TypeVariableName;
import com.squareup.javawriter.WildcardName;
import java.io.IOException;
import java.io.Serializable;
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
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addMethod(new MethodSpec.Builder()
            .name("toString")
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
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addField(new FieldSpec.Builder()
            .type(ParameterizedTypeName.create(ClassName.fromClass(List.class),
                WildcardName.createWithUpperBound(ClassName.fromClass(Object.class))))
            .name("extendsObject")
            .build())
        .addField(new FieldSpec.Builder()
            .type(ParameterizedTypeName.create(ClassName.fromClass(List.class),
                WildcardName.createWithUpperBound(ClassName.fromClass(Serializable.class))))
            .name("extendsSerializable")
            .build())
        .addField(new FieldSpec.Builder()
            .type(ParameterizedTypeName.create(ClassName.fromClass(List.class),
                WildcardName.createWithLowerBound(ClassName.fromClass(String.class))))
            .name("superString")
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
    ParameterizedTypeName thingThangOfFooBar
        = ParameterizedTypeName.create(thingThang, foo, bar);
    ClassName thung = ClassName.create(tacosPackage, "Thung");
    ClassName simpleThung = ClassName.create(tacosPackage, "SimpleThung");
    ParameterizedTypeName thungOfSuperBar
        = ParameterizedTypeName.create(thung, WildcardName.createWithLowerBound(bar));
    ParameterizedTypeName thungOfSuperFoo
        = ParameterizedTypeName.create(thung, WildcardName.createWithLowerBound(foo));
    ParameterizedTypeName simpleThungOfBar = ParameterizedTypeName.create(simpleThung, bar);

    ParameterSpec thungParameter = new ParameterSpec.Builder()
        .addModifiers(Modifier.FINAL)
        .type(thungOfSuperFoo)
        .name("thung")
        .build();
    TypeSpec aSimpleThung = new TypeSpec.Builder()
        .superclass(simpleThungOfBar)
        .anonymousTypeArguments("$N", thungParameter)
        .addMethod(new MethodSpec.Builder()
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .name("doSomething")
            .addParameter(bar, "bar")
            .addCode("/* code snippets */\n")
            .build())
        .build();
    TypeSpec aThingThang = new TypeSpec.Builder()
        .superclass(thingThangOfFooBar)
        .anonymousTypeArguments("")
        .addMethod(new MethodSpec.Builder()
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(thungOfSuperBar)
            .name("call")
            .addParameter(thungParameter)
            .addCode("return $L;\n", aSimpleThung)
            .build())
        .build();
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addField(new FieldSpec.Builder()
            .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.FINAL)
            .type(thingThangOfFooBar)
            .name("NAME")
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
    TypeSpec service = new TypeSpec.Builder()
        .name("Foo")
        .addMethod(new MethodSpec.Builder()
            .addModifiers(Modifier.PUBLIC)
            .constructor()
            .addParameter(new ParameterSpec.Builder()
                .type(long.class)
                .name("id")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
                .type(String.class)
                .name("one")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
                .type(String.class)
                .name("two")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(new AnnotationSpec.Builder()
                    .type(ClassName.create(tacosPackage, "Pong"))
                    .addMember("value", "$S", "pong")
                    .build())
                .type(String.class)
                .name("three")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create(tacosPackage, "Ping"))
                .type(String.class)
                .name("four")
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
    TypeSpec service = new TypeSpec.Builder()
        .name("Service")
        .type(TypeSpec.Type.INTERFACE)
        .addMethod(new MethodSpec.Builder()
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(new AnnotationSpec.Builder()
                .type(headers)
                .addMember("value", "{\n$S,\n$S\n}",
                    "Accept: application/json", "User-Agent: foobar")
                .build())
            .addAnnotation(new AnnotationSpec.Builder()
                .type(post)
                .addMember("value", "$S", "/foo/bar")
                .build())
            .returns(ParameterizedTypeName.create(observable, fooBar))
            .name("fooBar")
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(body)
                .type(ParameterizedTypeName.create(things, thing))
                .name("things")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(new AnnotationSpec.Builder()
                    .type(queryMap)
                    .addMember("encodeValues", "false")
                    .build())
                .type(ParameterizedTypeName.create(map, string, string))
                .name("query")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(new AnnotationSpec.Builder()
                    .type(header)
                    .addMember("value", "$S", "Authorization")
                    .build())
                .type(string)
                .name("authorization")
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
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addField(new FieldSpec.Builder()
            .addAnnotation(new AnnotationSpec.Builder()
                .type(ClassName.create(tacosPackage, "JsonAdapter"))
                .addMember("value", "$T.class", ClassName.create(tacosPackage, "Foo"))
                .build())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(String.class)
            .name("thing")
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
    TypeSpec taco = new TypeSpec.Builder()
        .addAnnotation(new AnnotationSpec.Builder()
            .type(ClassName.create(tacosPackage, "Something"))
            .addMember("hi", "$T.$N", someType, "FIELD")
            .addMember("hey", "$L", 12)
            .addMember("hello", "$S", "goodbye")
            .build())
        .name("Foo")
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
      TypeSpec roshambo = new TypeSpec.Builder()
        .type(TypeSpec.Type.ENUM)
        .name("Roshambo")
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("ROCK")
        .addEnumConstant("PAPER", new TypeSpec.Builder()
            .anonymousTypeArguments("$S", "flat")
            .addMethod(new MethodSpec.Builder()
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .name("toString")
                .addCode("return $S;\n", "paper airplane!")
                .build())
            .build())
        .addEnumConstant("SCISSORS", new TypeSpec.Builder()
            .anonymousTypeArguments("$S", "peace sign")
            .build())
        .addField(new FieldSpec.Builder()
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(String.class)
            .name("handPosition")
            .build())
        .addMethod(new MethodSpec.Builder()
            .constructor()
            .addParameter(String.class, "handPosition")
            .addCode("this.handPosition = handPosition;\n")
            .build())
        .addMethod(new MethodSpec.Builder()
            .constructor()
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
      new TypeSpec.Builder()
        .type(TypeSpec.Type.ENUM)
        .name("Roshambo")
        .build();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void onlyEnumsMayHaveEnumConstants() throws Exception {
    try {
      new TypeSpec.Builder()
        .type(TypeSpec.Type.CLASS)
        .name("Roshambo")
        .addEnumConstant("ROCK")
        .build();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void enumWithMembersButNoConstructorCall() throws Exception {
    TypeSpec roshambo = new TypeSpec.Builder()
        .type(TypeSpec.Type.ENUM)
        .name("Roshambo")
        .addEnumConstant("SPOCK", new TypeSpec.Builder()
            .anonymousTypeArguments()
            .addMethod(new MethodSpec.Builder()
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .name("toString")
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
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addMethod(new MethodSpec.Builder()
            .name("throwOne")
            .addException(IOException.class)
            .build())
        .addMethod(new MethodSpec.Builder()
            .name("throwTwo")
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
    TypeVariableName t = TypeVariableName.create("T");
    TypeVariableName p = TypeVariableName.create("P", ClassName.fromClass(Number.class));
    ClassName location = ClassName.create(tacosPackage, "Location");
    TypeSpec typeSpec = new TypeSpec.Builder()
        .name("Location")
        .addTypeVariable(t)
        .addTypeVariable(p)
        .addSuperinterface(ParameterizedTypeName.create(ClassName.fromClass(Comparable.class), p))
        .addField(new FieldSpec.Builder()
            .type(t)
            .name("label")
            .build())
        .addField(new FieldSpec.Builder()
            .type(p)
            .name("x")
            .build())
        .addField(new FieldSpec.Builder()
            .type(p)
            .name("y")
            .build())
        .addMethod(new MethodSpec.Builder()
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .name("compareTo")
            .addParameter(p, "p")
            .addCode("return 0;\n")
            .build())
        .addMethod(new MethodSpec.Builder()
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(t)
            .addTypeVariable(p)
            .returns(ParameterizedTypeName.create(location, t, p))
            .name("of")
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
    TypeSpec typeSpec = new TypeSpec.Builder()
        .name("Taco")
        .addModifiers(Modifier.ABSTRACT)
        .superclass(ParameterizedTypeName.create(AbstractSet.class, food))
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.create(Comparable.class, taco))
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
    TypeSpec typeSpec = new TypeSpec.Builder()
        .type(TypeSpec.Type.ENUM)
        .name("Food")
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
    TypeSpec typeSpec = new TypeSpec.Builder()
        .type(TypeSpec.Type.INTERFACE)
        .name("Taco")
        .addSuperinterface(Serializable.class)
        .addSuperinterface(ParameterizedTypeName.create(Comparable.class, taco))
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
    TypeSpec typeSpec = new TypeSpec.Builder()
        .name("Combo")
        .addField(FieldSpec.of(taco, "taco"))
        .addField(FieldSpec.of(chips, "chips"))
        .addType(new TypeSpec.Builder()
            .addModifiers(Modifier.STATIC)
            .name(taco.simpleName())
            .addField(FieldSpec.of(ParameterizedTypeName.create(List.class, topping), "toppings"))
            .addField(FieldSpec.of(sauce, "sauce"))
            .addType(new TypeSpec.Builder()
                .type(TypeSpec.Type.ENUM)
                .name(topping.simpleName())
                .addEnumConstant("SHREDDED_CHEESE")
                .addEnumConstant("LEAN_GROUND_BEEF")
                .build())
            .build())
        .addType(new TypeSpec.Builder()
            .addModifiers(Modifier.STATIC)
            .name(chips.simpleName())
            .addField(FieldSpec.of(topping, "topping"))
            .addField(FieldSpec.of(sauce, "dippingSauce"))
            .build())
        .addType(new TypeSpec.Builder()
            .type(TypeSpec.Type.ENUM)
            .name(sauce.simpleName())
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
    TypeSpec top = new TypeSpec.Builder()
        .name("Top")
        .addField(internalTop)
        .addField(internalBottom)
        .addField(externalTop)
        .addField(externalBottom)
        .addType(new TypeSpec.Builder()
            .name("Middle")
            .addField(internalTop)
            .addField(internalBottom)
            .addField(externalTop)
            .addField(externalBottom)
            .addType(new TypeSpec.Builder()
                .name("Bottom")
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
    TypeSpec outer = new TypeSpec.Builder()
        .name("Outer")
        .addOriginatingElement(outerElement)
        .addType(new TypeSpec.Builder()
            .name("Inner")
            .addOriginatingElement(innerElement)
            .build())
        .build();
    assertThat(outer.originatingElements).containsExactly(outerElement, innerElement);
  }

  @Test public void intersectionType() {
    TypeVariableName typeVariable = TypeVariableName.create("T", IntersectionTypeName.create(
        ClassName.fromClass(Comparator.class), ClassName.fromClass(Serializable.class)));
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
        .addMethod(new MethodSpec.Builder()
            .addTypeVariable(typeVariable)
            .returns(typeVariable)
            .name("getComparator")
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
    TypeSpec taco = new TypeSpec.Builder()
        .name("Taco")
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
