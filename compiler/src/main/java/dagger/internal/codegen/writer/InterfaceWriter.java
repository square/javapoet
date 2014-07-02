package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class InterfaceWriter extends TypeWriter {
  private final List<TypeVariableName> typeVariables;
  private final List<MethodWriter> methodWriters;
  private final List<TypeWriter> nestedTypeWriters;

  InterfaceWriter(ClassName name) {
    super(name);
    this.typeVariables = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    this.nestedTypeWriters = Lists.newArrayList();
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context) throws IOException {
    writeModifiers(appendable).append("class ").append(name.simpleName());
    if (!typeVariables.isEmpty()) {
      appendable.append('<');
      Joiner.on(", ").appendTo(appendable, typeVariables);
      appendable.append('>');
    }
    appendable.append(" {\n");
    for (MethodWriter methodWriter : methodWriters) {
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append('\n');
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append("}\n");
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(nestedTypeWriters)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }
}
