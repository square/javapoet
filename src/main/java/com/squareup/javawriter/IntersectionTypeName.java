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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class IntersectionTypeName implements TypeName {
  private final List<TypeName> typeNames;

  IntersectionTypeName(List<TypeName> typeNames) {
    this.typeNames = typeNames;
  }

  @Override public Set<ClassName> referencedClasses() {
    return FluentIterable.from(typeNames)
        .transformAndConcat(new Function<TypeName, Iterable<ClassName>>() {
          @Override public Iterable<ClassName> apply(TypeName input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }

  @Override public Appendable write(Appendable appendable, Context context) throws IOException {
    Iterator<TypeName> iterator = typeNames.iterator();
    if (iterator.hasNext()) {
      iterator.next().write(appendable, context);
      while (iterator.hasNext()) {
        appendable.append(" & ");
        iterator.next().write(appendable, context);
      }
    }
    return appendable;
  }

  @Override public int hashCode() {
    return typeNames.hashCode();
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof IntersectionTypeName
        && ((IntersectionTypeName) obj).typeNames.equals(typeNames);
  }
}
