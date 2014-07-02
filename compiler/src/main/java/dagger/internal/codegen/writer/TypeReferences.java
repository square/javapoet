package dagger.internal.codegen.writer;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;

public class TypeReferences {
  static Function<TypeMirror, TypeName> FOR_TYPE_MIRROR =
      new Function<TypeMirror, TypeName>() {
        @Override public TypeName apply(TypeMirror input) {
          return forTypeMirror(input);
        }
      };

  public static TypeName forTypeMirror(TypeMirror mirror) {
    return mirror.accept(new SimpleTypeVisitor6<TypeName, Void>() {
      @Override
      protected TypeName defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException(e.toString());
      }

      @Override
      public TypeName visitArray(ArrayType t, Void p) {
        return super.visitArray(t, p);
      }

      @Override
      public TypeName visitDeclared(DeclaredType t, Void p) {
        return t.getTypeArguments().isEmpty()
            ? ClassName.fromTypeElement((TypeElement) t.asElement())
            : new ParameterizedTypeName(
                ClassName.fromTypeElement((TypeElement) t.asElement()),
                FluentIterable.from(t.getTypeArguments()).transform(FOR_TYPE_MIRROR));
      }

      @Override
      public TypeName visitPrimitive(PrimitiveType t, Void p) {
        return PrimitiveName.forTypeMirror(t);
      }

      @Override
      public TypeName visitWildcard(WildcardType t, Void p) {
        return WildcardName.forTypeMirror(t);
      }
    }, null);
  }
}
