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
package com.squareup.javawriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;

public final class IntersectionTypeName implements TypeName {
  private final ImmutableList<TypeName> typeNames;

  IntersectionTypeName(List<TypeName> typeNames) {
    this.typeNames = ImmutableList.copyOf(typeNames);
  }

  public List<TypeName> typeNames() {
    return typeNames;
  }

  @Override public int hashCode() {
    return typeNames.hashCode();
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof IntersectionTypeName
        && ((IntersectionTypeName) obj).typeNames.equals(typeNames);
  }

  public static IntersectionTypeName create(TypeName bound1, TypeName bound2, TypeName... rest) {
    return new IntersectionTypeName(ImmutableList.copyOf(Lists.asList(bound1, bound2, rest)));
  }
}
