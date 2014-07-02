package dagger.internal.codegen.writer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import dagger.internal.codegen.writer.JavaWriter.CompilationUnitContext;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import static dagger.internal.codegen.writer.Writables.toStringWritable;

public class AnnotationWriter implements Writable, HasClassReferences {
  private final ClassName annotationName;
  private final SortedMap<String, Writable> memberMap = Maps.newTreeMap();

  AnnotationWriter(ClassName annotationName) {
    this.annotationName = annotationName;
  }

  public void setValue(String value) {
    setMember("value", value);
  }

  public void setMember(String name, int value) {
    memberMap.put(name, toStringWritable(value));
  }

  public void setMember(String name, String value) {
    memberMap.put(name, toStringWritable(StringLiteral.forValue(value)));
  }

  @Override
  public Appendable write(Appendable appendable, CompilationUnitContext context)
      throws IOException {
    appendable.append('@');
    annotationName.write(appendable, context);
    if (!memberMap.isEmpty()) {
      appendable.append('(');
      if (memberMap.size() == 1) {
        Entry<String, Writable> onlyEntry = Iterables.getOnlyElement(memberMap.entrySet());
        if (!onlyEntry.getKey().equals("value")) {
          appendable.append(onlyEntry.getKey()).append(" = ");
        }
        onlyEntry.getValue().write(appendable, context);
      }
      appendable.append(')');
    }
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    return ImmutableSet.of(annotationName);
  }
}
