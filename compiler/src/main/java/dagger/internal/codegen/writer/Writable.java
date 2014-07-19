package dagger.internal.codegen.writer;

import java.io.IOException;

interface Writable {
  interface Context {
    String sourceReferenceForClassName(ClassName className);
    String compressTypesWithin(String snippet);
  }

  Appendable write(Appendable appendable, Context context) throws IOException;
}
