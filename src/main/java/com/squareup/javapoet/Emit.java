package com.squareup.javapoet;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;

import static com.squareup.javapoet.Util.checkState;
import static com.squareup.javapoet.Util.stringLiteralWithDoubleQuotes;
//Extract Class
public class Emit {
    public static void emitJavadoc(CodeWriter codeWriter, CodeBlock javadocCodeBlock) throws IOException {
        if (javadocCodeBlock.isEmpty()) return;

        codeWriter.emit("/**\n");
        codeWriter.javadoc = true;
        try {
            codeWriter.emit(javadocCodeBlock, true);
        } finally {
            codeWriter.javadoc = false;
        }
        codeWriter.emit(" */\n");
    }

    public static void emitAnnotations(CodeWriter codeWriter, List<AnnotationSpec> annotations, boolean inline) throws IOException {
        for (AnnotationSpec annotationSpec : annotations) {
            annotationSpec.emit(codeWriter, inline);
            codeWriter.emit(inline ? " " : "\n");
        }
    }


    public static void emitModifiers( CodeWriter codeWriter,Set<Modifier> modifiers, Set<Modifier> implicitModifiers)
            throws IOException {
        if (modifiers.isEmpty()) return;
        for (Modifier modifier : EnumSet.copyOf(modifiers)) {
            if (implicitModifiers.contains(modifier)) continue;
            codeWriter.emitAndIndent(modifier.name().toLowerCase(Locale.US));
            codeWriter.emitAndIndent(" ");
        }
    }

    public static void emitModifiers(CodeWriter codeWriter, Set<Modifier> modifiers) throws IOException {
        emitModifiers(codeWriter , modifiers, Collections.emptySet());
    }

}
