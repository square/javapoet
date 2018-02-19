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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

@RunWith(JUnit4.class)
public class FileReadingTest {
  
  // Used for storing compilation output.
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test public void javaFileObjectUri() {
    TypeSpec type = TypeSpec.classBuilder("Test").build();
    assertThat(JavaFile.builder("", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("Test.java"));
    assertThat(JavaFile.builder("foo", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("foo/Test.java"));
    assertThat(JavaFile.builder("com.example", type).build().toJavaFileObject().toUri())
        .isEqualTo(URI.create("com/example/Test.java"));
  }
  
  @Test public void javaFileObjectKind() {
    JavaFile javaFile = JavaFile.builder("", TypeSpec.classBuilder("Test").build()).build();
    assertThat(javaFile.toJavaFileObject().getKind()).isEqualTo(Kind.SOURCE);
  }
  
  @Test public void javaFileObjectCharacterContent() throws IOException {
    TypeSpec type = TypeSpec.classBuilder("Test")
        .addJavadoc("Pi\u00f1ata\u00a1")
        .addMethod(MethodSpec.methodBuilder("fooBar").build())
        .build();
    JavaFile javaFile = JavaFile.builder("foo", type).build();
    JavaFileObject javaFileObject = javaFile.toJavaFileObject();
    
    // We can never have encoding issues (everything is in process)
    assertThat(javaFileObject.getCharContent(true)).isEqualTo(javaFile.toString());
    assertThat(javaFileObject.getCharContent(false)).isEqualTo(javaFile.toString());
  }
  
  @Test public void javaFileObjectInputStreamIsUtf8() throws IOException {
    JavaFile javaFile = JavaFile.builder("foo", TypeSpec.classBuilder("Test").build())
        .addFileComment("Pi\u00f1ata\u00a1")
        .build();
    byte[] bytes = ByteStreams.toByteArray(javaFile.toJavaFileObject().openInputStream());
    
    // JavaPoet always uses UTF-8.
    assertThat(bytes).isEqualTo(javaFile.toString().getBytes(UTF_8));
  }
  
  @Test public void compileJavaFile() throws Exception {
    final String value = "Hello World!";
    TypeSpec type = TypeSpec.classBuilder("Test")
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(ParameterizedTypeName.get(Callable.class, String.class))
        .addMethod(MethodSpec.methodBuilder("call")
            .returns(String.class)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return $S", value)
            .build())
        .build();
    JavaFile javaFile = JavaFile.builder("foo", type).build();

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, 
        Locale.getDefault(), UTF_8);
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
        Collections.singleton(temporaryFolder.newFolder()));
    CompilationTask task = compiler.getTask(null, 
        fileManager,
        diagnosticCollector,
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.singleton(javaFile.toJavaFileObject()));
    
    assertThat(task.call()).isTrue();
    assertThat(diagnosticCollector.getDiagnostics()).isEmpty();

    ClassLoader loader = fileManager.getClassLoader(StandardLocation.CLASS_OUTPUT);
    Callable<?> test = Class.forName("foo.Test", true, loader)
            .asSubclass(Callable.class)
            .getDeclaredConstructor()
            .newInstance();
    assertThat(Callable.class.getMethod("call").invoke(test)).isEqualTo(value);
  }
}
