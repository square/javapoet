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

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.testing.compile.CompilationRule;

@RunWith(JUnit4.class)
public final class TypesTest extends AbstractTypesTest {
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
