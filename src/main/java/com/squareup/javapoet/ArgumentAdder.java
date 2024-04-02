package com.squareup.javapoet;

interface ArgumentAdder {
    void addArgument(CodeBlock.Builder builder, Object arg);
}
