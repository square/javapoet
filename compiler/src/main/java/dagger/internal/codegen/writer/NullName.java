package dagger.internal.codegen.writer;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;

enum NullName implements TypeName {
  NULL;

  @Override
  public Set<ClassName> referencedClasses() {
    return ImmutableSet.of();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    return appendable.append("null");
  }

  @Override
  public String toString() {
    return "null";
  }
}
