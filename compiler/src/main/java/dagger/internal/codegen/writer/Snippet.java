package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class Snippet implements HasClassReferences, Writable {
  private final String format;
  private final ImmutableSet<TypeName> types;
  private final ImmutableList<Object> args;

  private Snippet(String format, ImmutableSet<TypeName> types, ImmutableList<Object> args) {
    this.format = format;
    this.types = types;
    this.args = args;
  }

  public String format() {
    return format;
  }

  public ImmutableList<Object> args() {
    return args;
  }

  public ImmutableSet<TypeName> types() {
    return types;
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
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
    ImmutableList.Builder<Object> formattedArgsBuilder = ImmutableList.builder();
    for (Object arg : args) {
      if (arg instanceof Writable) {
        formattedArgsBuilder.add(((Writable) arg).write(new StringBuilder(), context).toString());
      } else {
        formattedArgsBuilder.add(arg);
      }
    }

    @SuppressWarnings("resource") // intentionally don't close the formatter
    Formatter formatter = new Formatter(appendable);
    formatter.format(format, formattedArgsBuilder.build().toArray(new Object[0]));

    return appendable;
  }

  public static Snippet format(String format, Object... args) {
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
    return new Snippet(format, types.build(), ImmutableList.copyOf(args));
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
    ImmutableList.Builder<Object> argsBuilder = ImmutableList.builder();
    if (iterator.hasNext()) {
      Snippet firstSnippet = iterator.next();
      stringBuilder.append(firstSnippet.format());
      typesBuilder.addAll(firstSnippet.types());
      argsBuilder.addAll(firstSnippet.args());
    }
    while (iterator.hasNext()) {
      Snippet nextSnippet = iterator.next();
      stringBuilder.append(", ").append(nextSnippet.format());
      typesBuilder.addAll(nextSnippet.types());
      argsBuilder.addAll(nextSnippet.args());
    }
    return new Snippet(stringBuilder.toString(), typesBuilder.build(), argsBuilder.build());
  }
}
