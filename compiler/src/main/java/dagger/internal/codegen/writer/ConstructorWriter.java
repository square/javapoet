package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;

import static com.google.common.base.Preconditions.checkArgument;

public class ConstructorWriter extends Modifiable implements Writable, HasClassReferences {
  private final String name;
  private final Map<String, VariableWriter> parameterWriters;
  private final BlockWriter blockWriter;

  ConstructorWriter(String name) {
    this.name = name;
    this.parameterWriters = Maps.newLinkedHashMap();
    this.blockWriter = new BlockWriter();
  }

  public VariableWriter addParameter(Class<?> type, String name) {
    return addParameter(ClassName.fromClass(type), name);
  }

  public VariableWriter addParameter(TypeElement type, String name) {
    return addParameter(ClassName.fromTypeElement(type), name);
  }

  public VariableWriter addParameter(TypeWriter type, String name) {
    return addParameter(type.name, name);
  }

  public VariableWriter addParameter(TypeName type, String name) {
    VariableWriter parameterWriter = new VariableWriter(type, name);
    parameterWriters.put(name, parameterWriter);
    return parameterWriter;
  }

  public BlockWriter body() {
    return blockWriter;
  }

  private VariableWriter addParameter(ClassName type, String name) {
    checkArgument(!parameterWriters.containsKey(name));
    VariableWriter parameterWriter = new VariableWriter(type, name);
    parameterWriters.put(name, parameterWriter);
    return parameterWriter;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(
        Iterables.concat(parameterWriters.values(), ImmutableList.of(blockWriter)))
            .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
              @Override
              public Set<ClassName> apply(HasClassReferences input) {
                return input.referencedClasses();
              }
            })
            .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    writeModifiers(appendable).append(name).append('(');
    Iterator<VariableWriter> parameterWritersIterator = parameterWriters.values().iterator();
    if (parameterWritersIterator.hasNext()) {
      parameterWritersIterator.next().write(appendable, context);
    }
    while (parameterWritersIterator.hasNext()) {
      appendable.append(", ");
      parameterWritersIterator.next().write(appendable, context);
    }
    appendable.append(") {");
    blockWriter.write(new IndentingAppendable(appendable), context);
    return appendable.append("}\n");
  }
}
