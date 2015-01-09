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
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.WildcardName;
import java.io.Serializable;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TypeSpecTest {
  @Test public void basic() throws Exception {
    TypeSpec taco = new TypeSpec.Builder()
        .name(ClassName.create("com.squareup.tacos", "Taco"))
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
        .name(ClassName.create("com.squareup.tacos", "Taco"))
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
    ClassName foo = ClassName.create("com.squareup.tacos", "Foo");
    ClassName bar = ClassName.create("com.squareup.tacos", "Bar");
    ClassName thingThang = ClassName.create(
        "com.squareup.tacos", ImmutableList.of("Thing"), "Thang");
    ParameterizedTypeName thingThangOfFooBar
        = ParameterizedTypeName.create(thingThang, foo, bar);
    ClassName thung = ClassName.create("com.squareup.tacos", "Thung");
    ClassName simpleThung = ClassName.create("com.squareup.tacos", "SimpleThung");
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
        .supertype(simpleThungOfBar)
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
        .supertype(thingThangOfFooBar)
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
        .name(ClassName.create("com.squareup.tacos", "Taco"))
        .addField(new FieldSpec.Builder()
            .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.FINAL)
            .type(thingThangOfFooBar)
            .name("NAME")
            .initializer("$L", aThingThang)
            .build())
        .build();

    // TODO: import Thing, and change references from "Thang" to "Thing.Thang"
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.tacos.Bar;\n"
        + "import com.squareup.tacos.Foo;\n"
        + "import com.squareup.tacos.SimpleThung;\n"
        + "import com.squareup.tacos.Thing.Thang;\n"
        + "import com.squareup.tacos.Thung;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "class Taco {\n"
        + "  static final Thang<Foo, Bar> NAME = new Thang<Foo, Bar>() {\n"
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
        .name(ClassName.create("com.squareup.tacos", "Foo"))
        .addMethod(new MethodSpec.Builder()
            .addModifiers(Modifier.PUBLIC)
            .constructor()
            .addParameter(new ParameterSpec.Builder()
                .type(long.class)
                .name("id")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create("com.squareup.tacos", "Ping"))
                .type(String.class)
                .name("one")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create("com.squareup.tacos", "Ping"))
                .type(String.class)
                .name("two")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(new AnnotationSpec.Builder()
                    .type(ClassName.create("com.squareup.tacos", "Pong"))
                    .addMember("value", "$S", "pong")
                    .build())
                .type(String.class)
                .name("three")
                .build())
            .addParameter(new ParameterSpec.Builder()
                .addAnnotation(ClassName.create("com.squareup.tacos", "Ping"))
                .type(String.class)
                .name("four")
                .build())
            .addCode("/* code snippets */\n")
            .build())
        .build();

    assertThat(toString(service)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.tacos.Ping;\n"
        + "import com.squareup.tacos.Pong;\n"
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
    ClassName observable = ClassName.create("com.squareup.tacos", "Observable");
    ClassName fooBar = ClassName.create("com.squareup.tacos", "FooBar");
    ClassName thing = ClassName.create("com.squareup.tacos", "Thing");
    ClassName things = ClassName.create("com.squareup.tacos", "Things");
    ClassName map = ClassName.create("java.util", "Map");
    ClassName string = ClassName.create("java.lang", "String");
    ClassName headers = ClassName.create("com.squareup.tacos", "Headers");
    ClassName post = ClassName.create("com.squareup.tacos", "POST");
    ClassName body = ClassName.create("com.squareup.tacos", "Body");
    ClassName queryMap = ClassName.create("com.squareup.tacos", "QueryMap");
    ClassName header = ClassName.create("com.squareup.tacos", "Header");
    TypeSpec service = new TypeSpec.Builder()
        .name(ClassName.create("com.squareup.tacos", "Service"))
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
        + "import com.squareup.tacos.Body;\n"
        + "import com.squareup.tacos.FooBar;\n"
        + "import com.squareup.tacos.Header;\n"
        + "import com.squareup.tacos.Headers;\n"
        + "import com.squareup.tacos.Observable;\n"
        + "import com.squareup.tacos.POST;\n"
        + "import com.squareup.tacos.QueryMap;\n"
        + "import com.squareup.tacos.Thing;\n"
        + "import com.squareup.tacos.Things;\n"
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
        .name(ClassName.create("com.squareup.tacos", "Taco"))
        .addField(new FieldSpec.Builder()
            .addAnnotation(new AnnotationSpec.Builder()
                .type(ClassName.create("com.squareup.tacos", "JsonAdapter"))
                .addMember("value", "$T.class", ClassName.create("com.squareup.tacos", "Foo"))
                .build())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(String.class)
            .name("thing")
            .build())
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.tacos.Foo;\n"
        + "import com.squareup.tacos.JsonAdapter;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "class Taco {\n"
        + "  @JsonAdapter(Foo.class)\n"
        + "  private final String thing;\n"
        + "}\n");
  }

  @Test public void annotatedClass() throws Exception {
    ClassName someType = ClassName.create("com.squareup.tacos", "SomeType");
    TypeSpec taco = new TypeSpec.Builder()
        .addAnnotation(new AnnotationSpec.Builder()
            .type(ClassName.create("com.squareup.tacos", "Something"))
            .addMember("hi", "$T.$N", someType, "FIELD")
            .addMember("hey", "$L", 12)
            .addMember("hello", "$S", "goodbye")
            .build())
        .name(ClassName.create("com.squareup.tacos", "Foo"))
        .addModifiers(Modifier.PUBLIC)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.tacos.SomeType;\n"
        + "import com.squareup.tacos.Something;\n"
        + "\n"
        + "@Something(\n"
        + "    hello = \"goodbye\",\n"
        + "    hey = 12,\n"
        + "    hi = SomeType.FIELD\n"
        + ")\n"
        + "public class Foo {\n"
        + "}\n");
  }

  private String toString(TypeSpec typeSpec) {
    return new JavaFile.Builder()
        .classSpec(typeSpec)
        .build()
        .toString();
  }
}
