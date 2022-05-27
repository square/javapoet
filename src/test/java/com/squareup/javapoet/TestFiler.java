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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

final class TestFiler implements Filer {
  class Source extends SimpleJavaFileObject {
    private final Path path;
    protected Source(Path path) {
      super(path.toUri(), Kind.SOURCE);
      this.path = path;
    }
    @Override public OutputStream openOutputStream() throws IOException {
      Path parent = path.getParent();
      if (!Files.exists(parent)) fileSystemProvider.createDirectory(parent);
      return fileSystemProvider.newOutputStream(path);
    }
  }

  private final String separator;
  private final Path fileSystemRoot;
  private final FileSystemProvider fileSystemProvider;
  private final Map<Path, Set<Element>> originatingElementsMap;

  public TestFiler(FileSystem fileSystem, Path fsRoot) {
    separator = fileSystem.getSeparator();
    fileSystemRoot = fsRoot;
    fileSystemProvider = fileSystem.provider();
    originatingElementsMap = new LinkedHashMap<>();
  }

  public Set<Element> getOriginatingElements(Path path) {
    return originatingElementsMap.get(path);
  }

  @Override public JavaFileObject createSourceFile(
      CharSequence name, Element... originatingElements) throws IOException {
    String relative = name.toString().replace(".", separator) + ".java"; // Assumes well-formed.
    Path path = fileSystemRoot.resolve(relative);
    originatingElementsMap.put(path, Util.immutableSet(Arrays.asList(originatingElements)));
    return new Source(path);
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
