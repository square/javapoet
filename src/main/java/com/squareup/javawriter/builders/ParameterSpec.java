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
package com.squareup.javawriter.builders;

import com.squareup.javawriter.TypeName;
import com.squareup.javawriter.TypeNames;

import static com.google.common.base.Preconditions.checkNotNull;

/** A generated parameter declaration. */
public final class ParameterSpec {
  public final TypeName type;
  public final Name name;

  private ParameterSpec(Builder builder) {
    this.type = checkNotNull(builder.type);
    this.name = checkNotNull(builder.name);
  }

  public static final class Builder {
    private TypeName type;
    private Name name;

    public Builder type(TypeName type) {
      this.type = type;
      return this;
    }

    public Builder type(Class<?> type) {
      this.type = TypeNames.forClass(type);
      return this;
    }

    public Builder name(String name) {
      this.name = new Name(name);
      return this;
    }

    public Builder name(Name name) {
      this.name = name;
      return this;
    }

    public ParameterSpec build() {
      return new ParameterSpec(this);
    }
  }
}
