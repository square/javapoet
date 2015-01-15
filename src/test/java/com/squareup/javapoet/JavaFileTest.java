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

import static com.google.common.truth.Truth.assertThat;

public final class JavaFileTest {
  @Test public void noImports() throws Exception {
    String source = new JavaFile.Builder()
        .packageName("com.squareup.tacos")
        .typeSpec(TypeSpec.classBuilder("Taco")
            .build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void singleImport() throws Exception {
    String source = new JavaFile.Builder()
        .packageName("com.squareup.tacos")
        .typeSpec(TypeSpec.classBuilder("Taco")
            .add(FieldSpec.of(Date.class, "madeFreshDate"))
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
    String source = new JavaFile.Builder()
        .packageName("com.squareup.tacos")
        .typeSpec(TypeSpec.classBuilder("Taco")
            .add(FieldSpec.of(Date.class, "madeFreshDate"))
            .add(FieldSpec.of(java.sql.Date.class, "madeFreshDatabaseDate"))
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

  @Test public void defaultPackage() throws Exception {
    String source = new JavaFile.Builder()
        .typeSpec(TypeSpec.classBuilder("HelloWorld")
            .add(MethodSpec.methodBuilder("main")
                .add(Modifier.PUBLIC, Modifier.STATIC).add(ParameterSpec.of(String[].class, "args"))
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
    String source = new JavaFile.Builder()
        .fileComment("Generated $L by JavaWriter. DO NOT EDIT!", "2015-01-13")
        .packageName("com.squareup.tacos")
        .typeSpec(TypeSpec.classBuilder("Taco").build())
        .build()
        .toString();
    assertThat(source).isEqualTo(""
        + "// Generated 2015-01-13 by JavaWriter. DO NOT EDIT!\n"
        + "package com.squareup.tacos;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void emptyLinesInTopOfFileComment() throws Exception {
    String source = new JavaFile.Builder()
        .fileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .packageName("com.squareup.tacos")
        .typeSpec(TypeSpec.classBuilder("Taco")
            .build())
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
