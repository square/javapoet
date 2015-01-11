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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final DeclarationType declarationType;
  public final String name;
  public final Snippet anonymousTypeArguments;
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final ImmutableList<TypeVariable<?>> typeVariables;
  public final Type superclass;
  public final ImmutableList<Type> superinterfaces;
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

    this.declarationType = checkNotNull(builder.declarationType);
    this.name = builder.name;
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.typeVariables = ImmutableList.copyOf(builder.typeVariables);
    this.superclass = builder.superclass;
    this.superinterfaces = ImmutableList.copyOf(builder.superinterfaces);
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

  public static Builder classBuilder(String name) {
    return new Builder(DeclarationType.CLASS, name, null);
  }

  public static Builder interfaceBuilder(String name) {
    return new Builder(DeclarationType.INTERFACE, name, null);
  }

  public static Builder enumBuilder(String name) {
    return new Builder(DeclarationType.ENUM, name, null);
  }

  public static Builder anonymousClassBuilder(String typeArgumentsFormat, Object... args) {
    return new Builder(DeclarationType.CLASS, null, new Snippet(typeArgumentsFormat, args));
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
    private final DeclarationType declarationType;
    private final String name;
    private final Snippet anonymousTypeArguments;

    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<TypeVariable<?>> typeVariables = new ArrayList<>();
    private Type superclass = ClassName.OBJECT;
    private final List<Type> superinterfaces = new ArrayList<>();
    private final Map<String, TypeSpec> enumConstants = new LinkedHashMap<>();
    private final List<FieldSpec> fieldSpecs = new ArrayList<>();
    private final List<MethodSpec> methodSpecs = new ArrayList<>();
    private final List<TypeSpec> typeSpecs = new ArrayList<>();
    private final List<Element> originatingElements = new ArrayList<>();

    private Builder(DeclarationType declarationType, String name,
        Snippet anonymousTypeArguments) {
      this.declarationType = declarationType;
      this.name = name;
      this.anonymousTypeArguments = anonymousTypeArguments;
    }

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

    public Builder addEnumConstant(String name) {
      checkState(declarationType == DeclarationType.ENUM);
      return addEnumConstant(name, anonymousClassBuilder("").build());
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
