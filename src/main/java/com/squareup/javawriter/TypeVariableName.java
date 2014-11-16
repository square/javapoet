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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;

public final class TypeVariableName implements TypeName {
  private final String name;
  private final Optional<TypeName> extendsBound;
  private final Optional<TypeName> superBound;
  TypeVariableName(String name, Optional<TypeName> extendsBound,
      Optional<TypeName> superBound) {
    this.name = name;
    this.extendsBound = extendsBound;
    this.superBound = superBound;
  }

  public String name() {
    return name;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    ImmutableSet.Builder<ClassName> builder = new ImmutableSet.Builder<ClassName>();
    if (extendsBound.isPresent()) {
      builder.addAll(extendsBound.get().referencedClasses());
    }
    if (superBound.isPresent()) {
      builder.addAll(superBound.get().referencedClasses());
    }
    return builder.build();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    appendable.append(name);
    if (extendsBound.isPresent()) {
      appendable.append(' ');
      extendsBound.get().write(appendable, context);
    }
    if (superBound.isPresent()) {
      appendable.append(' ');
      superBound.get().write(appendable, context);
    }
    return appendable;
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }

  static TypeVariableName named(String name) {
    return new TypeVariableName(
        name, Optional.<TypeName>absent(), Optional.<TypeName>absent());
  }
}
