Java Writer
===========

`JavaWriter` is a utility class which aids in generating Java source files.

Source file generation can useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.



Example
-------

```java
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.Modifier;

import com.squareup.javawriter.JavaWriter;


public class SampleGenerator {
	public static void main(String[] args) throws IOException {
		JavaWriter writer= new JavaWriter(new BufferedWriter(new FileWriter("D:\\git_repos\\generator\\Person.java")));
		
		Set<Modifier> modifiers= new TreeSet<Modifier>();
		modifiers.add(Modifier.PUBLIC);
		
		writer.emitPackage("com.example")
	    .beginType("com.example.Person", "class")
	    .emitField("String", "firstName")
	    .emitField("String", "lastName")
	    .emitJavadoc("Returns the person's full name.")
	    .beginMethod("String", "getName", modifiers)
	    .emitStatement("return firstName + \" \" + lastName")
	    .endMethod()
	    .endType();
		writer.close();
	}
}
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
