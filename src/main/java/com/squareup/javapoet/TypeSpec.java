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

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final Kind kind;
  public final String name;
  public final CodeBlock anonymousTypeArguments;
  public final CodeBlock javadoc;
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
    this.kind = builder.kind;
    this.name = builder.name;
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.javadoc = builder.javadoc.build();
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
    return new Builder(Kind.CLASS, checkNotNull(name, "name == null"), null);
  }

  public static Builder interfaceBuilder(String name) {
    return new Builder(Kind.INTERFACE, checkNotNull(name, "name == null"), null);
  }

  public static Builder enumBuilder(String name) {
    return new Builder(Kind.ENUM, checkNotNull(name, "name == null"), null);
  }

  public static Builder anonymousClassBuilder(String typeArgumentsFormat, Object... args) {
    return new Builder(Kind.CLASS, null, CodeBlock.builder()
        .add(typeArgumentsFormat, args)
        .build());
  }

  void emit(CodeWriter codeWriter, String enumName, Set<Modifier> implicitModifiers)
      throws IOException {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    int previousStatementLine = codeWriter.statementLine;
    codeWriter.statementLine = -1;

    try {
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
        codeWriter.emitJavadoc(javadoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emitModifiers(modifiers, Sets.union(implicitModifiers, kind.asMemberModifiers));
        codeWriter.emit("$L $L", Ascii.toLowerCase(kind.name()), name);
        codeWriter.emitTypeVariables(typeVariables);

        List<Type> extendsTypes;
        List<Type> implementsTypes;
        if (kind == Kind.INTERFACE) {
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
        enumConstant.getValue()
            .emit(codeWriter, enumConstant.getKey(), ImmutableSet.<Modifier>of());
        firstMember = false;
        if (i.hasNext()) {
          codeWriter.emit(",\n");
        } else if (!fieldSpecs.isEmpty() || !methodSpecs.isEmpty() || !typeSpecs.isEmpty()) {
          codeWriter.emit(";\n");
        } else {
          codeWriter.emit("\n");
        }
      }

      // Static fields.
      for (FieldSpec fieldSpec : fieldSpecs) {
        if (!fieldSpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        fieldSpec.emit(codeWriter, kind.implicitFieldModifiers);
        firstMember = false;
      }

      // Non-static fields.
      for (FieldSpec fieldSpec : fieldSpecs) {
        if (fieldSpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        fieldSpec.emit(codeWriter, kind.implicitFieldModifiers);
        firstMember = false;
      }

      // Constructors.
      for (MethodSpec methodSpec : methodSpecs) {
        if (!methodSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        methodSpec.emit(codeWriter, name, kind.implicitMethodModifiers);
        firstMember = false;
      }

      // Methods (static and non-static).
      for (MethodSpec methodSpec : methodSpecs) {
        if (methodSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        methodSpec.emit(codeWriter, name, kind.implicitMethodModifiers);
        firstMember = false;
      }

      // Types.
      for (TypeSpec typeSpec : typeSpecs) {
        if (!firstMember) codeWriter.emit("\n");
        typeSpec.emit(codeWriter, null, kind.implicitTypeModifiers);
        firstMember = false;
      }

      codeWriter.unindent();
      codeWriter.popType();

      codeWriter.emit("}");
      if (enumName == null && anonymousTypeArguments == null) {
        codeWriter.emit("\n"); // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.statementLine = previousStatementLine;
    }
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, null, ImmutableSet.<Modifier>of());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  private enum Kind {
    CLASS(
        ImmutableSet.<Modifier>of(),
        ImmutableSet.<Modifier>of(),
        ImmutableSet.<Modifier>of(),
        ImmutableSet.<Modifier>of()),

    INTERFACE(
        ImmutableSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
        ImmutableSet.of(Modifier.PUBLIC, Modifier.ABSTRACT),
        ImmutableSet.of(Modifier.PUBLIC, Modifier.STATIC),
        ImmutableSet.of(Modifier.STATIC)),

    ENUM(
        ImmutableSet.<Modifier>of(),
        ImmutableSet.<Modifier>of(),
        ImmutableSet.<Modifier>of(),
        ImmutableSet.of(Modifier.STATIC));

    private final ImmutableSet<Modifier> implicitFieldModifiers;
    private final ImmutableSet<Modifier> implicitMethodModifiers;
    private final ImmutableSet<Modifier> implicitTypeModifiers;
    private final ImmutableSet<Modifier> asMemberModifiers;

    private Kind(ImmutableSet<Modifier> implicitFieldModifiers,
        ImmutableSet<Modifier> implicitMethodModifiers,
        ImmutableSet<Modifier> implicitTypeModifiers,
        ImmutableSet<Modifier> asMemberModifiers) {
      this.implicitFieldModifiers = implicitFieldModifiers;
      this.implicitMethodModifiers = implicitMethodModifiers;
      this.implicitTypeModifiers = implicitTypeModifiers;
      this.asMemberModifiers = asMemberModifiers;
    }
  }

  public static final class Builder {
    private final Kind kind;
    private final String name;
    private final CodeBlock anonymousTypeArguments;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
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

    private Builder(Kind kind, String name,
        CodeBlock anonymousTypeArguments) {
      checkArgument(name == null || SourceVersion.isName(name), "not a valid name: %s", name);
      this.kind = kind;
      this.name = name;
      this.anonymousTypeArguments = anonymousTypeArguments;
    }

    public Builder addJavadoc(String format, Object... args) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      javadoc.add(format, args);
      return this;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Type annotation) {
      return addAnnotation(AnnotationSpec.builder(annotation).build());
    }

    public Builder addModifiers(Modifier... modifiers) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder addTypeVariable(TypeVariable<?> typeVariable) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
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
      return addEnumConstant(name, anonymousClassBuilder("").build());
    }

    public Builder addEnumConstant(String name, TypeSpec typeSpec) {
      checkState(kind == Kind.ENUM, "%s is not enum", this.name);
      checkArgument(typeSpec.anonymousTypeArguments != null,
          "enum constants must have anonymous type arguments");
      checkArgument(SourceVersion.isName(name), "not a valid enum constant: %s", name);
      enumConstants.put(name, typeSpec);
      return this;
    }

    public Builder addField(FieldSpec fieldSpec) {
      checkArgument(fieldSpec.modifiers.containsAll(kind.implicitFieldModifiers),
          "%s %s.%s requires modifiers %s", kind, name, fieldSpec.name,
          kind.implicitFieldModifiers);
      fieldSpecs.add(fieldSpec);
      return this;
    }

    public Builder addField(Type type, String name, Modifier... modifiers) {
      return addField(FieldSpec.builder(type, name, modifiers).build());
    }

    public Builder addMethod(MethodSpec methodSpec) {
      checkArgument(methodSpec.modifiers.containsAll(kind.implicitMethodModifiers),
          "%s %s.%s requires modifiers %s", kind, name, methodSpec.name,
          kind.implicitMethodModifiers);
      methodSpecs.add(methodSpec);
      return this;
    }

    public Builder addType(TypeSpec typeSpec) {
      checkArgument(typeSpec.modifiers.containsAll(kind.implicitTypeModifiers),
          "%s %s.%s requires modifiers %s", kind, name, typeSpec.name,
          kind.implicitFieldModifiers);
      typeSpecs.add(typeSpec);
      return this;
    }

    public Builder addOriginatingElement(Element originatingElement) {
      originatingElements.add(originatingElement);
      return this;
    }

    public TypeSpec build() {
      checkArgument(kind != Kind.ENUM || !enumConstants.isEmpty(),
          "at least one enum constant is required for %s", name);

      boolean isAbstract = modifiers.contains(Modifier.ABSTRACT) || kind != Kind.CLASS;
      for (MethodSpec methodSpec : methodSpecs) {
        checkArgument(isAbstract || !methodSpec.hasModifier(Modifier.ABSTRACT),
            "non-abstract type %s cannot declare abstract method %s", name, methodSpec.name);
      }

      boolean superclassIsObject = superclass.equals(ClassName.OBJECT);
      int interestingSupertypeCount = (superclassIsObject ? 0 : 1) + superinterfaces.size();
      checkArgument(anonymousTypeArguments == null || interestingSupertypeCount <= 1,
          "anonymous type has too many supertypes");

      return new TypeSpec(this);
    }
  }
}
