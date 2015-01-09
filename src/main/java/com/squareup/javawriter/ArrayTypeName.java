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

import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Set;

final class ArrayTypeName extends TypeName {
  private final TypeName componentType;

  ArrayTypeName(TypeName componentType) {
    this.componentType = componentType;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat
        = Iterables.concat(super.referencedClasses(), ImmutableList.of(componentType));
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    super.write(appendable, context);
    return componentType.write(appendable, context).append("[]");
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ArrayTypeName)
        && super.equals(obj)
        && this.componentType.equals(((ArrayTypeName) obj).componentType);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), componentType);
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }

  public static ArrayTypeName create(TypeName componentType) {
    return new ArrayTypeName(componentType);
  }
}
