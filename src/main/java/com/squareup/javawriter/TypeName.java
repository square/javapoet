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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public abstract class TypeName implements HasClassReferences, Writable {
  final List<AnnotationWriter> annotations;

  protected TypeName() {
    annotations = Lists.newArrayList();
  }

  public AnnotationWriter annotate(Class<? extends Annotation> annotation) {
    return annotate(ClassName.fromClass(annotation));
  }

  public AnnotationWriter annotate(ClassName className) {
    AnnotationWriter annotationWriter = new AnnotationWriter(className);
    annotations.add(annotationWriter);
    return annotationWriter;
  }

  @Override public Appendable write(Appendable appendable, Context context) throws IOException {
    return Writables.Joiner.on(" ").suffix(" ").appendTo(appendable, context, annotations);
  }

  @Override public int hashCode() {
    return annotations.hashCode();
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof TypeName && ((TypeName) obj).annotations.equals(annotations);
  }

  @Override public Set<ClassName> referencedClasses() {
    return FluentIterable.from(annotations)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }
}
