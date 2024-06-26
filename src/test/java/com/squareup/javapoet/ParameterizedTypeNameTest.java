/*
 * Copyright (C) 2020 Square, Inc.
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

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ParameterizedTypeNameTest {
  @Test public void typeNameListEquivalentToVarargs() {
    ParameterizedTypeName fromList = ParameterizedTypeName.get(ClassName.get(Map.class),
        Arrays.asList(TypeName.INT.box(), TypeName.LONG.box()));
    ParameterizedTypeName fromVarargs =
        ParameterizedTypeName.get(ClassName.get(Map.class), TypeName.INT.box(), TypeName.LONG.box());
    assertEquals(fromVarargs, fromList);
  }

  @Test public void typeListEquivalentToVarargs() {
    ParameterizedTypeName fromList = ParameterizedTypeName.get(Map.class,
        Arrays.asList(Integer.class, Long.class));
    ParameterizedTypeName fromVarargs =
        ParameterizedTypeName.get(Map.class, Integer.class, Long.class);
    assertEquals(fromVarargs, fromList);
  }
}
