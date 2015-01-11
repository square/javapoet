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
package com.squareup.javawriter;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final DeclarationType declarationType;
  public final String name;
  public final ImmutableList<TypeVariable<?>> typeVariables;
  public final Type superclass;
  public final ImmutableList<Type> superinterfaces;
  public final Snippet anonymousTypeArguments;
  public final ImmutableMap<String, TypeSpec> enumConstants;
  public final ImmutableList<FieldSpec> fieldSpecs;
  public final ImmutableList<MethodSpec> methodSpecs;
  public final ImmutableList<TypeSpec> typeSpecs;
  public final ImmutableList<Element> originatingElements;

  private TypeSpec(Builder builder) {
    checkArgument(builder.name != null ^ builder.anonymousTypeArguments != null,
        "types must have either a name or anonymous type arguments");
    boolean isInterface = builder.declarationType == DeclarationType.INTERFACE;
    boolean typeIsAbstract = builder.modifiers.contains(Modifier.ABSTRACT) || isInterface;
    checkArgument(builder.declarationType == DeclarationType.ENUM ^ builder.enumConstants.isEmpty(),
        "unexpected enum constants %s for type %s", builder.enumConstants, builder.declarationType);
    for (MethodSpec methodSpec : builder.methodSpecs) {
      checkArgument(typeIsAbstract || !methodSpec.hasModifier(Modifier.ABSTRACT),
          "non-abstract type %s cannot declare abstract method %s", builder.name, methodSpec.name);
      checkArgument(!isInterface || methodSpec.hasModifier(Modifier.ABSTRACT),
          "interface %s cannot declare non-abstract method %s", builder.name, methodSpec.name);
      checkArgument(!isInterface || methodSpec.hasModifier(Modifier.PUBLIC),
          "interface %s cannot declare non-public method %s", builder.name, methodSpec.name);
    }
    for (FieldSpec fieldSpec : builder.fieldSpecs) {
      if (isInterface) {
        checkArgument(fieldSpec.hasModifier(Modifier.PUBLIC)
            && fieldSpec.hasModifier(Modifier.STATIC)
            && fieldSpec.hasModifier(Modifier.FINAL),
            "interface %s field %s must be public static final", builder.name, fieldSpec.name);
      }
    }
    boolean superclassIsObject = builder.superclass.equals(ClassName.OBJECT);
    int interestingSupertypeCount = (superclassIsObject ? 0 : 1) + builder.superinterfaces.size();
    checkArgument(builder.anonymousTypeArguments == null || interestingSupertypeCount <= 1,
        "anonymous type has too many supertypes");

    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.declarationType = checkNotNull(builder.declarationType);
    this.name = builder.name;
    this.typeVariables = ImmutableList.copyOf(builder.typeVariables);
    this.superclass = builder.superclass;
    this.superinterfaces = ImmutableList.copyOf(builder.superinterfaces);
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.enumConstants = ImmutableMap.copyOf(builder.enumConstants);
    this.fieldSpecs = ImmutableList.copyOf(builder.fieldSpecs);
    this.methodSpecs = ImmutableList.copyOf(builder.methodSpecs);
    this.typeSpecs = ImmutableList.copyOf(builder.typeSpecs);

    ImmutableList.Builder<Element> originatingElementsBuilder = ImmutableList.builder();
    originatingElementsBuilder.addAll(builder.originatingElements);
    for (TypeSpec typeSpec : builder.typeSpecs) {
      originatingElementsBuilder.addAll(typeSpec.originatingElements);
    }
    this.originatingElements = originatingElementsBuilder.build();
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, String enumName) {
    if (enumName != null) {
      codeWriter.emit("$L", enumName);
      if (!anonymousTypeArguments.formatParts.isEmpty()) {
        codeWriter.emit("(");
        codeWriter.emit(anonymousTypeArguments);
        codeWriter.emit(")");
      }
      if (fieldSpecs.isEmpty() && methodSpecs.isEmpty() && typeSpecs.isEmpty()) {
        return; // Avoid unnecessary braces "{}".
      }
      codeWriter.emit(" {\n");
    } else if (anonymousTypeArguments != null) {
      codeWriter.emit("new $T(", getOnlyElement(superinterfaces, superclass));
      codeWriter.emit(anonymousTypeArguments);
      codeWriter.emit(") {\n");
    } else {
      codeWriter.emitAnnotations(annotations, false);
      codeWriter.emitModifiers(modifiers);
      codeWriter.emit("$L $L", Ascii.toLowerCase(declarationType.name()), name);
      codeWriter.emitTypeVariables(typeVariables);

      List<Type> extendsTypes;
      List<Type> implementsTypes;
      if (declarationType == DeclarationType.INTERFACE) {
        extendsTypes = superinterfaces;
        implementsTypes = ImmutableList.of();
      } else {
        extendsTypes = superclass.equals(ClassName.OBJECT)
            ? ImmutableList.<Type>of()
            : ImmutableList.of(superclass);
        implementsTypes = superinterfaces;
      }

      if (!extendsTypes.isEmpty()) {
        codeWriter.emit(" extends");
        boolean firstType = true;
        for (Type type : extendsTypes) {
          if (!firstType) codeWriter.emit(",");
          codeWriter.emit(" $T", type);
          firstType = false;
        }
      }

      if (!implementsTypes.isEmpty()) {
        codeWriter.emit(" implements");
        boolean firstType = true;
        for (Type type : implementsTypes) {
          if (!firstType) codeWriter.emit(",");
          codeWriter.emit(" $T", type);
          firstType = false;
        }
      }

      codeWriter.emit(" {\n");
    }

    codeWriter.pushType(this);
    codeWriter.indent();
    boolean firstMember = true;
    for (Iterator<Map.Entry<String, TypeSpec>> i = enumConstants.entrySet().iterator();
        i.hasNext();) {
      Map.Entry<String, TypeSpec> enumConstant = i.next();
      if (!firstMember) codeWriter.emit("\n");
      enumConstant.getValue().emit(codeWriter, enumConstant.getKey());
      firstMember = false;
      if (i.hasNext()) {
        codeWriter.emit(",\n");
      } else if (!fieldSpecs.isEmpty() || !methodSpecs.isEmpty() || !typeSpecs.isEmpty()) {
        codeWriter.emit(";\n");
      } else {
        codeWriter.emit("\n");
      }
    }
    for (FieldSpec fieldSpec : fieldSpecs) {
      if (!firstMember) codeWriter.emit("\n");
      fieldSpec.emit(codeWriter, declarationType.implicitFieldModifiers);
      firstMember = false;
    }
    for (MethodSpec methodSpec : methodSpecs) {
      if (!firstMember) codeWriter.emit("\n");
      methodSpec.emit(codeWriter, name, declarationType.implicitMethodModifiers);
      firstMember = false;
    }
    for (TypeSpec typeSpec : typeSpecs) {
      if (!firstMember) codeWriter.emit("\n");
      typeSpec.emit(codeWriter, null);
      firstMember = false;
    }
    codeWriter.unindent();
    codeWriter.popType();

    codeWriter.emit("}");
    if (enumName == null && anonymousTypeArguments == null) {
      codeWriter.emit("\n"); // If this type isn't also a value, include a trailing newline.
    }
  }

  private enum DeclarationType {
    CLASS(ImmutableSet.<Modifier>of(), ImmutableSet.<Modifier>of()),
    INTERFACE(ImmutableSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
        ImmutableSet.of(Modifier.PUBLIC, Modifier.ABSTRACT)),
    ENUM(ImmutableSet.<Modifier>of(), ImmutableSet.<Modifier>of());

    private final ImmutableSet<Modifier> implicitFieldModifiers;
    private final ImmutableSet<Modifier> implicitMethodModifiers;

    private DeclarationType(ImmutableSet<Modifier> implicitFieldModifiers,
        ImmutableSet<Modifier> implicitMethodModifiers) {
      this.implicitFieldModifiers = implicitFieldModifiers;
      this.implicitMethodModifiers = implicitMethodModifiers;
    }
  }

  public static final class Builder {
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private DeclarationType declarationType = DeclarationType.CLASS;
    private String name;
    private final List<TypeVariable<?>> typeVariables = new ArrayList<>();
    private Type superclass = ClassName.OBJECT;
    private final List<Type> superinterfaces = new ArrayList<>();
    private Snippet anonymousTypeArguments;
    private final Map<String, TypeSpec> enumConstants = new LinkedHashMap<>();
    private final List<FieldSpec> fieldSpecs = new ArrayList<>();
    private final List<MethodSpec> methodSpecs = new ArrayList<>();
    private final List<TypeSpec> typeSpecs = new ArrayList<>();
    private final List<Element> originatingElements = new ArrayList<>();

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Type annotation) {
      this.annotations.add(AnnotationSpec.of(annotation));
      return this;
    }

    public Builder addModifiers(Modifier... modifiers) {
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder interfaceType() {
      this.declarationType = DeclarationType.INTERFACE;
      return this;
    }

    public Builder enumType() {
      this.declarationType = DeclarationType.ENUM;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder addTypeVariable(TypeVariable<?> typeVariable) {
      typeVariables.add(typeVariable);
      return this;
    }

    public Builder superclass(Type superclass) {
      this.superclass = superclass;
      return this;
    }

    public Builder addSuperinterface(Type superinterface) {
      this.superinterfaces.add(superinterface);
      return this;
    }

    public Builder anonymousTypeArguments() {
      return anonymousTypeArguments("");
    }

    public Builder anonymousTypeArguments(String format, Object... args) {
      this.anonymousTypeArguments = new Snippet(format, args);
      return this;
    }

    public Builder addEnumConstant(String name) {
      return addEnumConstant(name, new Builder()
          .anonymousTypeArguments()
          .build());
    }

    public Builder addEnumConstant(String name, TypeSpec typeSpec) {
      checkArgument(typeSpec.anonymousTypeArguments != null,
          "enum constants must have anonymous type arguments");
      enumConstants.put(name, typeSpec);
      return this;
    }

    public Builder addField(FieldSpec fieldSpec) {
      fieldSpecs.add(fieldSpec);
      return this;
    }

    public Builder addMethod(MethodSpec methodSpec) {
      methodSpecs.add(methodSpec);
      return this;
    }

    public Builder addType(TypeSpec typeSpec) {
      typeSpecs.add(typeSpec);
      return this;
    }

    public Builder addOriginatingElement(Element originatingElement) {
      originatingElements.add(originatingElement);
      return this;
    }

    public TypeSpec build() {
      return new TypeSpec(this);
    }
  }
}
