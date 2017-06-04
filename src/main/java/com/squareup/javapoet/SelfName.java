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
import java.util.ArrayList;
import java.util.List;

public final class SelfName extends TypeName {

  private final int depth;

  SelfName(List<AnnotationSpec> annotations) {
    this(annotations, 0);
  }

  SelfName(List<AnnotationSpec> annotations, int depth) {
    super(annotations);
    this.depth = depth;
  }

  public SelfName(int depth) {
    this(new ArrayList<AnnotationSpec>(), depth);
  }

  @Override public TypeName annotated(List<AnnotationSpec> annotations) {
    return new SelfName(annotations);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
 // ClassName className = out.resolve(name == null ? out.peekType().name : name);
    ClassName className = out.resolve(out.peekType(depth).name);
    return emitAnnotations(out).emitAndIndent(out.lookupName(className));
  }

}
