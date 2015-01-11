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

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/** A Java file containing a single top level class. */
public final class JavaFile {
  public final String packageName;
  public final TypeSpec typeSpec;

  private JavaFile(Builder builder) {
    this.packageName = builder.packageName;
    this.typeSpec = checkNotNull(builder.typeSpec);
  }

  public String toString() {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    CodeWriter importsCollector = new CodeWriter(new StringBuilder());
    emit(importsCollector);
    ImmutableMap<ClassName, String> suggestedImports = importsCollector.suggestedImports();

    // Second pass: Write the code, taking advantage of the imports.
    StringBuilder result = new StringBuilder();
    CodeWriter codeWriter = new CodeWriter(result, suggestedImports);
    emit(codeWriter);
    return result.toString();
  }

  private void emit(CodeWriter codeWriter) {
    codeWriter.pushPackage(packageName);

    if (!packageName.isEmpty()) {
      codeWriter.emit("package $L;\n", packageName);
      codeWriter.emit("\n");
    }

    if (!codeWriter.importedTypes().isEmpty()) {
      for (ClassName className : codeWriter.importedTypes().keySet()) {
        codeWriter.emit("import $L;\n", className);
      }
      codeWriter.emit("\n");
    }

    typeSpec.emit(codeWriter, null);

    codeWriter.popPackage();
  }

  public static final class Builder {
    private String packageName = "";
    private TypeSpec typeSpec;

    public Builder packageName(String packageName) {
      this.packageName = checkNotNull(packageName);
      return this;
    }

    public Builder typeSpec(TypeSpec typeSpec) {
      this.typeSpec = typeSpec;
      return this;
    }

    public JavaFile build() {
      return new JavaFile(this);
    }
  }
}
