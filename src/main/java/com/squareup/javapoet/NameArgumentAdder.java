package com.squareup.javapoet;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;

public class NameArgumentAdder implements ArgumentAdder {
    public void addArgument(CodeBlock.Builder builder, Object arg) {
        builder.args.add(argToName(arg));
    }

    private String argToName(Object o) {
        if (o instanceof CharSequence) return o.toString();
        if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
        if (o instanceof FieldSpec) return ((FieldSpec) o).name;
        if (o instanceof MethodSpec) return ((MethodSpec) o).name;
        if (o instanceof TypeSpec) return ((TypeSpec) o).name;
        throw new IllegalArgumentException("expected name but was " + o);
    }
}

class LiteralArgumentAdder implements ArgumentAdder {
    public void addArgument(CodeBlock.Builder builder, Object arg) {
        builder.args.add(arg);
    }
}

class StringArgumentAdder implements ArgumentAdder {
    public void addArgument(CodeBlock.Builder builder, Object arg) {
        builder.args.add(argToString(arg));
    }

    private String argToString(Object o) {
        return o != null ? String.valueOf(o) : null;
    }
}

class TypeArgumentAdder implements ArgumentAdder {
    public void addArgument(CodeBlock.Builder builder, Object arg) {
        builder.args.add(argToType(arg));
    }

    private TypeName argToType(Object o) {
        if (o instanceof TypeName) return (TypeName) o;
        if (o instanceof TypeMirror) return TypeName.get((TypeMirror) o);
        if (o instanceof Element) return TypeName.get(((Element) o).asType());
        if (o instanceof Type) return TypeName.get((Type) o);
        throw new IllegalArgumentException("expected type but was " + o);
    }
}

