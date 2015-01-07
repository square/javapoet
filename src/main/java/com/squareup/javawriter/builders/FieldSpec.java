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

import static com.google.common.base.Preconditions.checkNotNull;

/** A generated field declaration. */
public final class FieldSpec {
  public final TypeName type;
  public final Name name;
  public final Snippet initializer;

  private FieldSpec(Builder builder) {
    this.type = checkNotNull(builder.type);
    this.name = checkNotNull(builder.name);
    this.initializer = checkNotNull(builder.initializer);
  }

  public static final class Builder {
    private TypeName type;
    private Name name;
    private Snippet initializer;

    public Builder type(TypeName type) {
      this.type = type;
      return this;
    }

    public Builder name(Name name) {
      this.name = name;
      return this;
    }

    public Builder initializer(String format, Object... args) {
      this.initializer = new Snippet(format, args);
      return this;
    }

    public FieldSpec build() {
      return new FieldSpec(this);
    }
  }
}
