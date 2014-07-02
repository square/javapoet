package dagger.internal.codegen.writer;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;


/**
 * Only named types. Doesn't cover anonymous inner classes.
 */
public abstract class TypeWriter /* ha ha */ extends Modifiable
    implements Writable, TypeName {
  final ClassName name;
  Optional<TypeName> supertype;
  final List<TypeName> implementedTypes;

  TypeWriter(ClassName name) {
    this.name = name;
    this.supertype = Optional.absent();
    this.implementedTypes = Lists.newArrayList();
  }
}
