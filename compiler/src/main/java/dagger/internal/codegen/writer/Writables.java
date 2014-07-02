package dagger.internal.codegen.writer;

import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;

final class Writables {
  static Writable toStringWritable(final Object object) {
    return new Writable() {
      @Override
      public Appendable write(Appendable appendable, CompilationUnitContext ignored)
          throws IOException {
        return appendable.append(object.toString());
      }
    };
  }

  private Writables() {}
}
