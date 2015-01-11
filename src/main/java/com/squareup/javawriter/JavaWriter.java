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

import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

import static com.google.common.base.Preconditions.checkArgument;

public final class JavaWriter {
  private final List<JavaFile> javaFiles = new ArrayList<>();

  public JavaWriter add(JavaFile javaFile) {
    javaFiles.add(javaFile);
    return this;
  }

  public JavaWriter add(String packageName, TypeSpec type) {
    return add(new JavaFile.Builder()
        .packageName(packageName)
        .typeSpec(type)
        .build());
  }

  public void writeTo(Path directory) throws IOException {
    checkArgument(Files.notExists(directory) || Files.isDirectory(directory),
        "Path %s exists but is not a directory.", directory);
    for (JavaFile javaFile : javaFiles) {
      String packageName = javaFile.packageName;

      Path outputDirectory = directory;
      if (!packageName.isEmpty()) {
        for (String packageComponent : packageName.split("\\.")) {
          outputDirectory = outputDirectory.resolve(packageComponent);
        }
        Files.createDirectories(outputDirectory);
      }

      Path outputPath = outputDirectory.resolve(javaFile.typeSpec.name + ".java");
      try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputPath))) {
        writer.write(javaFile.toString());
      }
    }
  }

  public void writeTo(File directory) throws IOException {
    writeTo(directory.toPath());
  }

  public void writeTo(Filer filer) throws IOException {
    for (JavaFile javaFile : javaFiles) {
      JavaFileObject filerSourceFile = filer.createSourceFile(
          javaFile.packageName + "." + javaFile.typeSpec.name,
          Iterables.toArray(javaFile.typeSpec.originatingElements, Element.class));
      try (Writer writer = filerSourceFile.openWriter()) {
        writer.write(javaFile.toString());
      } catch (Exception e) {
        try {
          filerSourceFile.delete();
        } catch (Exception ignored) {
        }
        throw e;
      }
    }
  }
}
