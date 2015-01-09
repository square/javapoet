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

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

public final class VoidName extends TypeName {
  public static final VoidName INSTANCE = new VoidName();

  private VoidName() {
  }

  @Override public AnnotationWriter annotate(Class<? extends Annotation> annotation) {
    throw new UnsupportedOperationException("Cannot annotate 'void'.");
  }

  @Override public AnnotationWriter annotate(ClassName className) {
    throw new UnsupportedOperationException("Cannot annotate 'void'.");
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return ImmutableSet.of();
  }

  @Override
  public String toString() {
    return "void";
  }

  @Override public int hashCode() {
    return 1;
  }

  @Override public boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    return appendable.append("void");
  }
}
