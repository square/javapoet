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
package com.squareup.javapoet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AnnotatedTypeName extends TypeName {

  public static AnnotatedTypeName get(TypeName type, AnnotationSpec... annotations) {
    return new AnnotatedTypeName(type, Arrays.asList(annotations));
  }

  public static AnnotatedTypeName get(Type type, Annotation... annotations) {
    List<AnnotationSpec> specs = new ArrayList<>();
    for (int i = 0; i < annotations.length; i++) {
      specs.add(AnnotationSpec.get(annotations[i]));
    }
    return new AnnotatedTypeName(TypeName.get(type), specs);
  }

  public final TypeName type;
  public final List<AnnotationSpec> annotations;

  AnnotatedTypeName(TypeName type, List<AnnotationSpec> annotations) {
    this.type = type;
    this.annotations = Util.immutableList(annotations);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    if (!annotations.isEmpty()) {
      for (AnnotationSpec annotation : annotations) {
        annotation.emit(out, true);
        out.emit(" ");
      }
    }
    return type.emit(out);
  }

}
