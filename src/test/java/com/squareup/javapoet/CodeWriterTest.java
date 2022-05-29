package com.squareup.javapoet;

import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class CodeWriterTest {

    @Test
    public void emptyLineInJavaDocDosEndings() throws IOException {
        CodeBlock javadocCodeBlock = CodeBlock.of("A\r\n\r\nB\r\n");
        StringBuilder out = new StringBuilder();
        new CodeWriter(out).emitJavadoc(javadocCodeBlock);
        assertThat(out.toString()).isEqualTo(
                "/**\n" +
                        " * A\n" +
                        " *\n" +
                        " * B\n" +
                        " */\n");
    }

    @Test
    public void floatLiteralInInitializer() throws IOException {
        float a = 7.0f;
        float b = 8.0f;
        FieldSpec fieldSpec = FieldSpec.builder(Float[].class, "floatArray", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).
                initializer("new Float[] { $L, $L }", a, b).build();

        System.out.print(fieldSpec.toString());
        assertThat(fieldSpec.toString()).isEqualTo(
                "public static final java.lang.Float[] floatArray = new Float[] { 7.0f, 8.0f };\n"
        );
    }

    @Test
    public void LongLiteralInInitializer() throws IOException {
        Long a = 11111111111L;
        Long b = 22222222222L;
        FieldSpec fieldSpec = FieldSpec.builder(Long[].class, "longArray", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).
                initializer("new Long[] { $L, $L }", a, b).build();

        System.out.print(fieldSpec.toString());
        assertThat(fieldSpec.toString()).isEqualTo(
                "public static final java.lang.Long[] longArray = new Long[] { 11111111111L, 22222222222L };\n"
        );
    }

}