/*
 * Copyright (C) 2019 Square, Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.squareup.javapoet.ClassName;
import org.junit.Test;

/**
 * Since it is impossible to import classes from the default package into other
 * modules, this test must live in this package.
 */
public final class ClassNameNoPackageTest {
  @Test public void shouldSupportClassInDefaultPackage() {
    ClassName className = ClassName.get(ClassNameNoPackageTest.class);
    assertThat(className.packageName()).isEqualTo("");
    assertThat(className.simpleName()).isEqualTo("ClassNameNoPackageTest");
    assertThat(className.toString()).isEqualTo("ClassNameNoPackageTest");
  }
}
