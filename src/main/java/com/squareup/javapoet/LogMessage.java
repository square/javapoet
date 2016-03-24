package com.squareup.javapoet;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static com.squareup.javapoet.Util.NULL_APPENDABLE;

/**
 * Text to be printed which references Java types. Unlike a plain {@link CodeBlock}, this is not
 * intended to end up in source code.
 *
 * <p>A common use is for errors or warnings that reference types. If using fully qualified class
 * names make the message hard to read, create a {@link LogMessage} and then format the message with
 * {@code text} and {@code unqualifiedTypes}. For example:
 *
 * <pre>{@code
 *   LogMessage logMessage = LogMessage.create(codeBlock);
 *   messager.printMessage(
 *       Kind.ERROR, logMessage.text + "\nReferenced types: " + logMessage.unqualifiedTypes);
 * }</pre>
 */
public final class LogMessage {
  public final String text;
  public final Set<ClassName> unqualifiedTypes;

  private LogMessage(String text, Set<ClassName> unqualifiedTypes) {
    this.text = text;
    this.unqualifiedTypes = unqualifiedTypes;
  }

  public static LogMessage create(CodeBlock codeBlock) {
    CodeWriter importsCollector = new CodeWriter(
        NULL_APPENDABLE, "", Collections.<String>emptySet());
    StringBuilder output = new StringBuilder();
    try {
      importsCollector.emit(codeBlock);

      CodeWriter writer = new CodeWriter(
          output, "", importsCollector.suggestedImports(), Collections.<String>emptySet());
      writer.emit(codeBlock);

      return new LogMessage(
          output.toString(),
          Collections.unmodifiableSet(new TreeSet<>(writer.importedTypes().values())));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
