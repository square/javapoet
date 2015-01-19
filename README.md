JavaPoet
========

`JavaPoet` is a Java API for generating `.java` source files.

Source file generation can useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.


### Example

Here's a (boring) `HelloWorld` class:

```
package com.example.helloworld;

public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}
```

And this is the (exciting) code to generate it with JavaPoet:

```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(main)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.emit(System.out);
```

To declare the main method, we've created a `MethodSpec` "main" configured with modifiers, return
type, parameters and code statements. We add the main method to a `HelloWorld` class, and then add
that to a `HelloWorld.java` file.

In this case we write the file to `System.out`, but we could also get it as a string
(`JavaFile.toString()`) or write it to the file system (`JavaPoet.writeTo()`).

### Code & Control Flow

Most of JavaPoet's API uses plain old immutable Java objects. There's also builders, method chaining
and varargs to make the API friendly. JavaPoet offers models for classes & interfaces (`TypeSpec`),
fields (`FieldSpec`), methods & constructors (`MethodSpec`), parameters (`ParameterSpec`) and
annotations (`AnnotationSpec`).

But the _body_ of methods and constructors is not modeled. There's no expression class, no
statement class or syntax tree nodes. Instead, JavaPoet uses strings for code blocks:

```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addCode(""
        + "int total = 0;\n"
        + "for (int i = 0; i < 10; i++) {\n"
        + "  total += i;\n"
        + "}\n")
    .build();
```

Which generates this:

```
  void main() {
    int total = 0;
    for (int i = 0; i < 10; i++) {
      total += i;
    }
  }
```

The manual semicolons, line wrapping, and indentation are tedious and so JavaPoet offers APIs to
make it easier. There's `addStatement()` which takes care of semicolons and newline, and
`beginControlFlow()` + `endControlFlow()` which are used together for braces, newlines, and
indentation:

```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();
```

This example is lame because the generated code is constant! Suppose instead of just adding 0 to 10,
we want to make the operation and range configurable. Here's a method that generates a method:

```
  private MethodSpec computeRange(String name, int from, int to, String op) {
    return MethodSpec.methodBuilder(name)
        .returns(int.class)
        .addStatement("int result = 0")
        .beginControlFlow("for (int i = " + from + "; i < " + to + "; i++)")
        .addStatement("result = result " + op + " i")
        .endControlFlow()
        .addStatement("return result")
        .build();
  }
```

And here's what we get when we call `computeRange("multiply10to20", 10, 20, "*")`:

```
  int multiply10to20() {
    int result = 0;
    for (int i = 10; i < 20; i++) {
      result = result * i;
    }
    return result;
  }
```

Methods generating methods! And since JavaPoet generates source instead of bytecode, you can
read through it to make sure it's right.


### $L _for_ Literals

The string-concatenation in calls to `beginControlFlow()` and `addStatement` is distracting. Too
many operators. To address this, JavaPoet offers a syntax inspired-by but incompatible-with
[`String.format()`][formatter]. It accepts **`$L`** to emit a **literal** value in the output. This
works just like `Formatter`'s `%s`:

```
  private MethodSpec computeRange(String name, int from, int to, String op) {
    return MethodSpec.methodBuilder(name)
        .returns(int.class)
        .addStatement("int result = 0")
        .beginControlFlow("for (int i = $L; i < $L; i++)", from, to)
        .addStatement("result = result $L i", op)
        .endControlFlow()
        .addStatement("return result")
        .build();
  }
```

Literals are emitted directly to the output code with no escaping. Arguments for literals may be
strings, primitives, and a few JavaPoet types described below.

### $S _for_ Strings

When emitting code that includes string literals, we can use **`$S`** to emit a **string**, complete
with wrapping quotation marks and escaping. Here's a program that emits 3 methods, each of which
returns its own name:

```
  @Test public void stringLiterals() throws Exception {
    TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(whatsMyName("slimShady"))
        .addMethod(whatsMyName("eminem"))
        .addMethod(whatsMyName("marshallMathers"))
        .build();
    JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
        .build();
    javaFile.emit(System.out);
  }

  private MethodSpec whatsMyName(String name) {
    return MethodSpec.methodBuilder(name)
        .returns(String.class)
        .addStatement("return $S", name)
        .build();
  }
```

In this case, using `$S` gives us quotation marks:

```
public final class HelloWorld {
  String slimShady() {
    return "slimShady";
  }

  String eminem() {
    return "eminem";
  }

  String marshallMathers() {
    return "marshallMathers";
  }
}
```

### $T _for_ Types

We Java programmers love our types: they make our code easier to understand. And JavaPoet is on
board. It has rich built-in support for types, including automatic generation of `import`
statements. Just use **`$T`** to reference **types**:

