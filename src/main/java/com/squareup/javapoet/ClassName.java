/*
 * Copyright (C) 2014 Google, Inc.
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

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

/** A fully-qualified class name for top-level and member classes. */
public final class ClassName implements Type, Comparable<ClassName> {
  public static final ClassName OBJECT = ClassName.get(Object.class);

  /** From top to bottom. This will be ["java.util", "Map", "Entry"] for {@link Map.Entry}. */
  final ImmutableList<String> names;
  final String canonicalName;

  private ClassName(List<String> names) {
    for (int i = 1; i < names.size(); i++) {
      checkArgument(SourceVersion.isName(names.get(i)), "part '%s' is keyword", names.get(i));
    }
    this.names = ImmutableList.copyOf(names);
    this.canonicalName = Joiner.on(".").join(names.get(0).isEmpty()
        ? names.subList(1, names.size())
        : names);
  }

  /** Returns the package name, like {@code "java.util"} for {@code Map.Entry}. */
  public String packageName() {
    return names.get(0);
  }

  /**
   * Returns the enclosing class, like {@link Map} for {@code Map.Entry}. Returns null if this class
   * is not nested in another class.
   */
  public ClassName enclosingClassName() {
    if (names.size() == 2) return null;
    return new ClassName(names.subList(0, names.size() - 1));
  }

  /**
   * Returns a new {@link ClassName} instance for the specified {@code name} as nested inside this
   * class.
   */
  public ClassName nestedClass(String name) {
    checkNotNull(name, "name == null");
    return new ClassName(new ImmutableList.Builder<String>()
        .addAll(names)
        .add(name)
        .build());
  }

  public ImmutableList<String> simpleNames() {
    return names.subList(1, names.size());
  }

  /**
   * Returns a class that shares the same enclosing package or class. If this class is enclosed by
   * another class, this is equivalent to {@code enclosingClassName().nestedClass(name)}. Otherwise
   * it is equivalent to {@code get(packageName(), name)}.
   */
  public ClassName peerClass(String name) {
    return new ClassName(new ImmutableList.Builder<String>()
        .addAll(names.subList(0, names.size() - 1))
        .add(name)
        .build());
  }

  /** Returns the simple name of this class, like {@code "Entry"} for {@link Map.Entry}. */
  public String simpleName() {
    return Iterables.getLast(names);
  }

  public static ClassName get(Class<?> clazz) {
    checkNotNull(clazz, "clazz == null");
    checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
    checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
    checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");
    List<String> names = new ArrayList<>();
    for (Class<?> c = clazz; c != null; c = c.getEnclosingClass()) {
      names.add(c.getSimpleName());
    }
    names.add(clazz.getPackage().getName());
    Collections.reverse(names);
    return new ClassName(names);
  }

  /**
   * Returns a new {@link ClassName} instance for the given fully-qualified class name string. This
   * method assumes that the input is ASCII and follows typical Java style (lowercase package
   * names, UpperCamelCase class names) and may produce incorrect results or throw
   * {@link IllegalArgumentException} otherwise. For that reason, {@link #get(Class)} and
   * {@link #get(Class)} should be preferred as they can correctly create {@link ClassName}
   * instances without such restrictions.
   */
  public static ClassName bestGuess(String classNameString) {
    List<String> names = new ArrayList<>();

    // Add the package name, like "java.util.concurrent", or "" for no package.
    int p = 0;
    while (p < classNameString.length() && Ascii.isLowerCase(classNameString.charAt(p))) {
      p = classNameString.indexOf('.', p) + 1;
      checkArgument(p != 0, "couldn't make a guess for %s", classNameString);
    }
    names.add(p != 0 ? classNameString.substring(0, p - 1) : "");

    // Add the class names, like "Map" and "Entry".
    for (String part : Splitter.on('.').split(classNameString.substring(p))) {
      checkArgument(!part.isEmpty() && Ascii.isUpperCase(part.charAt(0)),
          "couldn't make a guess for %s", classNameString);
      names.add(part);
    }

    checkArgument(names.size() >= 2, "couldn't make a guess for %s", classNameString);
    return new ClassName(names);
  }

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * {@code "java.util"} and simple names {@code "Map"}, {@code "Entry"} yields {@link Map.Entry}.
   */
  public static ClassName get(String packageName, String simpleName, String... simpleNames) {
    return new ClassName(new ImmutableList.Builder<String>()
        .add(packageName)
        .add(simpleName)
        .add(simpleNames)
        .build());
  }

  private static final ImmutableSet<NestingKind> ACCEPTABLE_NESTING_KINDS =
      Sets.immutableEnumSet(TOP_LEVEL, MEMBER);

  /** Returns the class name for {@code element}. */
  public static ClassName get(TypeElement element) {
    checkNotNull(element, "element == null");
    List<String> names = new ArrayList<>();
    for (Element e = element; isClassOrInterface(e); e = e.getEnclosingElement()) {
      checkArgument(ACCEPTABLE_NESTING_KINDS.contains(element.getNestingKind()));
      names.add(e.getSimpleName().toString());
    }
    names.add(getPackage(element).getQualifiedName().toString());
    Collections.reverse(names);
    return new ClassName(names);
  }

  private static boolean isClassOrInterface(Element e) {
    return e.getKind().isClass() || e.getKind().isInterface();
  }

  private static PackageElement getPackage(Element type) {
    while (type.getKind() != ElementKind.PACKAGE) {
      type = type.getEnclosingElement();
    }
    return (PackageElement) type;
  }

  @Override public boolean equals(Object o) {
    return o instanceof ClassName
        && canonicalName.equals(((ClassName) o).canonicalName);
  }

  @Override public int hashCode() {
    return canonicalName.hashCode();
  }

  @Override public int compareTo(ClassName o) {
    return canonicalName.compareTo(o.canonicalName);
  }

  @Override public String toString() {
    return canonicalName;
  }
}
