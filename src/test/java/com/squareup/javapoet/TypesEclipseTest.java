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
package com.squareup.javapoet;

import static com.google.common.base.Charsets.*;
import static com.google.common.base.Preconditions.*;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.Statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(JUnit4.class)
public final class TypesEclipseTest extends AbstractTypesTest {
  /**
   * A {@link JUnit4} {@link Rule} that executes tests such that a instances of {@link Elements} and
   * {@link Types} are available during execution.
   *
   * <p>To use this rule in a test, just add the following field: <pre>   {@code
   *   @Rule public CompilationRule compilationRule = new CompilationRule();}
   *
   * @author Gregory Kick
   */
  public static final class CompilationRule implements TestRule {
    private Elements elements;
    private Types types;

    @Override
    public Statement apply(final Statement base, Description description) {
      return new Statement() {
        @Override public void evaluate() throws Throwable {
          final AtomicReference<Throwable> thrown = new AtomicReference<>();
          boolean successful = compile(ImmutableList.of(new AbstractProcessor() {
            @Override
            public SourceVersion getSupportedSourceVersion() {
              return SourceVersion.latest();
            }

            @Override
            public Set<String> getSupportedAnnotationTypes() {
              return ImmutableSet.of("*");
            }

            @Override
            public synchronized void init(ProcessingEnvironment processingEnv) {
              super.init(processingEnv);
              elements = processingEnv.getElementUtils();
              types = processingEnv.getTypeUtils();
            }

            @Override
            public boolean process(Set<? extends TypeElement> annotations,
                RoundEnvironment roundEnv) {
              // just run the test on the last round after compilation is over
              if (roundEnv.processingOver()) {
                try {
                  base.evaluate();
                } catch (Throwable e) {
                  thrown.set(e);
                }
              }
              return false;
            }
          }));
          checkState(successful);
          Throwable t = thrown.get();
          if (t != null) {
            throw t;
          }
        }
      };
    }

    /**
     * Returns the {@link Elements} instance associated with the current execution of the rule.
     *
     * @throws IllegalStateException if this method is invoked outside the execution of the rule.
     */
    public Elements getElements() {
      checkState(elements != null, "Not running within the rule");
      return elements;
    }

    /**
     * Returns the {@link Types} instance associated with the current execution of the rule.
     *
     * @throws IllegalStateException if this method is invoked outside the execution of the rule.
     */
    public Types getTypes() {
      checkState(elements != null, "Not running within the rule");
      return types;
    }

    static private boolean compile(Iterable<? extends Processor> processors) {
      JavaCompiler compiler = new EclipseCompiler();
      DiagnosticCollector<JavaFileObject> diagnosticCollector =
          new DiagnosticCollector<>();
      JavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, Locale.getDefault(), UTF_8);
      JavaCompiler.CompilationTask task = compiler.getTask(
          null,
          fileManager,
          diagnosticCollector,
          ImmutableSet.of(),
          ImmutableSet.of(TypesEclipseTest.class.getCanonicalName()),
          ImmutableSet.of());
      task.setProcessors(processors);
      return task.call();
    }
  }

  @Rule public final CompilationRule compilation = new CompilationRule();

  @Override
  protected Elements getElements() {
    return compilation.getElements();
  }

  @Override
  protected Types getTypes() {
    return compilation.getTypes();
  }
}
