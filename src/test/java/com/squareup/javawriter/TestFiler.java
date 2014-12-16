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
import com.sun.tools.javac.nio.JavacPathFileManager;
import com.sun.tools.javac.nio.PathFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;

final class TestFiler implements Filer {
  private final String separator;
  private final Path fileSystemRoot;
  private final PathFileManager fileManager;

  public TestFiler(FileSystem fileSystem) {
    separator = fileSystem.getSeparator();
    fileSystemRoot = Iterables.get(fileSystem.getRootDirectories(), 0);
    fileManager = new JavacPathFileManager(new Context(), false, UTF_8);
    fileManager.setDefaultFileSystem(fileSystem);
  }

  public Path getLocationPath(JavaFileManager.Location location) {
    Iterable<? extends Path> locationPaths = fileManager.getLocation(location);
    if (locationPaths == null || Iterables.isEmpty(locationPaths)) {
      Path locationPath = fileSystemRoot.resolve(location.getName());
      locationPaths = ImmutableList.of(locationPath);
      try {
        Files.createDirectories(locationPath);
        fileManager.setLocation(location, locationPaths);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Iterables.getOnlyElement(locationPaths);
  }

  private JavaFileObject getJavaFileObject(String fqcn, JavaFileManager.Location location) {
    String javaPath = fqcn.replace(".", separator) + ".java"; // Not robust, assumes well-formed.
    Path finalPath = getLocationPath(location).resolve(javaPath);
    return Iterables.getOnlyElement(fileManager.getJavaFileObjects(finalPath));
  }

  @Override
  public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
      throws IOException {
    return getJavaFileObject(name.toString(), SOURCE_OUTPUT);
  }

  @Override public JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
      throws IOException {
    return getJavaFileObject(name.toString(), CLASS_OUTPUT);
  }

  @Override public FileObject createResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName, Element... originatingElements) throws IOException {
    return getJavaFileObject(pkg + "." + relativeName, location);
  }

  @Override public FileObject getResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName) throws IOException {
    return getJavaFileObject(pkg + "." + relativeName, location);
  }
}
