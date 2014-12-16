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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.sun.tools.javac.nio.JavacPathFileManager;
import com.sun.tools.javac.nio.PathFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import static java.nio.charset.StandardCharsets.UTF_8;

final class TestFiler implements Filer {
  private final String separator;
  private final Path fileSystemRoot;
  private final PathFileManager fileManager;
  private final SetMultimap<Path, Element> originatingElementsMap;

  public TestFiler(FileSystem fileSystem, Path fsRoot) {
    separator = fileSystem.getSeparator();
    fileSystemRoot = fsRoot;
    fileManager = new JavacPathFileManager(new Context(), false, UTF_8);
    fileManager.setDefaultFileSystem(fileSystem);
    originatingElementsMap = LinkedHashMultimap.create();
  }

  public Set<Element> getOriginatingElements(Path path) {
    return originatingElementsMap.get(path);
  }

  @Override
  public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
      throws IOException {
    String relative = name.toString().replace(".", separator) + ".java"; // Not robust, assumes well-formed.
    Path path = fileSystemRoot.resolve(relative);
    originatingElementsMap.putAll(path, Arrays.asList(originatingElements));
    return Iterables.getOnlyElement(fileManager.getJavaFileObjects(path));
  }

  @Override public JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
      throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }

  @Override public FileObject createResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName, Element... originatingElements) throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }

  @Override public FileObject getResource(JavaFileManager.Location location, CharSequence pkg,
      CharSequence relativeName) throws IOException {
    throw new UnsupportedOperationException("Not implemented.");
  }
}
