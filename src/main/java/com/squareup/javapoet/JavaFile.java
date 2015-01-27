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
package com.squareup.javapoet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/** A Java file containing a single top level class. */
public final class JavaFile {
  private static final Appendable NULL_APPENDABLE = new Appendable() {
    @Override public Appendable append(CharSequence charSequence) {
      return this;
    }
    @Override public Appendable append(CharSequence charSequence, int start, int end) {
      return this;
    }
    @Override public Appendable append(char c) {
      return this;
    }
  };

  public final CodeBlock fileComment;
  public final String packageName;
  public final TypeSpec typeSpec;
  public final boolean skipJavaLangImports;

  private JavaFile(Builder builder) {
    this.fileComment = builder.fileComment.build();
    this.packageName = builder.packageName;
    this.typeSpec = builder.typeSpec;
    this.skipJavaLangImports = builder.skipJavaLangImports;
  }

  public void emit(Appendable out, String indent) throws IOException {
    // First pass: emit the entire class, just to collect the types we'll need to import.
    CodeWriter importsCollector = new CodeWriter(NULL_APPENDABLE, indent);
    emit(importsCollector);
    ImmutableMap<ClassName, String> suggestedImports = importsCollector.suggestedImports();

    // Second pass: write the code, taking advantage of the imports.
    CodeWriter codeWriter = new CodeWriter(out, indent, suggestedImports);
    emit(codeWriter);
  }

  private void emit(CodeWriter codeWriter) throws IOException {
    codeWriter.pushPackage(packageName);

    if (!fileComment.isEmpty()) {
      codeWriter.emitComment(fileComment);
    }

    if (!packageName.isEmpty()) {
      codeWriter.emit("package $L;\n", packageName);
      codeWriter.emit("\n");
    }

    int importedTypesCount = 0;
    for (ClassName className : codeWriter.importedTypes().keySet()) {
      if (skipJavaLangImports && className.packageName().equals("java.lang")) continue;
      codeWriter.emit("import $L;\n", className);
      importedTypesCount++;
    }

    if (importedTypesCount > 0) {
      codeWriter.emit("\n");
    }

    typeSpec.emit(codeWriter, null, ImmutableSet.<Modifier>of());

    codeWriter.popPackage();
  }

  public String toString() {
    try {
      StringBuilder result = new StringBuilder();
      emit(result, "  ");
      return result.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder builder(String packageName, TypeSpec typeSpec) {
    checkNotNull(packageName, "packageName == null");
    checkNotNull(typeSpec, "typeSpec == null");
    return new Builder(packageName, typeSpec);
  }

  public static final class Builder {
    private final String packageName;
    private final TypeSpec typeSpec;
    private CodeBlock.Builder fileComment = CodeBlock.builder();
    private boolean skipJavaLangImports;

    private Builder(String packageName, TypeSpec typeSpec) {
      this.packageName = packageName;
      this.typeSpec = typeSpec;
    }

    public Builder addFileComment(String format, Object... args) {
      this.fileComment.add(format, args);
      return this;
    }

    /**
     * Call this to omit imports for classes in {@code java.lang}, such as {@code java.lang.String}.
     *
     * <p>By default, JavaPoet explicitly imports types in {@code java.lang} to defend against
     * naming conflicts. Suppose an (ill-advised) class is named {@code com.example.String}. When
     * {@code java.lang} imports are skipped, generated code in {@code com.example} that references
     * {@code java.lang.String} will get {@code com.example.String} instead.
     */
    public Builder skipJavaLangImports(boolean skipJavaLangImports) {
      this.skipJavaLangImports = skipJavaLangImports;
      return this;
    }

    public JavaFile build() {
      return new JavaFile(this);
    }
  }
}
