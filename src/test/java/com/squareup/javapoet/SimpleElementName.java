/*
 * Copyright (C) 2018 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import javax.lang.model.element.Name;

/**
 * Simple implementation of {@link Name} for testing.
 */
public class SimpleElementName implements Name {
  private final String realName;

  public SimpleElementName(String realName) {
    this.realName = realName;
  }

  @Override
  public boolean contentEquals(CharSequence cs) {
    return realName.contentEquals(cs);
  }

  @Override
  public int length() {
    return realName.length();
  }

  @Override
  public char charAt(int index) {
    return realName.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return realName.subSequence(start, end);
  }

  @Override
  public String toString() {
    return realName;
  }
}
