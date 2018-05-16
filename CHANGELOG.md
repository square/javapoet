Change Log
==========

JavaPoet 1.11.1 *(2018-05-16)*
-----------------------------

 * Fix: JavaPoet 1.11 had a regression where `TypeName.get()` would throw on error types, masking
   other errors in an annotation processing round. This is fixed with a test to prevent future
   regressions!


JavaPoet 1.11.0 *(2018-04-29)*
-----------------------------

 * New: Support `TYPE_USE` annotations on each enclosing `ClassName`.
 * New: Work around a compiler bug in `TypeName.get(TypeElement)`. There was a problem getting an
   element's kind when building from source ABIs.


JavaPoet 1.10.0 *(2018-01-27)*
-----------------------------

 * **JavaPoet now requires Java 8 or newer.**
 * New: `$Z` as an optional newline (zero-width space) if a line may exceed 100 chars.
 * New: `CodeBlock.join()` and `CodeBlock.joining()` let you join codeblocks by delimiters.
 * New: Add `CodeBlock.Builder.isEmpty()`.
 * New: `addStatement(CodeBlock)` overloads for `CodeBlock` and `MethodSpec`.
 * Fix: Include annotations when emitting type variables.
 * Fix: Use the right imports for annotated type parameters.
 * Fix: Don't incorrectly escape classnames that start with `$`.


JavaPoet 1.9.0 *(2017-05-13)*
-----------------------------

 * Fix: Don't emit incorrect code when the declared type's signature references another type with
   the same simple name.
 * Fix: Support anonymous inner classes in `ClassName.get()`.
 * New: `MethodSpec.Builder.addNamedCode()` and `TypeSpec.anonymousClassBuilder(CodeBlock)`.


JavaPoet 1.8.0 *(2016-11-09)*
-----------------------------

 * New: Basic support for line wrapping. Use `$W` to insert a Wrappable Whitespace character. It'll
   emit either a single space or a newline with appropriate indentation.
 * New: Named arguments in `CodeBlock`. These are intended to make larger code snippets easier to
   read:

   ```
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("count", 3);
    map.put("greeting", "Hello, ");
    map.put("system", System.class);

    String template = ""
        + "for (int i = 0; i < $count:L; i++) {\n"
        + "  $system:T.out.println($greeting:S + list.get(i));\n"
        + "}\n";

    CodeBlock.Builder builder = CodeBlock.builder();
    builder.addNamed(template, map);
   ```

 * New: `addJavadoc(CodeBlock)` overloads for TypeSpec, MethodSpec, and FieldSpec.
 * New: `MethodSpec.addComment()` makes it easy to add a `// single-line comment.`
 * New: `ClassName.getReflectionName()` returns a string like `java.util.Map$Entry`.
 * Fix: Always write UTF-8. Previously JavaPoet would use the system default charset which was
   potentially inconsistent across environments.
 * Fix: Permit (constant) fields to be defined in annotation types.


JavaPoet 1.7.0 *(2016-04-26)*
-----------------------------

 * New: Support parameterized types that enclose other types, like `Outer<String>.Inner`.
 * New: `TypeName.isBoxedPrimitive()`.


JavaPoet 1.6.1 *(2016-03-21)*
-----------------------------

 * Fix: Double quotes and backslashes in string literals were not properly quoted in 1.6.0. This is
   now fixed.


JavaPoet 1.6.0 *(2016-03-19)*
-----------------------------

 * New: Revive `CodeBlock.of()`, a handy factory method for building code blocks.
 * New: Add `TypeSpec` factory methods that take a `ClassName`.
 * New: `TypeName.annotated()` adds an annotation to a type.
 * New: `TypeVariableName.withBounds()` adds bounds to a type variable.
 * New: `TypeSpec.Builder.addInitializerBlock()` adds an instance initializer.
 * Fix: Make `TypeSpec.Kind` enum public. This can be used to check if a `TypeSpec` is a class,
   interface, enum, or annotation.
 * Fix: Don’t break import resolution on annotated types.
 * Fix: Forbid unexpected modifiers like `private` on annotation members.
 * Fix: Deduplicate exceptions in `MethodSpec.Builder`.
 * Fix: Treat `ErrorType` like a regular `DeclaredType` in `TypeName.get()`. This should make it
   easier to write annotation processors.


JavaPoet 1.5.1 *(2016-01-10)*
-----------------------------

 * Fix: Annotated `TypeName` instances are only equal if their annotations are equal.

JavaPoet 1.5.0 *(2016-01-10)*
-----------------------------

 * New: `import static`! See `JavaFile.Builder.addStaticImport()` variants.
 * New: Overload `NameAllocator.newName(String)` for creating a one-off name without a tag.
 * Fix: AnnotationSpec escapes character literals properly.
 * Fix: Don't stack overflow when `TypeVariableName` is part of `ParameterizedTypeName`.
 * Fix: Reporting not used indexed arguments in like `add("$1S", "a", "b")`.
 * Fix: Prevent import of types located in the default package, i.e. have no package name.


JavaPoet 1.4.0 *(2015-11-13)*
-----------------------------

 * New: `AnnotationSpec.get(Annotation)`.
 * New: Type annotations! `TypeName.annotated()` can attach annotations like `@Nullable` directly to
   types. This works for both top-level types and type parameters as in `List<@Nullable String>`.
 * New: `equals()` and `hashCode()` on `AnnotationSpec`, `CodeBlock`, `FieldSpec`, `JavaFile`,
   `MethodSpec`, `ParameterSpec`, `TypeName`, and `TypeSpec`.
 * New: `NameAllocator.clone()` to refine a NameAllocator for use in an inner scope code block.
 * Fix: Don't stack overflow when `TypeVariableName` gets a self-referential type.
 * Fix: Better handling of name collisions on imports. Previously JavaPoet did the wrong thing when
   a referenced type and a nested types had the same name.


