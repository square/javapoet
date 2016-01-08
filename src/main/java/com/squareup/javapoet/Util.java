/*
 * Copyright (C) 2015 Square, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileObject.Kind;

/**
 * Like Guava, but worse and standalone. This makes it easier to mix JavaPoet with libraries that
 * bring their own version of Guava.
 */
final class Util {
  private Util() {
  }

  /**
   * In-memory file manager.
   *
   * @author Christian Stein
   */
  static final class Manager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    final class ByteArrayFileObject extends SimpleJavaFileObject {
      ByteArrayOutputStream stream;

      ByteArrayFileObject(String canonical, Kind kind) {
        super(URI.create("javapoet:///" + canonical.replace('.', '/') + kind.extension), kind);
      }

      byte[] getBytes() {
        return stream.toByteArray();
      }

      @Override public CharSequence getCharContent(boolean ignoreErrors) throws IOException {
        return new String(getBytes(), StandardCharsets.UTF_8.name());
      }

      @Override public OutputStream openOutputStream() throws IOException {
        this.stream = new ByteArrayOutputStream(2000);
        return stream;
      }
    }

    final class SecureLoader extends SecureClassLoader {
      SecureLoader(ClassLoader parent) {
        super(parent);
      }

      @Override protected Class<?> findClass(String className) throws ClassNotFoundException {
        ByteArrayFileObject object = map.get(className);
        if (object == null) {
          throw new ClassNotFoundException(className);
        }
        byte[] b = object.getBytes();
        return super.defineClass(className, b, 0, b.length);
      }
    }

    private final Map<String, ByteArrayFileObject> map = new HashMap<>();
    private final ClassLoader parent;

    Manager(StandardJavaFileManager standardManager, ClassLoader parent) {
      super(standardManager);
      this.parent = parent != null ? parent : getClass().getClassLoader();
    }

    @Override public ClassLoader getClassLoader(Location location) {
      return new SecureLoader(parent);
    }

    @Override public JavaFileObject getJavaFileForOutput(Location location, String className,
        Kind kind, FileObject sibling) throws IOException {
      ByteArrayFileObject object = new ByteArrayFileObject(className, kind);
      map.put(className, object);
      return object;
    }

    @Override public boolean isSameFile(FileObject a, FileObject b) {
      return a.toUri().equals(b.toUri());
    }
  }

  /** Modifier.DEFAULT doesn't exist until Java 8, but we want to run on earlier releases. */
  public static final Modifier DEFAULT;
  static {
    Modifier def = null;
    try {
      def = Modifier.valueOf("DEFAULT");
    } catch (IllegalArgumentException ignored) {
    }
    DEFAULT = def;
  }

  public static <K, V> Map<K, List<V>> immutableMultimap(Map<K, List<V>> multimap) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>();
    for (Map.Entry<K, List<V>> entry : multimap.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      result.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  public static void checkArgument(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalArgumentException(String.format(format, args));
  }

  public static <T> T checkNotNull(T reference, String format, Object... args) {
    if (reference == null) throw new NullPointerException(String.format(format, args));
    return reference;
  }

  public static void checkState(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalStateException(String.format(format, args));
  }

  public static <T> List<T> immutableList(List<T> list) {
    return Collections.unmodifiableList(new ArrayList<>(list));
  }

  public static <T> Set<T> immutableSet(Collection<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  public static String join(String separator, List<String> parts) {
    if (parts.isEmpty()) return "";
    StringBuilder result = new StringBuilder();
    result.append(parts.get(0));
    for (int i = 1; i < parts.size(); i++) {
      result.append(separator).append(parts.get(i));
    }
    return result.toString();
  }

  public static <T> Set<T> union(Set<T> a, Set<T> b) {
    Set<T> result = new LinkedHashSet<>();
    result.addAll(a);
    result.addAll(b);
    return result;
  }

  public static void requireExactlyOneOf(Set<Modifier> modifiers, Modifier... mutuallyExclusive) {
    int count = 0;
    for (Modifier modifier : mutuallyExclusive) {
      if (modifier == null && Util.DEFAULT == null) continue; // Skip 'DEFAULT' if it doesn't exist!
      if (modifiers.contains(modifier)) count++;
    }
    checkArgument(count == 1, "modifiers %s must contain one of %s",
        modifiers, Arrays.toString(mutuallyExclusive));
  }

  public static boolean hasDefaultModifier(Collection<Modifier> modifiers) {
    return DEFAULT != null && modifiers.contains(DEFAULT);
  }
}
