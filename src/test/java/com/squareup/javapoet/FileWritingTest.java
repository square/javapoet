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
package com.squareup.javapoet;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class FileWritingTest {
  // Used for testing java.io File behavior.
  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  // Used for testing java.nio.file Path behavior.
  private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
  private final Path fsRoot = fs.getRootDirectories().iterator().next();

  // Used for testing annotation processor Filer behavior.
  private final TestFiler filer = new TestFiler(fs, fsRoot);

  @Test public void pathNotDirectory() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile javaFile = JavaFile.builder("example", type).build();
    Path path = fs.getPath("/foo/bar");
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    try {
      javaFile.writeTo(path);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("path /foo/bar exists but is not a directory.");
    }
  }

  @Test public void fileNotDirectory() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile javaFile = JavaFile.builder("example", type).build();
    File file = new File(tmp.newFolder("foo"), "bar");
    file.createNewFile();
    try {
      javaFile.writeTo(file);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo(
          "path " + file.getPath() + " exists but is not a directory.");
    }
  }

  @Test public void pathDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(fsRoot);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  @Test public void fileDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(tmp.getRoot());

    File testFile = new File(tmp.getRoot(), "Test.java");
    assertThat(testFile.exists()).isTrue();
  }

  @Test public void filerDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("", type).build().writeTo(filer);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  @Test public void pathNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(fsRoot);
    JavaFile.builder("foo.bar", type).build().writeTo(fsRoot);
    JavaFile.builder("foo.bar.baz", type).build().writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  @Test public void fileNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(tmp.getRoot());
    JavaFile.builder("foo.bar", type).build().writeTo(tmp.getRoot());
    JavaFile.builder("foo.bar.baz", type).build().writeTo(tmp.getRoot());

    File fooDir = new File(tmp.getRoot(), "foo");
    File fooFile = new File(fooDir, "Test.java");
    File barDir = new File(fooDir, "bar");
    File barFile = new File(barDir, "Test.java");
    File bazDir = new File(barDir, "baz");
    File bazFile = new File(bazDir, "Test.java");
    assertThat(fooFile.exists()).isTrue();
    assertThat(barFile.exists()).isTrue();
    assertThat(bazFile.exists()).isTrue();
  }

  @Test public void filerNestedClasses() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    JavaFile.builder("foo", type).build().writeTo(filer);
    JavaFile.builder("foo.bar", type).build().writeTo(filer);
    JavaFile.builder("foo.bar.baz", type).build().writeTo(filer);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  @Test public void filerPassesOriginatingElements() throws IOException {
    Element element1_1 = Mockito.mock(Element.class);
    TypeSpec test1 = TypeSpec.classBuilder("Test1")
        .addOriginatingElement(element1_1)
        .build();

    Element element2_1 = Mockito.mock(Element.class);
    Element element2_2 = Mockito.mock(Element.class);
    TypeSpec test2 = TypeSpec.classBuilder("Test2")
        .addOriginatingElement(element2_1)
        .addOriginatingElement(element2_2)
        .build();

    JavaFile.builder("example", test1).build().writeTo(filer);
    JavaFile.builder("example", test2).build().writeTo(filer);

    Path testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.java"));
    assertThat(filer.getOriginatingElements(testPath1)).containsExactly(element1_1);
    Path testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.java"));
    assertThat(filer.getOriginatingElements(testPath2)).containsExactly(element2_1, element2_2);
  }

  @Test public void filerClassesWithTabIndent() throws IOException {
    TypeSpec test = TypeSpec.classBuilder("Test")
        .addField(Date.class, "madeFreshDate")
        .addMethod(MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String[].class, "args")
            .addCode("$T.out.println($S);\n", System.class, "Hello World!")
            .build())
        .build();
    JavaFile.builder("foo", test).indent("\t").build().writeTo(filer);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    String source = new String(Files.readAllBytes(fooPath));

    assertThat(source).isEqualTo(""
        + "package foo;\n"
        + "\n"
        + "import java.lang.String;\n"
        + "import java.lang.System;\n"
        + "import java.util.Date;\n"
        + "\n"
        + "class Test {\n"
        + "\tDate madeFreshDate;\n"
        + "\n"
        + "\tpublic static void main(String[] args) {\n"
        + "\t\tSystem.out.println(\"Hello World!\");\n"
        + "\t}\n"
        + "}\n");
  }

  /**
   * This test confirms that JavaPoet ignores the host charset and always uses UTF-8. The host
   * charset is customized with {@code -Dfile.encoding=ISO-8859-1}.
   */
  @Test public void fileIsUtf8() throws IOException {
    JavaFile javaFile = JavaFile.builder("foo", TypeSpec.classBuilder("Taco").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build();
    javaFile.writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Taco.java"));
    assertThat(new String(Files.readAllBytes(fooPath), UTF_8)).isEqualTo(""
        + "// Pi\u00f1ata\u00a1\n"
        + "package foo;\n"
        + "\n"
        + "class Taco {\n"
        + "}\n");
  }
}
