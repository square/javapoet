package dagger.internal.codegen.writer;

import dagger.internal.codegen.writer.Writable.Context;
import java.io.IOException;

final class Writables {
  static Writable toStringWritable(final Object object) {
    return new Writable() {
      @Override
      public Appendable write(Appendable appendable, Context context) throws IOException {
        return appendable.append(object.toString());
      }
    };
  }

  private static Context DEFAULT_CONTEXT = new Context() {
    @Override
    public String sourceReferenceForClassName(ClassName className) {
      return className.canonicalName();
    }

    @Override
    public String compressTypesWithin(String snippet) {
      return snippet;
    }
  };

  static String writeToString(Writable writable) {
    StringBuilder builder = new StringBuilder();
    try {
      writable.write(builder, DEFAULT_CONTEXT);
    } catch (IOException e) {
      throw new AssertionError("StringBuilder doesn't throw IOException", e);
    }
    return builder.toString();
  }

  private Writables() {}
}