```
    MethodSpec today = MethodSpec.methodBuilder("today")
        .returns(Date.class)
        .addStatement("return new $T()", Date.class)
        .build();
    TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(today)
        .build();
    JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
        .build();
    javaFile.emit(System.out);
```

That generates the following `.java` file, complete with the necessary `import`:

```
package com.example.helloworld;

import java.util.Date;

public final class HelloWorld {
  Date today() {
    return new Date();
  }
}
```

We passed `Date.class` to reference a class that just-so-happens to be available when we're
generating code. This doesn't need to be the case. Here's a similar example, but this one
references a class that doesn't exist (yet):

```
    ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");

    MethodSpec today = MethodSpec.methodBuilder("tomorrow")
        .returns(hoverboard)
        .addStatement("return new $T()", hoverboard)
        .build();
```

And that not-yet-existent class is imported as well:

```
package com.example.helloworld;

import com.mattel.Hoverboard;

public final class HelloWorld {
  Hoverboard tomorrow() {
    return new Hoverboard();
  }
}
```

The `ClassName` type is very important, and you'll need it frequently when you're using JavaPoet.
It can identify any _declared_ class. Declared types are just the beginning of Java's rich type
system: we also have arrays, parameterized types, wildcard types, and type variables. JavaPoet has a
`Types` class that can compose each of these:

```
    ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
    ClassName list = ClassName.get("java.util", "List");
    ClassName arrayList = ClassName.get("java.util", "ArrayList");
    Type listOfHoverboards = Types.parameterizedType(list, hoverboard);

    MethodSpec today = MethodSpec.methodBuilder("beyond")
        .returns(listOfHoverboards)
        .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
        .addStatement("result.add(new $T())", hoverboard)
        .addStatement("result.add(new $T())", hoverboard)
        .addStatement("result.add(new $T())", hoverboard)
        .addStatement("return result")
        .build();
```

JavaPoet will decompose each type and import its components where possible.

```
package com.example.helloworld;

import com.mattel.Hoverboard;
import java.util.ArrayList;
import java.util.List;

public final class HelloWorld {
  List<Hoverboard> beyond() {
    List<Hoverboard> result = new ArrayList<>();
    result.add(new Hoverboard());
    result.add(new Hoverboard());
    result.add(new Hoverboard());
    return result;
  }
}
```

### $N _for_ Names

Generated code is often self-referential. Use **`$N`** to refer to another generated declaration by
its name. Here's a method that calls another:

```
  public String byteToHex(int b) {
    char[] result = new char[2];
    result[0] = hexDigit((b >>> 4) & 0xf);
    result[1] = hexDigit(b & 0xf);
    return new String(result);
  }

  public char hexDigit(int i) {
    return (char) (i < 10 ? i + '0' : i - 10 + 'a');
  }
```

When generating the code above, we pass the `hexDigit()` method as an argument to the `byteToHex()`
method using `$N`:

```
    MethodSpec hexDigit = MethodSpec.methodBuilder("hexDigit")
        .addParameter(int.class, "i")
        .returns(char.class)
        .addStatement("return (char) (i < 10 ? i + '0' : i - 10 + 'a')")
        .build();
    MethodSpec byteToHex = MethodSpec.methodBuilder("byteToHex")
        .addParameter(int.class, "b")
        .returns(String.class)
        .addStatement("char[] result = new char[2]")
        .addStatement("result[0] = $N((b >>> 4) & 0xf)", hexDigit)
        .addStatement("result[1] = $N(b & 0xf)", hexDigit)
        .addStatement("return new String(result)")
        .build();
```


Download
--------

Download [the latest .jar][dl] or depend via Maven:
```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>javapoet</artifactId>
  <version>1.0.0</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup:javapoet:1.0.0'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



License
-------

    Copyright 2015 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



JavaWriter
==========

JavaPoet is the successor to [JavaWriter][javawriter]. New projects should prefer JavaPoet because
it has a stronger code model: it understands types and can manage imports automatically. JavaPoet is
also better suited to composition: rather than streaming the contents of a `.java` file
top-to-bottom in a single pass, a file can be assembled as a tree of declarations.

JavaWriter continues to be available in [GitHub][javawriter] and [Maven Central][javawriter_maven].


 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=javapoet&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [javawriter]: https://github.com/square/javapoet/tree/javawriter_2
 [javawriter_maven]: http://search.maven.org/#artifactdetails%7Ccom.squareup%7Cjavawriter%7C2.5.1%7Cjar
 [formatter]: http://developer.android.com/reference/java/util/Formatter.html
