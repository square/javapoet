package com.squareup.javapoet;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class LogMessageTest {

  @Test public void create() {
    ClassName concreteType = ClassName.get("com.squareup.tacos", "ConcreteType");
    ClassName interfaceType = ClassName.get("com.squareup.tacos", "InterfaceType");
    LogMessage logMessage = LogMessage.create(CodeBlock.of("$T does not implement $T", concreteType, interfaceType));
    assertThat(logMessage.text).isEqualTo("ConcreteType does not implement InterfaceType");
    assertThat(logMessage.unqualifiedTypes).containsExactly(concreteType, interfaceType);
  }

  @Test public void conflictingSimpleNames() {
    ClassName taco = ClassName.get("com.squareup.tacos", "Sandwich");
    ClassName burrito = ClassName.get("com.squareup.burritos", "Sandwich");
    LogMessage logMessage = LogMessage.create(CodeBlock.of("$T is not a $T", taco, burrito));
    assertThat(logMessage.text).isEqualTo("Sandwich is not a com.squareup.burritos.Sandwich");
    assertThat(logMessage.unqualifiedTypes).containsExactly(taco);
  }
}
