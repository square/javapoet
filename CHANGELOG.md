Change Log
==========

JavaPoet 1.0.0 *(unreleased)*
----------------------------

 * Project name is now `javapoet`. We renamed the project so you could simultaneously
   use the old JavaWriter API and our new builder-based APIs in one project.
 * Package name is now `com.squareup.javapoet`.
 * Builders.
 * Automatic imports.


JavaWriter 2.5.1 *(2014-12-03)*
----------------------------

 * New: `StringLiteral` type which encapsulates the behavior of `stringLiteral`.
 * Fix: Use canonical name when emitting a class import.
 * Fix: Apply type compression to varargs and array types.
 * Fix: Restore binary compatibility with pre-2.5 versions.


JavaWriter 2.5.0 *(2014-04-18)*
----------------------------

 * New: Methods in interfaces will always have no body declaration.
 * New: Control flow begin declaration now supports String format arguments.
 * Fix: Truncate any generic type when emitting constructors.
 * Fix: Do not emit trailing whitespace after '=' at end-of-line.


JavaWriter 2.4.0 *(2014-01-10)*
----------------------------

 * New: Properly indent hanging lines in field initializers.
 * New: `emitEnumValue` variant which exposes a boolean of whether the current value is the last.


JavaWriter 2.3.1 *(2013-12-16)*
----------------------------

 * Fix: Properly handle subpackages of `java.lang` in `compressType`.


JavaWriter 2.3.0 *(2013-11-24)*
----------------------------

 * New: Configurable indent level via `setIndent`.
 * New: `beginConstructor` method is a semantically clearer alternative for constructors.
 * New: `emitEnumValues` method emits a list of values followed by semicolon.
 * `emitImports` now supports `Class` arguments directly.
 * Previously-deprecated, `int`-based modifier methods have been removed.


JavaWriter 2.2.1 *(2013-10-23)*
----------------------------

 * Fix: Do not emit trailing whitespace for empty Javadoc lines.


JavaWriter 2.2.0 *(2013-09-25)*
----------------------------

 * `setCompressingTypes` controls whether types are emitted as fully-qualified or not.


JavaWriter 2.1.2 *(2013-08-23)*
----------------------------

 * Attempt to keep annotations on a single line.


JavaWriter 2.1.1 *(2013-07-23)*
----------------------------

 * Fix: `stringLiteral` now correctly handles escapes and control characters.


JavaWriter 2.1.0 *(2013-07-15)*
----------------------------

 * New: All methods now take a `Set` of `Modifier`s rather than an `int`. The `int` methods are
   now deprecated for removal in JavaPoet 1.0.
 * Annotations with a single "value" attribute will now omit the key.


JavaWriter 2.0.1 *(2013-06-17)*
----------------------------

 * Correct casing of `emitSingleLineComment`.


JavaWriter 2.0.0 *(2013-06-06)*
----------------------------

 * Package name is now `com.squareup.javawriter`.
 * Support declaring `throws` clause on methods.


JavaWriter 1.0.5 *(2013-05-08)*
----------------------------

 * Fix: Fully qualify types whose simple name matches an import.


JavaWriter 1.0.4 *(2013-03-15)*
----------------------------

 * Fix: Static import emit now properly supports method imports.


JavaWriter 1.0.3 *(2013-02-21)*
-----------------------------

 * Add support for emitting static imports.


JavaWriter 1.0.2 *(2013-02-11)*
----------------------------

 * Add `type` API for helping build generic types.
 * Minor performance improvements.


JavaWriter 1.0.1 *(2013-02-03)*
----------------------------

 * Expose `compressType` API.


JavaWriter 1.0.0 *(2013-02-01)*
----------------------------

Initial release.
