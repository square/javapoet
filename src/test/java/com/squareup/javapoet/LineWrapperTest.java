/*
 * Copyright (C) 2016 Square, Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class LineWrapperTest {
  @Test public void wrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghij");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghij");
  }

  @Test public void noWrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi");
  }

  @Test public void zeroWidthNoWrap() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghij");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij");
  }

  @Test public void nospaceWrapMax() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghijk");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghijk");
  }

  @Test public void multipleWrite() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("ab");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("cd");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("ef");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("gh");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("ij");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("kl");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("mn");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("op");
    lineWrapper.wrappingSpace(1);
    lineWrapper.append("qr");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("ab cd ef\n  gh ij kl\n  mn op qr");
  }

  @Test public void fencepost() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.append("fghij");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("k");
    lineWrapper.append("lmnop");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\n    klmnop");
  }

  @Test public void fencepostZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.append("fghij");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("k");
    lineWrapper.append("lmnop");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\n    klmnop");
  }

  @Test public void overlyLongLinesWithoutLeadingSpace() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghijkl");
  }

  @Test public void overlyLongLinesWithLeadingSpace() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("\n    abcdefghijkl");
  }

  @Test public void overlyLongLinesWithLeadingZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("abcdefghijkl");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghijkl");
  }

  @Test public void noWrapEmbeddedNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\njklmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\njklmnopqrstuvwxy");
  }

  @Test public void wrapEmbeddedNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghij\nklmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghij\nklmnopqrstuvwxy");
  }

  @Test public void noWrapEmbeddedNewlines_ZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghij\nklmn");
    lineWrapper.append("opqrstuvwxyz");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcdefghij\nklmnopqrstuvwxyz");
  }

  @Test public void wrapEmbeddedNewlines_ZeroWidth() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.zeroWidthSpace(2);
    lineWrapper.append("fghijk\nlmn");
    lineWrapper.append("opqrstuvwxy");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde\n    fghijk\nlmnopqrstuvwxy");
  }

  @Test public void noWrapMultipleNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\nklmnopq\nr");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("stuvwxyz");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nr stuvwxyz");
  }

  @Test public void wrapMultipleNewlines() throws Exception {
    StringBuffer out = new StringBuffer();
    LineWrapper lineWrapper = new LineWrapper(out, "  ", 10);
    lineWrapper.append("abcde");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("fghi\nklmnopq\nrs");
    lineWrapper.wrappingSpace(2);
    lineWrapper.append("tuvwxyz1");
    lineWrapper.close();
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nrs\n    tuvwxyz1");
  }
}
