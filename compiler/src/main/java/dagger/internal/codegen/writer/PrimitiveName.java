package dagger.internal.codegen.writer;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import javax.lang.model.type.PrimitiveType;

public enum PrimitiveName implements TypeName {
  BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE;

  @Override
  public Set<ClassName> referencedClasses() {
    return ImmutableSet.of();
  }

  @Override
  public String toString() {
    return Ascii.toLowerCase(name());
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    return appendable.append(toString());
  }

  static PrimitiveName forTypeMirror(PrimitiveType mirror) {
    switch (mirror.getKind()) {
      case BOOLEAN:
        return BOOLEAN;
      case BYTE:
        return BYTE;
      case SHORT:
        return SHORT;
      case INT:
        return INT;
      case LONG:
        return LONG;
      case CHAR:
        return CHAR;
      case FLOAT:
        return FLOAT;
      case DOUBLE:
        return DOUBLE;
      default:
        throw new AssertionError();
    }
  }

  static PrimitiveName forClass(Class<?> primitiveClass) {
    if (boolean.class.equals(primitiveClass)) {
      return BOOLEAN;
    }
    if (byte.class.equals(primitiveClass)) {
      return BYTE;
    }
    if (short.class.equals(primitiveClass)) {
      return SHORT;
    }
    if (int.class.equals(primitiveClass)) {
      return INT;
    }
    if (long.class.equals(primitiveClass)) {
      return LONG;
    }
    if (char.class.equals(primitiveClass)) {
      return CHAR;
    }
    if (float.class.equals(primitiveClass)) {
      return FLOAT;
    }
    if (double.class.equals(primitiveClass)) {
      return DOUBLE;
    }
    throw new IllegalArgumentException(primitiveClass + " is not a primitive type");
  }
}
