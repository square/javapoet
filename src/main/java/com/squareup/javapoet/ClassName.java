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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

/** A fully-qualified class name for top-level and member classes. */
public final class ClassName extends TypeName implements Comparable<ClassName> {
  public static final ClassName OBJECT = ClassName.get(Object.class);

  /** From top to bottom. This will be ["java.util", "Map", "Entry"] for {@link Map.Entry}. */
  final List<String> names;
  final String canonicalName;

  private ClassName(List<String> names) {
    this(names, new ArrayList<AnnotationSpec>());
  }

  private ClassName(List<String> names, List<AnnotationSpec> annotations) {
    super(annotations);
    for (int i = 1; i < names.size(); i++) {
      checkArgument(SourceVersion.isName(names.get(i)), "part '%s' is keyword", names.get(i));
    }
    this.names = Util.immutableList(names);
    this.canonicalName = names.get(0).isEmpty()
        ? Util.join(".", names.subList(1, names.size()))
        : Util.join(".", names);
  }

  @Override public ClassName annotated(List<AnnotationSpec> annotations) {
    return new ClassName(names, concatAnnotations(annotations));
  }

  @Override public TypeName withoutAnnotations() {
    return new ClassName(names);
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
   * Returns the top class in this nesting group. Equivalent to chained calls to {@link
   * #enclosingClassName()} until the result's enclosing class is null.
   */
  public ClassName topLevelClassName() {
    return new ClassName(names.subList(0, 2));
  }

  public String reflectionName() {
    // trivial case: no nested names
    if (names.size() == 2) {
      String packageName = packageName();
      if (packageName.isEmpty()) {
        return names.get(1);
      }
      return packageName + "." + names.get(1);
    }
    // concat top level class name and nested names
    StringBuilder builder = new StringBuilder();
    builder.append(topLevelClassName());
    for (String name : simpleNames().subList(1, simpleNames().size())) {
      builder.append('$').append(name);
    }
    return builder.toString();
  }

  /**
   * Returns a new {@link ClassName} instance for the specified {@code name} as nested inside this
   * class.
   */
  public ClassName nestedClass(String name) {
    checkNotNull(name, "name == null");
    List<String> result = new ArrayList<>(names.size() + 1);
    result.addAll(names);
    result.add(name);
    return new ClassName(result);
  }

  public List<String> simpleNames() {
    return names.subList(1, names.size());
  }

  /**
   * Returns a class that shares the same enclosing package or class. If this class is enclosed by
   * another class, this is equivalent to {@code enclosingClassName().nestedClass(name)}. Otherwise
   * it is equivalent to {@code get(packageName(), name)}.
   */
  public ClassName peerClass(String name) {
    List<String> result = new ArrayList<>(names);
    result.set(result.size() - 1, name);
    return new ClassName(result);
  }

  /** Returns the simple name of this class, like {@code "Entry"} for {@link Map.Entry}. */
  public String simpleName() {
    return names.get(names.size() - 1);
  }

  public static ClassName get(Class<?> clazz) {
    checkNotNull(clazz, "clazz == null");
    checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
    checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
    checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");
    List<String> names = new ArrayList<>();
    while (true) {
      names.add(clazz.getSimpleName());
      Class<?> enclosing = clazz.getEnclosingClass();
      if (enclosing == null) break;
      clazz = enclosing;
    }
    // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
    int lastDot = clazz.getName().lastIndexOf('.');
    if (lastDot != -1) names.add(clazz.getName().substring(0, lastDot));
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
    while (p < classNameString.length() && Character.isLowerCase(classNameString.codePointAt(p))) {
      p = classNameString.indexOf('.', p) + 1;
      checkArgument(p != 0, "couldn't make a guess for %s", classNameString);
    }
    names.add(p != 0 ? classNameString.substring(0, p - 1) : "");

    // Add the class names, like "Map" and "Entry".
    for (String part : classNameString.substring(p).split("\\.", -1)) {
      checkArgument(!part.isEmpty() && Character.isUpperCase(part.codePointAt(0)),
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
    List<String> result = new ArrayList<>();
    result.add(packageName);
    result.add(simpleName);
    Collections.addAll(result, simpleNames);
    return new ClassName(result);
  }

  /** Returns the class name for {@code element}. */
  public static ClassName get(TypeElement element) {
    checkNotNull(element, "element == null");
    List<String> names = new ArrayList<>();
    for (Element e = element; isClassOrInterface(e); e = e.getEnclosingElement()) {
      checkArgument(element.getNestingKind() == TOP_LEVEL || element.getNestingKind() == MEMBER,
          "unexpected type testing");
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

  @Override public int compareTo(ClassName o) {
    return canonicalName.compareTo(o.canonicalName);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    return out.emitAndIndent(out.lookupName(this));
  }
}
