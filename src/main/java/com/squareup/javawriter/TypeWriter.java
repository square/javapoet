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
package com.squareup.javawriter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Only named types. Doesn't cover anonymous inner classes.
 */
public abstract class TypeWriter /* ha ha */ extends Modifiable
    implements Writable, HasTypeName, HasClassReferences {
  final ClassName name;
  final List<TypeName> implementedTypes;
  final List<MethodWriter> methodWriters;
  final List<TypeWriter> nestedTypeWriters;
  final Map<String, FieldWriter> fieldWriters;

  TypeWriter(ClassName name) {
    this.name = name;
    this.implementedTypes = Lists.newArrayList();
    this.methodWriters = Lists.newArrayList();
    this.nestedTypeWriters = Lists.newArrayList();
    this.fieldWriters = Maps.newLinkedHashMap();
  }

  @Override
  public ClassName name() {
    return name;
  }

  public MethodWriter addMethod(TypeWriter returnType, String name) {
    MethodWriter methodWriter = new MethodWriter(returnType.name, name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(TypeMirror returnType, String name) {
    MethodWriter methodWriter =
        new MethodWriter(TypeNames.forTypeMirror(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(TypeName returnType, String name) {
    MethodWriter methodWriter = new MethodWriter(returnType, name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public MethodWriter addMethod(Class<?> returnType, String name) {
    MethodWriter methodWriter =
        new MethodWriter(TypeNames.forClass(returnType), name);
    methodWriters.add(methodWriter);
    return methodWriter;
  }

  public ClassWriter addNestedClass(String name) {
    ClassWriter innerClassWriter = new ClassWriter(this.name.nestedClassNamed(name));
    nestedTypeWriters.add(innerClassWriter);
    return innerClassWriter;
  }

  public InterfaceWriter addNestedInterface(String name) {
    InterfaceWriter innerInterfaceWriter = new InterfaceWriter(this.name.nestedClassNamed(name));
    nestedTypeWriters.add(innerInterfaceWriter);
    return innerInterfaceWriter;
  }

  public EnumWriter addNestedEnum(String name) {
    EnumWriter innerEnumWriter = new EnumWriter(this.name.nestedClassNamed(name));
    nestedTypeWriters.add(innerEnumWriter);
    return innerEnumWriter;
  }

  public void addImplementedType(TypeName typeReference) {
    implementedTypes.add(typeReference);
  }

  public void addImplementedType(TypeElement typeElement) {
    implementedTypes.add(ClassName.fromTypeElement(typeElement));
  }

  public FieldWriter addField(Class<?> type, String name) {
    return addField(TypeNames.forClass(type), name);
  }

  public FieldWriter addField(TypeElement type, String name) {
    return addField(ClassName.fromTypeElement(type), name);
  }

  public FieldWriter addField(TypeName type, String name) {
    String candidateName = name;
    int differentiator = 1;
    while (fieldWriters.containsKey(candidateName)) {
      candidateName = name + differentiator;
      differentiator++;
    }
    FieldWriter fieldWriter = new FieldWriter(type, candidateName);
    fieldWriters.put(candidateName, fieldWriter);
    return fieldWriter;
  }

  @Override public final String toString() {
    try {
      return writeTypeToAppendable(new StringBuilder()).toString();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  Appendable writeTypeToAppendable(Appendable appendable) throws IOException {
    String packageName = name().packageName();
    appendable.append("package ").append(packageName).append(";\n\n");

    ImmutableSortedSet<ClassName> importCandidates = ImmutableSortedSet.<ClassName>naturalOrder()
        //.addAll(explicitImports) // TODO!
        .addAll(referencedClasses())
        .build();

    ImmutableSet.Builder<String> declaredSimpleNamesBuilder = ImmutableSet.builder();
    Deque<TypeWriter> declaredTypes = Queues.newArrayDeque(ImmutableSet.of(this));
    while (!declaredTypes.isEmpty()) {
      TypeWriter currentType = declaredTypes.pop();
      declaredSimpleNamesBuilder.add(currentType.name().simpleName());
      declaredTypes.addAll(currentType.nestedTypeWriters);
    }

    ImmutableSet<String> declaredSimpleNames = declaredSimpleNamesBuilder.build();

    BiMap<String, ClassName> importedClassIndex = HashBiMap.create();
    for (ClassName className : importCandidates) {
      if (!(className.packageName().equals(packageName)
          && !className.enclosingClassName().isPresent())
          && !(className.packageName().equals("java.lang")
          && className.enclosingSimpleNames().isEmpty())
          && !name().equals(className.topLevelClassName())) {
        Optional<ClassName> importCandidate = Optional.of(className);
        while (importCandidate.isPresent()
            && (importedClassIndex.containsKey(importCandidate.get().simpleName())
            || declaredSimpleNames.contains(importCandidate.get().simpleName()))) {
          importCandidate = importCandidate.get().enclosingClassName();
        }
        if (importCandidate.isPresent()) {
          appendable.append("import ").append(importCandidate.get().canonicalName()).append(";\n");
          importedClassIndex.put(importCandidate.get().simpleName(), importCandidate.get());
        }
      }
    }

    if (!importedClassIndex.isEmpty()) {
      appendable.append('\n');
    }

    CompilationUnitContext context =
        new CompilationUnitContext(packageName, ImmutableSet.copyOf(importedClassIndex.values()));
    write(appendable, context.createSubcontext(ImmutableSet.of(name())));

    return appendable;
  }

  static final class CompilationUnitContext implements Context {
    private final String packageName;
    private final ImmutableSortedSet<ClassName> visibleClasses;

    CompilationUnitContext(String packageName, Set<ClassName> visibleClasses) {
      this.packageName = packageName;
      this.visibleClasses =
          ImmutableSortedSet.copyOf(Ordering.natural().reverse(), visibleClasses);
    }

    @Override
    public Context createSubcontext(Set<ClassName> newTypes) {
      return new CompilationUnitContext(packageName, Sets.union(visibleClasses, newTypes));
    }

    @Override
    public String sourceReferenceForClassName(ClassName className) {
      if (isImported(className)) {
        return className.simpleName();
      }
      Optional<ClassName> enclosingClassName = className.enclosingClassName();
      while (enclosingClassName.isPresent()) {
        if (isImported(enclosingClassName.get())) {
          return enclosingClassName.get().simpleName()
              + className.canonicalName()
                  .substring(enclosingClassName.get().canonicalName().length());
        }
        enclosingClassName = enclosingClassName.get().enclosingClassName();
      }
      return className.canonicalName();
    }

    private boolean collidesWithVisibleClass(ClassName className) {
      return collidesWithVisibleClass(className.simpleName());
    }

    private boolean collidesWithVisibleClass(String simpleName) {
      return FluentIterable.from(visibleClasses)
          .transform(new Function<ClassName, String>() {
            @Override public String apply(ClassName input) {
              return input.simpleName();
            }
          })
          .contains(simpleName);
    }

    private boolean isImported(ClassName className) {
      return (packageName.equals(className.packageName())
              && !className.enclosingClassName().isPresent()
              && !collidesWithVisibleClass(className)) // need to account for scope & hiding
          || visibleClasses.contains(className)
          || (className.packageName().equals("java.lang")
              && className.enclosingSimpleNames().isEmpty());
    }
  }
}
