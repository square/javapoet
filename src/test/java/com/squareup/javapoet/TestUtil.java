package com.squareup.javapoet;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Collection;

final class TestUtil {

  static VariableElement findFirstVariableElement(Collection<VariableElement> elements, String name) {
    for (VariableElement variableElement : elements) {
      if (variableElement.getSimpleName().toString().equals(name)) {
        return variableElement;
      }
    }
    throw new IllegalArgumentException(name + " not found in " + elements);
  }

  static ExecutableElement findFirstExecutableElement(Collection<ExecutableElement> elements, String name) {
    for (ExecutableElement executableElement : elements) {
      if (executableElement.getSimpleName().toString().equals(name)) {
        return executableElement;
      }
    }
    throw new IllegalArgumentException(name + " not found in " + elements);
  }
}
