package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Set;

public class FieldWriter extends VariableWriter {
  private Optional<Snippet> initializer;

  FieldWriter(TypeName type, String name) {
    super(type, name);
    this.initializer = Optional.absent();
  }

  public void setInitializer(Snippet initializer) {
    this.initializer = Optional.of(initializer);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    super.write(appendable, context);
    if (initializer.isPresent()) {
      appendable.append(" = ");
      initializer.get().write(appendable, context);
    }
    appendable.append(';');
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(ImmutableList.of(type()), initializer.asSet(), annotations);
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
