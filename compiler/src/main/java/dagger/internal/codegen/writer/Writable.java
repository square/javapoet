package dagger.internal.codegen.writer;

import java.io.IOException;
import java.util.Set;

interface Writable {
  interface Context {
    String sourceReferenceForClassName(ClassName className);
    Context createSubcontext(Set<ClassName> newTypes);
  }

  Appendable write(Appendable appendable, Context context) throws IOException;
}
