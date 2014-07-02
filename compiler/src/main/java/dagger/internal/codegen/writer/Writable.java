package dagger.internal.codegen.writer;

import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;

interface Writable {
  Appendable write(Appendable appendable, CompilationUnitContext context) throws IOException;
}
