package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class Snippet implements HasClassReferences, Writable {
  private final String value;
  private final ImmutableSet<TypeName> types;

  private Snippet(String value, ImmutableSet<TypeName> types) {
    this.value = value;
    this.types = types;
  }

  public String value() {
    return value;
  }

  public ImmutableSet<TypeName> types() {
    return types;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(types)
        .transformAndConcat(new Function<TypeName, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(TypeName input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    return appendable.append(context.compressTypesWithin(value));
  }

  public static Snippet format(String format, Object... args) {
    String value = String.format(format, args);
    ImmutableSet.Builder<TypeName> types = ImmutableSet.builder();
    for (Object arg : args) {
      if (arg instanceof Snippet) {
        types.addAll(((Snippet) arg).types);
      }
      if (arg instanceof TypeName) {
        types.add((TypeName) arg);
      }
      if (arg instanceof HasTypeName) {
        types.add(((HasTypeName) arg).name());
      }
    }
    return new Snippet(value, types.build());
  }

  public static Snippet format(String format, Iterable<? extends Object> args) {
    return format(format, Iterables.toArray(args, Object.class));
  }

  public static Snippet memberSelectSnippet(Iterable<? extends Object> selectors) {
    return format(Joiner.on('.').join(Collections.nCopies(Iterables.size(selectors), "%s")),
        selectors);
  }

  public static Snippet makeParametersSnippet(List<Snippet> parameterSnippets) {
    Iterator<Snippet> iterator = parameterSnippets.iterator();
    StringBuilder stringBuilder = new StringBuilder();
    ImmutableSet.Builder<TypeName> typesBuilder = ImmutableSet.builder();
    if (iterator.hasNext()) {
      Snippet firstSnippet = iterator.next();
      stringBuilder.append(firstSnippet.value());
      typesBuilder.addAll(firstSnippet.types());
    }
    while (iterator.hasNext()) {
      Snippet nextSnippet = iterator.next();
      stringBuilder.append(", ").append(nextSnippet.value());
      typesBuilder.addAll(nextSnippet.types());
    }
    return new Snippet(stringBuilder.toString(), ImmutableSet.copyOf(typesBuilder.build()));
  }
}
