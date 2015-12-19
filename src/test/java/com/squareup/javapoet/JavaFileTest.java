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

import java.util.Date;
import javax.lang.model.element.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class JavaFileTest {
  @Test public void noImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void singleImport() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "}\n");
  }

  @Test public void conflictingImports() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(Date.class, "madeFreshDate")
            .addField(java.sql.Date.class, "madeFreshDatabaseDate")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Taco {\n"
        + "  Date madeFreshDate;\n"
        + "\n"
        + "  java.sql.Date madeFreshDatabaseDate;\n"
        + "}\n");
  }

  @Test public void skipJavaLangImportsWithConflictingClassLast() throws Exception {
    // Whatever is used first wins! In this case the Float in java.lang is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float litres;\n"
        + "\n"
        + "  com.squareup.soda.Float beverage;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  @Test public void skipJavaLangImportsWithConflictingClassFirst() throws Exception {
    // Whatever is used first wins! In this case the Float in com.squareup.soda is imported.
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .addField(ClassName.get("java.lang", "Float"), "litres")
            .build())
        .skipJavaLangImports(true)
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.soda.Float;\n"
        + "\n"
        + "class Taco {\n"
        + "  Float beverage;\n"
        + "\n"
        + "  java.lang.Float litres;\n" // Second 'Float' is fully qualified.
        + "}\n");
  }

  @Test public void conflictingParentName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class Twin {\n"
        + "    }\n"
        + "\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void conflictingChildName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Twin").build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "\n"
        + "      class Twin {\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void conflictingNameOutOfScope() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addField(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Nested")
                        .addType(TypeSpec.classBuilder("Twin").build())
                        .build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class A {\n"
        + "  class B {\n"
        + "    class C {\n"
        + "      A.Twin.D d;\n"
        + "\n"
        + "      class Nested {\n"
        + "        class Twin {\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  class Twin {\n"
        + "    class D {\n"
        + "    }\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void nestedClassAndSuperclassShareName() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName.get("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import com.squareup.wire.Message;\n"
        + "\n"
        + "class Taco extends Message {\n"
        + "  class Builder extends Message.Builder {\n"
        + "  }\n"
        + "}\n");
  }

  /** https://github.com/square/javapoet/issues/366 */
  @Test public void annotationIsNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName.get("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName.get("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import dagger.Component;\n"
        + "\n"
        + "@Component\n"
        + "class TestComponent {\n"
        + "  @Component.Builder\n"
        + "  class Builder {\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultPackage() throws Exception {
    String source = JavaFile.builder("",
        TypeSpec.classBuilder("HelloWorld")
            .addMethod(MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .addCode("$T.out.println($S);\n", System.class, "Hello World!")
                .build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "import java.lang.String;\n"
        + "import java.lang.System;\n"
        + "\n"
        + "class HelloWorld {\n"
        + "  public static void main(String[] args) {\n"
        + "    System.out.println(\"Hello World!\");\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void defaultPackageTypesAreNotImported() throws Exception {
    String source = JavaFile.builder("hello",
          TypeSpec.classBuilder("World").addSuperinterface(ClassName.get("", "Test")).build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package hello;\n"
        + "\n"
        + "class World implements Test {\n"
        + "}\n");
  }

  @Test public void topOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated $L by JavaPoet. DO NOT EDIT!", "2015-01-13")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "// Generated 2015-01-13 by JavaPoet. DO NOT EDIT!\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void emptyLinesInTopOfFileComment() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "//\n"
        + "// GENERATED FILE:\n"
        + "//\n"
        + "// DO NOT EDIT!\n"
        + "//\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void packageClassConflictsWithNestedClass() throws Exception {
    String source = JavaFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addField(ClassName.get("com.squareup.tacos", "A"), "a")
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "  com.squareup.tacos.A a;\n"
        + "\n"
        + "  class A {\n"
        + "  }\n"
        + "}\n");
  }
}
