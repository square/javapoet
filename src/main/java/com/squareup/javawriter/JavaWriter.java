/*
 * Copyright (C) 2014 Google, Inc.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

import static com.google.common.base.Preconditions.checkArgument;

public final class JavaWriter {
  /** Create a new Java writer for writing multiple types to a single location. */
  public static JavaWriter create() {
    return new JavaWriter();
  }

  private final List<TypeWriter> typeWriters;

  private JavaWriter() {
    typeWriters = Lists.newArrayList();
    // TODO take in options! indent, what else?
  }

  public JavaWriter addTypeWriter(TypeWriter typeWriter) {
    typeWriters.add(typeWriter);
    return this;
  }

  public JavaWriter addTypeWriters(Iterable<? extends TypeWriter> typeWriters) {
    Iterables.addAll(this.typeWriters, typeWriters);
    return this;
  }

  public void writeTo(Path directory) throws IOException {
    checkArgument(Files.notExists(directory) || Files.isDirectory(directory),
        "Path %s exists but is not a directory.", directory);
    for (TypeWriter typeWriter : typeWriters) {
      ClassName typeName = typeWriter.name();
      String packageName = typeName.packageName();

      Path outputDirectory = directory;
      if (!packageName.isEmpty()) {
        for (String packageComponent : packageName.split("\\.")) {
          outputDirectory = outputDirectory.resolve(packageComponent);
        }
        Files.createDirectories(outputDirectory);
      }

      Path outputFile = outputDirectory.resolve(typeName.simpleName() + ".java");
      try (Closer closer = Closer.create()) {
        Writer writer = new OutputStreamWriter(Files.newOutputStream(outputFile));
        typeWriter.writeTypeToAppendable(closer.register(writer));
      }
    }
  }

  public void writeTo(File directory) throws IOException {
    checkArgument(!directory.exists() || directory.isDirectory(),
        "File %s exists but is not a directory.", directory);
    for (TypeWriter typeWriter : typeWriters) {
      ClassName typeName = typeWriter.name();
      String packageName = typeName.packageName();

      File outputDir = directory;
      if (!packageName.isEmpty()) {
        for (String packageComponent : packageName.split("\\.")) {
          outputDir = new File(outputDir, packageComponent);
        }
        if (!outputDir.mkdirs()) {
          throw new IOException("Unable to create directory " + outputDir);
        }
      }

      File outputFile = new File(outputDir, typeName.simpleName() + ".java");
      try (Closer closer = Closer.create()) {
        typeWriter.writeTypeToAppendable(closer.register(new FileWriter(outputFile)));
      }
    }
  }

  public void writeTo(Filer filer) throws IOException {
    writeTo(filer, ImmutableSet.<Element>of());
  }

  public void writeTo(Filer filer, Iterable<? extends Element> originatingElements)
      throws IOException {
    // TODO tack originatingElements on TypeWriter? Losing a top-level-only writer for this sucks.
    for (TypeWriter typeWriter : typeWriters) {
      JavaFileObject sourceFile = filer.createSourceFile(typeWriter.name().canonicalName(),
          Iterables.toArray(originatingElements, Element.class));
      Writer closeable = sourceFile.openWriter();
      Closer closer = Closer.create();
      try {
        typeWriter.writeTypeToAppendable(closer.register(closeable));
      } catch (Exception e) {
        try {
          sourceFile.delete();
        } catch (Exception e2) {
          // Couldn't delete the file.
        }
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    }
  }
}
