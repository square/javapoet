package dagger.internal.codegen.writer;

import java.io.IOException;
import java.util.Set;


public class VariableWriter extends Modifiable implements Writable, HasClassReferences {
  private final TypeName type;
  private final String name;

  VariableWriter(TypeName type, String name) {
    this.type = type;
    this.name = name;
  }

  public TypeName type() {
    return type;
  }

  public String name() {
    return name;
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    writeModifiers(appendable);
    type.write(appendable, context);
    return appendable.append(' ').append(name);
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return type.referencedClasses();
  }
}
