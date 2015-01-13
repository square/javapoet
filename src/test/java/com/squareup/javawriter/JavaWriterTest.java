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

import com.google.common.collect.Iterables;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.lang.model.element.Element;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class JavaWriterTest {
  private final JavaWriter javaWriter = new JavaWriter();

  // Used for testing java.io File behavior.
  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  // Used for testing java.nio.file Path behavior.
  private final FileSystem fs = Jimfs.newFileSystem();
  private final Path fsRoot = Iterables.getOnlyElement(fs.getRootDirectories());

  // Used for testing annotation processor Filer behavior.
  private final TestFiler filer = new TestFiler(fs, fsRoot);

  @Test public void pathNotDirectory() throws IOException {
    Path path = fs.getPath("/foo/bar");
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    try {
      javaWriter.writeTo(path);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("path /foo/bar exists but is not a directory.");
    }
  }

  @Test public void fileNotDirectory() throws IOException {
    File file = new File(tmp.newFolder("foo"), "bar");
    file.createNewFile();
    try {
      javaWriter.writeTo(file);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("path .*?/foo/bar exists but is not a directory.");
    }
  }

  @Test public void pathDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    javaWriter.add("", type).writeTo(fsRoot);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  @Test public void fileDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    javaWriter.add("", type).writeTo(tmp.getRoot());

    File testFile = new File(tmp.getRoot(), "Test.java");
    assertThat(testFile.exists()).isTrue();
  }

  @Test public void filerDefaultPackage() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    javaWriter.add("", type).writeTo(filer);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  @Test public void pathSamePackage() throws IOException {
    TypeSpec test1 = TypeSpec.classBuilder("Test1").build();
    TypeSpec test2 = TypeSpec.classBuilder("Test2").build();
    javaWriter.add("example", test1).add("example", test2).writeTo(fsRoot);

    Path testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.java"));
    assertThat(Files.exists(testPath1)).isTrue();
    Path testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.java"));
    assertThat(Files.exists(testPath2)).isTrue();
  }

  @Test public void fileSamePackage() throws IOException {
    TypeSpec test1 = TypeSpec.classBuilder("Test1").build();
    TypeSpec test2 = TypeSpec.classBuilder("Test2").build();
    javaWriter.add("example", test1).add("example", test2).writeTo(tmp.getRoot());

    File examplePackage = new File(tmp.getRoot(), "example");
    File testFile1 = new File(examplePackage, "Test1.java");
    assertThat(testFile1.exists()).isTrue();
    File testFile2 = new File(examplePackage, "Test2.java");
    assertThat(testFile2.exists()).isTrue();
  }

  @Test public void filerSamePackage() throws IOException {
    TypeSpec test1 = TypeSpec.classBuilder("Test1").build();
    TypeSpec test2 = TypeSpec.classBuilder("Test2").build();
    javaWriter.add("example", test1).add("example", test2).writeTo(filer);

    Path testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.java"));
    assertThat(Files.exists(testPath1)).isTrue();
    Path testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.java"));
    assertThat(Files.exists(testPath2)).isTrue();
  }

  @Test public void pathNestedClasses() throws IOException {
    TypeSpec test = TypeSpec.classBuilder("Test").build();
    javaWriter.add("foo", test)
        .add("foo.bar", test)
        .add("foo.bar.baz", test)
        .writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  @Test public void fileNestedClasses() throws IOException {
    TypeSpec test = TypeSpec.classBuilder("Test").build();
    javaWriter.add("foo", test)
        .add("foo.bar", test)
        .add("foo.bar.baz", test)
        .writeTo(tmp.getRoot());

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
    TypeSpec test = TypeSpec.classBuilder("Test").build();
    javaWriter.add("foo", test)
        .add("foo.bar", test)
        .add("foo.bar.baz", test)
        .writeTo(filer);

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

    javaWriter.add("example", test1).add("example", test2).writeTo(filer);

    Path testPath1 = fsRoot.resolve(fs.getPath("example", "Test1.java"));
    assertThat(filer.getOriginatingElements(testPath1)).containsExactly(element1_1);
    Path testPath2 = fsRoot.resolve(fs.getPath("example", "Test2.java"));
    assertThat(filer.getOriginatingElements(testPath2)).containsExactly(element2_1, element2_2);
  }
}
