JavaPoet
========

`JavaPoet` is a Java API for generating `.java` source files.

Source file generation can useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.



Example
-------

```java
TypeSpec raven = TypeSpec.classBuilder("Raven")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(MethodSpec.methodBuilder("main")
        .addModifiers(Modifier.PUBLIC)
        .addCode("$T verses = new $T();\n",
            Types.parameterizedType(List.class, String.class),
            Types.parameterizedType(ArrayList.class, String.class))
        .addCode("verses.add($S);\n",
            "Once upon a midnight dreary, while I pondered, weak and weary...")
        .addCode("verses.add($S);\n",
            "Over many a quaint and curious volume of forgotten lore—")
        .addCode("System.out.println(verses);\n")
        .build())
    .build();
JavaFile ravenSourceFile = new JavaFile.Builder()
    .packageName("com.squareup.poe")
    .typeSpec(raven)
    .build();
```

Would produce the following source output:

```java
package com.squareup.poe;

import java.util.ArrayList;
import java.util.List;

public final class Raven {
  public void main() {
    List<String> verses = new ArrayList<String>();
    verses.add("Once upon a midnight dreary, while I pondered, weak and weary...");
    verses.add("Over many a quaint and curious volume of forgotten lore—");
    System.out.println(verses);
  }
}
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



 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=javapoet&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
