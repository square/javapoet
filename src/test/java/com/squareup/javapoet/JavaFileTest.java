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
}
