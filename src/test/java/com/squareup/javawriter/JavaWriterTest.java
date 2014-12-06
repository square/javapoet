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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class JavaWriterTest {
  private final JavaWriter javaWriter = JavaWriter.create();

  // Used for testing java.io File behavior.
  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  // Used for testing java.nio.file Path behavior.
  private final FileSystem fs = Jimfs.newFileSystem();
  private final Path fsRoot = Iterables.getOnlyElement(fs.getRootDirectories());

  @Test public void pathNotDirectory() throws IOException {
    Path path = fs.getPath("/foo/bar");
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    try {
      javaWriter.writeTo(path);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path /foo/bar exists but is not a directory.");
    }
  }

  @Test public void fileNotDirectory() throws IOException {
    File file = new File(tmp.newFolder("foo"), "bar");
    try {
      javaWriter.writeTo(file);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("File .*?/foo/bar exists but is not a directory.");
    }
  }

  @Test public void pathDefaultPackage() throws IOException {
    ClassName name = ClassName.create("", "Test");
    ClassWriter test = ClassWriter.forClassName(name);
    javaWriter.addTypeWriter(test).writeTo(fsRoot);

    Path testPath = fsRoot.resolve("Test.java");
    assertThat(Files.exists(testPath)).isTrue();
  }

  @Test public void fileDefaultPackage() throws IOException {
    ClassName name = ClassName.create("", "Test");
    ClassWriter test = ClassWriter.forClassName(name);
    javaWriter.addTypeWriter(test).writeTo(tmp.getRoot());

    File testFile = new File(tmp.getRoot(), "Test.java");
    assertThat(testFile.exists()).isTrue();
  }

  @Test public void pathSamePackage() throws IOException {
    ClassName name1 = ClassName.create("example", "Test1");
    ClassName name2 = ClassName.create("example", "Test2");
    ClassWriter test1 = ClassWriter.forClassName(name1);
    ClassWriter test2 = ClassWriter.forClassName(name2);
    javaWriter.addTypeWriter(test1).addTypeWriter(test2).writeTo(fsRoot);

    Path testPath1 = fsRoot.resolve("example/Test1.java");
    assertThat(Files.exists(testPath1)).isTrue();
    Path testPath2 = fsRoot.resolve("example/Test2.java");
    assertThat(Files.exists(testPath2)).isTrue();
  }

  @Test public void fileSamePackage() throws IOException {
    ClassName name1 = ClassName.create("example", "Test1");
    ClassName name2 = ClassName.create("example", "Test2");
    ClassWriter test1 = ClassWriter.forClassName(name1);
    ClassWriter test2 = ClassWriter.forClassName(name2);
    javaWriter.addTypeWriter(test1).addTypeWriter(test2).writeTo(tmp.getRoot());

    File examplePackage = new File(tmp.getRoot(), "example");
    File testFile1 = new File(examplePackage, "Test1.java");
    assertThat(testFile1.exists()).isTrue();
    File testFile2 = new File(examplePackage, "Test2.java");
    assertThat(testFile2.exists()).isTrue();
  }

  @Test public void pathNestedClasses() throws IOException {
    ClassName fooName = ClassName.create("foo", "Test");
    ClassName barName = ClassName.create("foo.bar", "Test");
    ClassName bazName = ClassName.create("foo.bar.baz", "Test");
    ClassWriter foo = ClassWriter.forClassName(fooName);
    ClassWriter bar = ClassWriter.forClassName(barName);
    ClassWriter baz = ClassWriter.forClassName(bazName);
    javaWriter.addTypeWriters(ImmutableList.of(foo, bar, baz)).writeTo(fsRoot);

    Path fooPath = fsRoot.resolve(fs.getPath("foo", "Test.java"));
    Path barPath = fsRoot.resolve(fs.getPath("foo", "bar", "Test.java"));
    Path bazPath = fsRoot.resolve(fs.getPath("foo", "bar", "baz", "Test.java"));
    assertThat(Files.exists(fooPath)).isTrue();
    assertThat(Files.exists(barPath)).isTrue();
    assertThat(Files.exists(bazPath)).isTrue();
  }

  @Test public void fileNestedClasses() throws IOException {
    ClassName fooName = ClassName.create("foo", "Test");
    ClassName barName = ClassName.create("foo.bar", "Test");
    ClassName bazName = ClassName.create("foo.bar.baz", "Test");
    ClassWriter foo = ClassWriter.forClassName(fooName);
    ClassWriter bar = ClassWriter.forClassName(barName);
    ClassWriter baz = ClassWriter.forClassName(bazName);
    javaWriter.addTypeWriters(ImmutableList.of(foo, bar, baz)).writeTo(tmp.getRoot());

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

  // TODO Filer-based tests
}
