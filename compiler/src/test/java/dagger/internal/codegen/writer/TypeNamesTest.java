package dagger.internal.codegen.writer;

import com.google.testing.compile.CompilationRule;
import java.nio.charset.Charset;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.truth0.Truth.ASSERT;

@RunWith(JUnit4.class)
public class TypeNamesTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private TypeMirror getType(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  @Test
  public void forTypeMirror_basicTypes() {
    ASSERT.that(TypeNames.forTypeMirror(getType(Object.class)))
        .isEqualTo(ClassName.fromClass(Object.class));
    ASSERT.that(TypeNames.forTypeMirror(getType(Charset.class)))
        .isEqualTo(ClassName.fromClass(Charset.class));
    ASSERT.that(TypeNames.forTypeMirror(getType(TypeNamesTest.class)))
        .isEqualTo(ClassName.fromClass(TypeNamesTest.class));
  }

  @Test
  public void forTypeMirror_parameterizedType() {
    DeclaredType setType =
        compilation.getTypes().getDeclaredType(getElement(Set.class), getType(Object.class));
    ASSERT.that(TypeNames.forTypeMirror(setType))
        .isEqualTo(ParameterizedTypeName.create(Set.class, ClassName.fromClass(Object.class)));
  }

  @Test
  public void forTypeMirror_primitive() {
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(PrimitiveName.BOOLEAN);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(PrimitiveName.BYTE);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(PrimitiveName.SHORT);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(PrimitiveName.INT);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(PrimitiveName.LONG);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(PrimitiveName.CHAR);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(PrimitiveName.FLOAT);
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(PrimitiveName.DOUBLE);
  }

  @Test
  public void forTypeMirror_arrays() {
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getArrayType(getType(Object.class))))
        .isEqualTo(new ArrayTypeName(ClassName.fromClass(Object.class)));
  }

  @Test
  public void forTypeMirror_void() {
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(VoidName.VOID);
  }

  @Test
  public void forTypeMirror_null() {
    ASSERT.that(TypeNames.forTypeMirror(compilation.getTypes().getNullType()))
        .isEqualTo(NullName.NULL);
  }
}
