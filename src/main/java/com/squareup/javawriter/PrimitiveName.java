/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javawriter;

import com.google.common.base.Objects;
import java.io.IOException;
import javax.lang.model.type.PrimitiveType;

public final class PrimitiveName extends TypeName {
  public static PrimitiveName createBoolean() {
    return new PrimitiveName("boolean");
  }

  public static PrimitiveName createByte() {
    return new PrimitiveName("byte");
  }

  public static PrimitiveName createShort() {
    return new PrimitiveName("short");
  }

  public static PrimitiveName createInt() {
    return new PrimitiveName("int");
  }

  public static PrimitiveName createLong() {
    return new PrimitiveName("long");
  }

  public static PrimitiveName createChar() {
    return new PrimitiveName("char");
  }

  public static PrimitiveName createFloat() {
    return new PrimitiveName("float");
  }

  public static PrimitiveName createDouble() {
    return new PrimitiveName("double");
  }

  private final String name;

  private PrimitiveName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return Writables.writeToString(this);
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    super.write(appendable, context);
    return appendable.append(name);
  }

  @Override public int hashCode() {
    return Objects.hashCode(super.hashCode(), name);
  }

  @Override public boolean equals(Object obj) {
    return super.equals(obj)
        && obj instanceof PrimitiveName
        && ((PrimitiveName) obj).name.equals(name);
  }

  static PrimitiveName forTypeMirror(PrimitiveType mirror) {
    switch (mirror.getKind()) {
      case BOOLEAN:
        return createBoolean();
      case BYTE:
        return createByte();
      case SHORT:
        return createShort();
      case INT:
        return createInt();
      case LONG:
        return createLong();
      case CHAR:
        return createChar();
      case FLOAT:
        return createFloat();
      case DOUBLE:
        return createDouble();
      default:
        throw new AssertionError();
    }
  }

  static PrimitiveName forClass(Class<?> primitiveClass) {
    if (boolean.class.equals(primitiveClass)) {
      return createBoolean();
    }
    if (byte.class.equals(primitiveClass)) {
      return createByte();
    }
    if (short.class.equals(primitiveClass)) {
      return createShort();
    }
    if (int.class.equals(primitiveClass)) {
      return createInt();
    }
    if (long.class.equals(primitiveClass)) {
      return createLong();
    }
    if (char.class.equals(primitiveClass)) {
      return createChar();
    }
    if (float.class.equals(primitiveClass)) {
      return createFloat();
    }
    if (double.class.equals(primitiveClass)) {
      return createDouble();
    }
    throw new IllegalArgumentException(primitiveClass + " is not a primitive type");
  }
}