JavaPoet 1.3.0 *(2015-09-20)*
-----------------------------

 * New: `NameAllocator` API makes it easy to declare non-conflicting names.
 * New: Support annotations on enum values.
 * Fix: Avoid infinite recursion in `TypeName.get(TypeMirror)`.
 * Fix: Use qualified name for conflicting simple names in the same file.
 * Fix: Better messages for parameter indexing errors.


JavaPoet 1.2.0 *(2015-07-04)*
-----------------------------

 * New: Arguments may have positional indexes like `$1T` and `$2N`. Indexes can be used to refer to
   the same argument multiple times in a single format string.
 * New: Permit Javadoc on enum constants.
 * New: Class initializer blocks with `addStaticBlock()`.
 * Fix: `MethodSpec.overriding()` retains annotations.


JavaPoet 1.1.0 *(2015-05-25)*
-----------------------------

 * New: Eager validation of argument types like `$T` and `$N`.
 * New: `MethodSpec.varargs(boolean)` to generate varags methods.
 * New: `AnnotationSpec.get()` and `MethodSpec.overriding()` to create annotations and methods from
   the `javax.lang.model` API.
 * New: `JavaFile.toJavaFileObject()`.
 * New: Java 8 `DEFAULT` modifier.
 * New: `toBuilder()` methods to build upon objects already constructed.
 * New: Generate `@interface` annotation types.
 * New: `TypeName.box()` and `TypeName.unbox()` convenience APIs.
 * Fix: `nextControlFlow()` accepts arguments.
 * Fix: Reject duplicate calls to set the superclass.
 * Fix: `WildcardTypeName.get(WildcardType)` no longer throws a `NullPointerException`.
 * Fix: Don't allow double field initialization.

JavaPoet 1.0.0 *(2015-01-28)*
-----------------------------

 * This update is a complete rewrite. The project name is now `javapoet`. We renamed the it so you
   can simultaneously use the old JavaWriter API and our new builder-based APIs in one project.
 * Immutable value objects and builders. Instead of streaming the `.java` file from top to bottom,
   you now define members in whatever way is convenient.
 * We now use our own models for type names.
 * Imports are now added automatically.


JavaWriter 2.5.1 *(2014-12-03)*
-------------------------------

 * New: `StringLiteral` type which encapsulates the behavior of `stringLiteral`.
 * Fix: Use canonical name when emitting a class import.
 * Fix: Apply type compression to varargs and array types.
 * Fix: Restore binary compatibility with pre-2.5 versions.


JavaWriter 2.5.0 *(2014-04-18)*
-------------------------------

 * New: Methods in interfaces will always have no body declaration.
 * New: Control flow begin declaration now supports String format arguments.
 * Fix: Truncate any generic type when emitting constructors.
 * Fix: Do not emit trailing whitespace after '=' at end-of-line.


JavaWriter 2.4.0 *(2014-01-10)*
-------------------------------

 * New: Properly indent hanging lines in field initializers.
 * New: `emitEnumValue` variant which exposes a boolean of whether the current value is the last.


JavaWriter 2.3.1 *(2013-12-16)*
-------------------------------

 * Fix: Properly handle subpackages of `java.lang` in `compressType`.


JavaWriter 2.3.0 *(2013-11-24)*
-------------------------------

 * New: Configurable indent level via `setIndent`.
 * New: `beginConstructor` method is a semantically clearer alternative for constructors.
 * New: `emitEnumValues` method emits a list of values followed by semicolon.
 * `emitImports` now supports `Class` arguments directly.
 * Previously-deprecated, `int`-based modifier methods have been removed.


JavaWriter 2.2.1 *(2013-10-23)*
-------------------------------

 * Fix: Do not emit trailing whitespace for empty Javadoc lines.


JavaWriter 2.2.0 *(2013-09-25)*
-------------------------------

 * `setCompressingTypes` controls whether types are emitted as fully-qualified or not.


JavaWriter 2.1.2 *(2013-08-23)*
-------------------------------

 * Attempt to keep annotations on a single line.


JavaWriter 2.1.1 *(2013-07-23)*
-------------------------------

 * Fix: `stringLiteral` now correctly handles escapes and control characters.


JavaWriter 2.1.0 *(2013-07-15)*
-------------------------------

 * New: All methods now take a `Set` of `Modifier`s rather than an `int`. The `int` methods are
   now deprecated for removal in JavaPoet 1.0.
 * Annotations with a single "value" attribute will now omit the key.


JavaWriter 2.0.1 *(2013-06-17)*
-------------------------------

 * Correct casing of `emitSingleLineComment`.


JavaWriter 2.0.0 *(2013-06-06)*
-------------------------------

 * Package name is now `com.squareup.javawriter`.
 * Support declaring `throws` clause on methods.


JavaWriter 1.0.5 *(2013-05-08)*
-------------------------------

 * Fix: Fully qualify types whose simple name matches an import.


JavaWriter 1.0.4 *(2013-03-15)*
-------------------------------

 * Fix: Static import emit now properly supports method imports.


JavaWriter 1.0.3 *(2013-02-21)*
-------------------------------

 * Add support for emitting static imports.


JavaWriter 1.0.2 *(2013-02-11)*
-------------------------------

 * Add `type` API for helping build generic types.
 * Minor performance improvements.


JavaWriter 1.0.1 *(2013-02-03)*
-------------------------------

 * Expose `compressType` API.


JavaWriter 1.0.0 *(2013-02-01)*
-------------------------------

Initial release.
