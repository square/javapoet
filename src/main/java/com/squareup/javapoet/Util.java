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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * Like Guava, but worse and standalone. This makes it easier to mix JavaPoet with libraries that
 * bring their own version of Guava.
 */
final class Util {
  private Util() {
  }

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

  public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
    Set<Set<T>> sets = new LinkedHashSet<>();
    if (originalSet.isEmpty()) {
      sets.add(new LinkedHashSet<T>());
      return sets;
    }
    List<T> list = new ArrayList<T>(originalSet);
    T head = list.get(0);
    Set<T> rest = new LinkedHashSet<T>(list.subList(1, list.size()));
    for (Set<T> set : powerSet(rest)) {
      Set<T> newSet = new LinkedHashSet<T>();
      newSet.add(head);
      newSet.addAll(set);
      sets.add(newSet);
      sets.add(set);
    }
    return sets;
}

  /**
   * Finds a subset of {@code set} in {@code modifiers}. Ignores empty subsets,
   * and if {@code set} is empty then {@code true} is always returned.
   *
   * @param modifiers
   *          - The input modifiers
   * @param set
   *          - The set of modifiers to make subsets of
   */
  public static void requireSubsetOf(Set<Modifier> set, Set<Modifier> modifiers) {
    Set<Set<Modifier>> powerSet = powerSet(set);
    boolean containsSubset = set.isEmpty();
    for (Set<Modifier> subset : powerSet) {
      if (subset.isEmpty()) {
        continue;
      }
      containsSubset |= modifiers.containsAll(subset);
    }
    checkState(containsSubset, "%s should contain a subset of %s", modifiers, set);
  }

  public static void requireExactlyOneOf(Set<Modifier> set, Set<Modifier> modifiers) {
    boolean containsOne = false;
    boolean containsMany = false;
    for (Modifier check : set) {
      boolean containsCheck = modifiers.contains(check);
      containsMany = containsOne && containsCheck;
      if (containsMany) {
        break;
      }
      containsOne |= containsCheck;
    }
    checkState(containsOne, "%s must contain one of %s", modifiers, set);
    checkState(!containsMany, "%s must contain only one of %s", modifiers, set);
  }

  public static boolean hasDefaultModifier(Collection<Modifier> modifiers) {
    return DEFAULT != null && modifiers.contains(DEFAULT);
  }
}
