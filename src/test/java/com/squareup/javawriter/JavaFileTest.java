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

import java.util.Date;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public final class JavaFileTest {
  @Test public void noImports() throws Exception {
    String source = new JavaFile.Builder()
        .packageName("com.squareup.tacos")
        .typeSpec(new TypeSpec.Builder()
            .name("Taco")
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
        .typeSpec(new TypeSpec.Builder()
            .name("Taco")
            .addField(new FieldSpec.Builder()
                .type(Date.class)
                .name("madeFreshDate")
                .build())
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
        .typeSpec(new TypeSpec.Builder()
            .name("Taco")
            .addField(new FieldSpec.Builder()
                .type(Date.class)
                .name("madeFreshDate")
                .build())
            .addField(new FieldSpec.Builder()
                .type(java.sql.Date.class)
                .name("madeFreshDatabaseDate")
                .build())
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
}
