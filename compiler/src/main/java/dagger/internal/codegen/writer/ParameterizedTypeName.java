package dagger.internal.codegen.writer;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

public class ParameterizedTypeName implements TypeName {
  private final ClassName type;
  private final ImmutableList<? extends TypeName> parameters;

  ParameterizedTypeName(ClassName type, Iterable<? extends TypeName> parameters) {
    this.type = type;
    this.parameters = ImmutableList.copyOf(parameters);
  }

  @Override
  public Set<ClassName> referencedClasses() {
    ImmutableSet.Builder<ClassName> builder = new ImmutableSet.Builder<ClassName>()
        .add(type);
    for (TypeName parameter : parameters) {
      builder.addAll(parameter.referencedClasses());
    }
    return builder.build();
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    appendable.append(context.sourceReferenceForClassName(type));
    Iterator<? extends TypeName> parameterIterator = parameters.iterator();
    verify(parameterIterator.hasNext(), type.toString());
    appendable.append('<');
    parameterIterator.next().write(appendable, context);
    while (parameterIterator.hasNext()) {
      appendable.append(", ");
      parameterIterator.next().write(appendable, context);
    }
    appendable.append('>');
    return appendable;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(type.toString()).append('<');
    Joiner.on(", ").appendTo(builder, parameters);
    return builder.append('>').toString();
  }

  public static ParameterizedTypeName create(ClassName className,
      TypeName... parameters) {
    return new ParameterizedTypeName(className, ImmutableList.copyOf(parameters));
  }

  public static ParameterizedTypeName create(Class<?> parameterizedClass,
      TypeName... parameters) {
    checkArgument(parameterizedClass.getTypeParameters().length == parameters.length);
    return new ParameterizedTypeName(ClassName.fromClass(parameterizedClass),
        ImmutableList.copyOf(parameters));
  }
}
