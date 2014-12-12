/*
 * Copyright (C) 2014 Square, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static javax.lang.model.element.Modifier.PUBLIC;

@RunWith(JUnit4.class) public class AnonymousClassWriterTest {
  private final ClassName className = ClassName.create("com.foo", "Bar");
  private final ClassName threadName = ClassName.fromClass(Thread.class);

  @Test public void definedAsField() throws IOException {
    ClassWriter classWriter = ClassWriter.forClassName(className);
    FieldWriter threadField = classWriter.addField(threadName, "thread");

    AnonymousClassWriter threadWriter = AnonymousClassWriter.forClassName(threadName);
    MethodWriter threadStartMethod = threadWriter.addMethod(void.class, "start");
    threadStartMethod.annotate(Override.class);
    threadStartMethod.addModifiers(PUBLIC);
    threadStartMethod.body().addSnippet("System.out.println(\"Hello World!\");");

    threadField.setInitializer("%s", threadWriter);

    assertThat(classWriter.toString()).isEqualTo(""
        + "package com.foo;\n"
        + "\n"
        + "class Bar {\n"
        + "  Thread thread = new Thread() {\n"
        + "    @Override\n"
        + "    public void start() {\n"
        + "      System.out.println(\"Hello World!\");\n"
        + "    }\n"
        + "  };\n"
        + "}\n");
  }

  @Test public void withConstructorParameters() throws IOException {
    ClassWriter classWriter = ClassWriter.forClassName(className);
    classWriter.addField(threadName, "thread");
    ConstructorWriter constructorWriter = classWriter.addConstructor();
    constructorWriter.addParameter(Runnable.class, "runnable");

    AnonymousClassWriter threadWriter = AnonymousClassWriter.forClassName(threadName);
    threadWriter.setConstructorArguments("runnable");
    MethodWriter threadStartMethod = threadWriter.addMethod(void.class, "start");
    threadStartMethod.annotate(Override.class);
    threadStartMethod.addModifiers(PUBLIC);
    threadStartMethod.body().addSnippet("System.out.println(\"Hello World!\");");

    constructorWriter.body().addSnippet("thread = %s;", threadWriter);

    assertThat(classWriter.toString()).isEqualTo(""
        + "package com.foo;\n"
        + "\n"
        + "class Bar {\n"
        + "  Thread thread;\n"
        + "\n"
        + "  Bar(Runnable runnable) {\n"
        + "    thread = new Thread(runnable) {\n"
        + "      @Override\n"
        + "      public void start() {\n"
        + "        System.out.println(\"Hello World!\");\n"
        + "      }\n"
        + "    };\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void withImports() throws IOException {
    ClassWriter classWriter = ClassWriter.forClassName(className);
    classWriter.addField(threadName, "thread");
    ConstructorWriter constructorWriter = classWriter.addConstructor();

    AnonymousClassWriter threadWriter = AnonymousClassWriter.forClassName(threadName);
    FieldWriter threadListField = threadWriter.addField(
        ParameterizedTypeName.create(List.class, ClassName.fromClass(String.class)), "list");
    threadListField.setInitializer("%s.asList(\"Hello World\")", ClassName.fromClass(Arrays.class));
    MethodWriter threadStartMethod = threadWriter.addMethod(void.class, "start");
    threadStartMethod.annotate(Override.class);
    threadStartMethod.addModifiers(PUBLIC);
    threadStartMethod.body().addSnippet("System.out.println(list.get(0));");

    constructorWriter.body().addSnippet("thread = %s;", threadWriter);

    assertThat(classWriter.toString()).isEqualTo(""
        + "package com.foo;\n"
        + "\n"
        + "import java.util.Arrays;\n"
        + "import java.util.List;\n"
        + "\n"
        + "class Bar {\n"
        + "  Thread thread;\n"
        + "\n"
        + "  Bar() {\n"
        + "    thread = new Thread() {\n"
        + "      List<String> list = Arrays.asList(\"Hello World\");\n"
        + "\n"
        + "      @Override\n"
        + "      public void start() {\n"
        + "        System.out.println(list.get(0));\n"
        + "      }\n"
        + "    };\n"
        + "  }\n"
        + "}\n");
  }

  @Test public void withType() throws IOException {
    ClassWriter classWriter = ClassWriter.forClassName(className);
    FieldWriter listField =
        classWriter.addField(ParameterizedTypeName.create(List.class, WildcardName.create()),
            "list");

    AnonymousClassWriter listWriter = AnonymousClassWriter.forParameterizedTypeName(
        ParameterizedTypeName.create(ArrayList.class,
            ParameterizedTypeName.create(Map.class, ClassName.fromClass(String.class),
                ClassName.fromClass(Integer.class))));

    MethodWriter listSizeMethod = listWriter.addMethod(int.class, "size");
    listSizeMethod.annotate(Override.class);
    listSizeMethod.addModifiers(PUBLIC);
    listSizeMethod.body().addSnippet("return 1;");

    listField.setInitializer("%s", listWriter);

    assertThat(classWriter.toString()).isEqualTo(""
        + "package com.foo;\n"
        + "\n"
        + "import java.util.ArrayList;\n"
        + "import java.util.List;\n"
        + "import java.util.Map;\n"
        + "\n"
        + "class Bar {\n"
        + "  List<?> list = new ArrayList<Map<String, Integer>>() {\n"
        + "    @Override\n"
        + "    public int size() {\n"
        + "      return 1;\n"
        + "    }\n"
        + "  };\n"
        + "}\n");
  }
}
