Change Log
==========

Version 2.3.1 *(2013-12-16)*
----------------------------

 * Fix: Properly handle subpackages of `java.lang` in `compressType`.


Version 2.3.0 *(2013-11-24)*
----------------------------

 * New: Configurable indent level via `setIndent`.
 * New: `beginConstructor` method is a semantically clearer alternative for constructors.
 * New: `emitEnumValues` method emits a list of values followed by semicolon.
 * `emitImports` now supports `Class` arguments directly.
 * Previously-deprecated, `int`-based modifier methods have been removed.


Version 2.2.1 *(2013-10-23)*
----------------------------

 * Fix: Do not emit trailing whitespace for empty Javadoc lines.


Version 2.2.0 *(2013-09-25)*
----------------------------

 * `setCompressingTypes` controls whether types are emitted as fully-qualified or not.


Version 2.1.2 *(2013-08-23)*
----------------------------

 * Attempt to keep annotations on a single line.


Version 2.1.1 *(2013-07-23)*
----------------------------

 * Fix: `stringLiteral` now correctly handles escapes and control characters.


Version 2.1.0 *(2013-07-15)*
----------------------------

 * New: All methods now take a `Set` of `Modifier`s rather than an `int`. The `int` methods are
   now deprecated for removal in version 3.0.
 * Annotations with a single "value" attribute will now omit the key.


Version 2.0.1 *(2013-06-17)*
----------------------------

 * Correct casing of `emitSingleLineComment`.


Version 2.0.0 *(2013-06-06)*
----------------------------

 * Package name is now `com.squareup.javawriter`.
 * Support declaring `throws` clause on methods.


Version 1.0.5 *(2013-05-08)*
----------------------------

 * Fix: Fully qualify types whose simple name matches an import.


Version 1.0.4 *(2013-03-15)*
----------------------------

 * Fix: Static import emit now properly supports method imports.


Version 1.0.3 *(2013-02-21)*
-----------------------------

 * Add support for emitting static imports.


Version 1.0.2 *(2013-02-11)*
----------------------------

 * Add `type` API for helping build generic types.
 * Minor performance improvements.


Version 1.0.1 *(2013-02-03)*
----------------------------

 * Expose `compressType` API.


Version 1.0.0 *(2013-02-01)*
----------------------------

Initial release.
