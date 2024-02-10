package com.squareup.javapoet;

public interface TypeInliner {
    boolean canInline(Class<?> type);
    String inline(ObjectInliner inliner, Object instance);
}
