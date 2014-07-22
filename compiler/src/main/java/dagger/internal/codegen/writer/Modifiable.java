package dagger.internal.codegen.writer;

import com.google.common.collect.Lists;
import dagger.internal.codegen.writer.Writable.Context;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

public abstract class Modifiable {
  final Set<Modifier> modifiers;
  final List<AnnotationWriter> annotations;

  Modifiable() {
    this.modifiers = EnumSet.noneOf(Modifier.class);
    this.annotations = Lists.newArrayList();
  }

  public void addModifiers(Modifier first, Modifier... rest) {
    this.modifiers.addAll(Lists.asList(first, rest));
  }

  public AnnotationWriter annotate(Class<? extends Annotation> annotation) {
    AnnotationWriter annotationWriter = new AnnotationWriter(ClassName.fromClass(annotation));
    this.annotations.add(annotationWriter);
    return annotationWriter;
  }

  Appendable writeModifiers(Appendable appendable) throws IOException {
    for (Modifier modifier : modifiers) {
      appendable.append(modifier.toString()).append(' ');
    }
    return appendable;
  }

  Appendable writeAnnotations(Appendable appendable, Context context) throws IOException {
    for (AnnotationWriter annotationWriter : annotations) {
      annotationWriter.write(appendable, context).append('\n');
    }
    return appendable;
  }
}
