package com.squareup.javapoet;

import java.io.IOException;

public interface Emitter {
    void emit(CodeWriter codeWriter) throws IOException;
}
