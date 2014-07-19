package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class InterfaceWriter extends TypeWriter {
  private final List<TypeVariableName> typeVariables;
  private final List<TypeWriter> nestedTypeWriters;

  InterfaceWriter(ClassName name) {
    super(name);
    this.typeVariables = Lists.newArrayList();
    this.nestedTypeWriters = Lists.newArrayList();
  }

  public void addTypeVariable(TypeVariableName typeVariable) {
    this.typeVariables.add(typeVariable);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    context = context.createSubcontext(FluentIterable.from(nestedTypeWriters)
        .transform(new Function<TypeWriter, ClassName>() {
          @Override public ClassName apply(TypeWriter input) {
            return input.name;
          }
        })
        .toSet());
    writeAnnotations(appendable, context);
    writeModifiers(appendable).append("interface ").append(name.simpleName());
    if (!typeVariables.isEmpty()) {
      appendable.append('<');
      Joiner.on(", ").appendTo(appendable, typeVariables);
      appendable.append('>');
    }
    if (supertype.isPresent()) {
      appendable.append(" extends ");
      supertype.get().write(appendable, context);
    }
    Iterator<TypeName> implementedTypesIterator = implementedTypes.iterator();
    if (implementedTypesIterator.hasNext()) {
      appendable.append(" implements ");
      implementedTypesIterator.next().write(appendable, context);
      while (implementedTypesIterator.hasNext()) {
        appendable.append(", ");
        implementedTypesIterator.next().write(appendable, context);
      }
    }
    appendable.append(" {");
    for (MethodWriter methodWriter : methodWriters) {
      appendable.append('\n');
      methodWriter.write(new IndentingAppendable(appendable), context);
    }
    for (TypeWriter nestedTypeWriter : nestedTypeWriters) {
      appendable.append('\n');
      nestedTypeWriter.write(new IndentingAppendable(appendable), context);
    }
    appendable.append("}\n");
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(nestedTypeWriters, methodWriters, implementedTypes, supertype.asSet(),
            annotations);
    return FluentIterable.from(concat)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }
}
