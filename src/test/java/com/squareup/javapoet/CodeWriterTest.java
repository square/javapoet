package com.squareup.javapoet;

import org.junit.Test;

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
}