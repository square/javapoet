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

import com.google.common.collect.Iterators;
import com.squareup.javawriter.Writable.Context;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

final class Writables {

  static class Joiner {
    static Joiner on(String separator) {
      return new Joiner(separator);
    }

    static Joiner on(char separator) {
      return on(String.valueOf(separator));
    }

    private final String separator;

    private Joiner(String separator) {
      this.separator = separator;
    }

    protected Joiner(Joiner prototype) {
      this.separator = prototype.separator;
    }

    final Appendable appendTo(Appendable appendable, Context context, Writable[] parts)
        throws IOException {
      return appendTo(appendable, context, Iterators.forArray(parts));
    }

    final Appendable appendTo(Appendable appendable, Context context, Writable part1,
        Writable part2, Writable... parts) throws IOException {
      return appendTo(appendable, context,
          Iterators.concat(Iterators.forArray(part1, part2), Iterators.forArray(parts)));
    }

    final Appendable appendTo(Appendable appendable, Context context,
        Iterable<? extends Writable> parts) throws IOException {
      return appendTo(appendable, context, parts.iterator());
    }

    Appendable appendTo(Appendable appendable, Context context, Iterator<? extends Writable> parts)
        throws IOException {
      checkNotNull(appendable);
      checkNotNull(context);
      if (parts.hasNext()) {
        parts.next().write(appendable, context);
        while (parts.hasNext()) {
          appendable.append(separator);
          parts.next().write(appendable, context);
        }
      }
      return appendable;
    }

    final Joiner prefix(String prefix) {
      return wrap(prefix, "");
    }

    final Joiner suffix(String suffix) {
      return wrap("", suffix);
    }

    final Joiner wrap(final String prefix, final String suffix) {
      return new Joiner(this) {
        @Override
        Appendable appendTo(Appendable appendable, Context context,
            Iterator<? extends Writable> parts) throws IOException {
          boolean needsWrap = parts.hasNext();
          if (needsWrap) {
            appendable.append(prefix);
          }
          super.appendTo(appendable, context, parts);
          if (needsWrap) {
            appendable.append(suffix);
          }
          return appendable;
        }
      };
    }
  }

  static Writable toStringWritable(final Object object) {
    return new Writable() {
      @Override
      public Appendable write(Appendable appendable, Context context) throws IOException {
        return appendable.append(object.toString());
      }
    };
  }

  private static final Context DEFAULT_CONTEXT = new Context() {
    @Override
    public String sourceReferenceForClassName(ClassName className) {
      return className.canonicalName();
    }

    @Override
    public Context createSubcontext(Set<ClassName> newTypes) {
      return this;
    }
  };

  static String writeToString(Writable writable) {
    StringBuilder builder = new StringBuilder();
    try {
      writable.write(builder, DEFAULT_CONTEXT);
    } catch (IOException e) {
      throw new AssertionError("StringBuilder doesn't throw IOException" + e);
    }
    return builder.toString();
  }

  private Writables() {
  }
}
