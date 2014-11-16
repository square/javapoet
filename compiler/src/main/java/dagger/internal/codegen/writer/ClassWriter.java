package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

public final class ClassWriter extends TypeWriter {
  private final List<ConstructorWriter> constructorWriters;
  private final List<TypeVariableName> typeVariables;

  ClassWriter(ClassName className) {
    super(className);
    this.constructorWriters = Lists.newArrayList();
    this.typeVariables = Lists.newArrayList();
  }

  public ConstructorWriter addConstructor() {
    ConstructorWriter constructorWriter = new ConstructorWriter(name.simpleName());
    constructorWriters.add(constructorWriter);
    return constructorWriter;
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
    writeModifiers(appendable).append("class ").append(name.simpleName());
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
    if (!fieldWriters.isEmpty()) {
      appendable.append('\n');
    }
    for (VariableWriter fieldWriter : fieldWriters.values()) {
      fieldWriter.write(new IndentingAppendable(appendable), context).append("\n");
    }
    for (ConstructorWriter constructorWriter : constructorWriters) {
      appendable.append('\n');
      if (!isDefaultConstructor(constructorWriter)) {
        constructorWriter.write(new IndentingAppendable(appendable), context);
      }
    }
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

  private static final Set<Modifier> VISIBILIY_MODIFIERS =
      Sets.immutableEnumSet(PUBLIC, PROTECTED, PRIVATE);

  private boolean isDefaultConstructor(ConstructorWriter constructorWriter) {
    return Sets.intersection(VISIBILIY_MODIFIERS, modifiers)
        .equals(Sets.intersection(VISIBILIY_MODIFIERS, constructorWriter.modifiers))
        && constructorWriter.body().isEmpty();
  }

  @Override
  public Set<ClassName> referencedClasses() {
    @SuppressWarnings("unchecked")
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(nestedTypeWriters, fieldWriters.values(), constructorWriters,
            methodWriters, implementedTypes, supertype.asSet(), annotations);
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
