package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class BlockWriter implements Writable, HasClassReferences {
  private final List<Snippet> snippets;

  BlockWriter() {
    this.snippets = Lists.newArrayList();
  }

  public BlockWriter addSnippet(String snippet, Object... args) {
    snippets.add(Snippet.format(snippet, args));
    return this;
  }

  public BlockWriter addSnippet(Snippet snippet) {
    snippets.add(snippet);
    return this;
  }

  boolean isEmpty() {
    return snippets.isEmpty();
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    for (Snippet snippet : snippets) {
      appendable.append('\n');
      snippet.write(appendable, context);
    }
    return appendable.append('\n');
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return FluentIterable.from(snippets)
        .transformAndConcat(new Function<HasClassReferences, Set<ClassName>>() {
          @Override
          public Set<ClassName> apply(HasClassReferences input) {
            return input.referencedClasses();
          }
        })
        .toSet();
  }
}
