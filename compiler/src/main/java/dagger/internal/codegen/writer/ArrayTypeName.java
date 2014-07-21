package dagger.internal.codegen.writer;

import java.io.IOException;
import java.util.Set;

final class ArrayTypeName implements TypeName {
  private final TypeName componentType;

  ArrayTypeName(TypeName componentType) {
    this.componentType = componentType;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return componentType.referencedClasses();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    return componentType.write(appendable, context).append("[]");
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ArrayTypeName)
        & this.componentType.equals(((ArrayTypeName) obj).componentType);
  }

  @Override
  public int hashCode() {
    return componentType.hashCode();
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }
}
