Java Writer
===========

`JavaWriter` is a utility class which aids in generating Java source files.

Source file generation can useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.



Example
-------

```java
writer.emitPackage("com.example")
    .beginType("com.example.Person", "class", EnumSet.of(PUBLIC, FINAL))
    .emitField("String", "firstName", EnumSet.of(PRIVATE))
    .emitField("String", "lastName", EnumSet.of(PRIVATE))
    .emitJavadoc("Returns the person's full name.")
    .beginMethod("String", "getName", EnumSet.of(PUBLIC))
    .emitStatement("return firstName + \" \" + lastName")
    .endMethod()
    .endType();
```

Would produce the following source output:

```java
package com.example;

public final class Person {
  private String firstName;
  private String lastName;
  /**
   * Returns the person's full name.
   */
  public String getName() {
    return firstName + " " + lastName;
  }
}
```



Download
--------

Download [the latest .jar][dl] or depend via Maven:

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>javawriter</artifactId>
  <version>(insert latest version)</version>
</dependency>
```



License
-------

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



 [dl]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup&a=javawriter&v=LATEST
