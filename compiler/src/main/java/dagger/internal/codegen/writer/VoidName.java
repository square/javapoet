package dagger.internal.codegen.writer;

import com.google.common.collect.ImmutableSet;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.Set;

public enum VoidName implements TypeName {
  VOID;

  @Override
  public Set<ClassName> referencedClasses() {
    return ImmutableSet.of();
  }

  @Override
  public String toString() {
    return "void";
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    return appendable.append("void");
  }
}
