package dagger.internal.codegen.writer;

import java.util.Set;

public interface HasClassReferences {
  Set<ClassName> referencedClasses();
}
