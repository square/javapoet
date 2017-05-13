JavaPoet
========

`JavaPoet` 是一个用来生成 .java源文件的Java API。

当做如注解或者数据库模式、协议格式等事情时，生成源文件就比较有用处。


### Example

以 `HelloWorld` 类为例:

```java
package com.example.helloworld;

public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}
```

上面的代码就是使用javapoet用下面的代码进行生成的：

```java
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

javaFile.writeTo(System.out);
```


通过`MethodSpec`类来创建一个"main"方法，并配置了修饰符、返回值类型、参数以及代码语句。然后把这个main方法添加到 `HelloWorld` 类中，最后添加到 `HelloWorld.java`文件中。
这个例子中，我们将文件通过`Sytem.out` 进行输出，但是同样也可以使用(`JavaFile.toString()`) 得到string字符串，或者通过 (`JavaPoet.writeTo()`) 方法写入到文件系统中。


[Javadoc][javadoc] 中包括了完整的JavaPoet API, 我们接着往下看。

### Code & Control Flow

大多数JavaPoet的API使用的是简单的不可变的Java对象。通过建造者模式，链式方法，可变参数是的API比较友好。JavaPoet提供了(`TypeSpec`)用于创建类或者接口，(`FieldSpec`)用来创建字段，(`MethodSpec`)用来创建方法和构造函数，(`ParameterSpec`)用来创建参数，(`AnnotationSpec`)用于创建注解。

但是如果没有语句类，没有语法结点数，可以通过字符串来构建代码块：


```java
MethodSpec main = MethodSpec.methodBuilder("main")
    .addCode(""
        + "int total = 0;\n"
        + "for (int i = 0; i < 10; i++) {\n"
        + "  total += i;\n"
        + "}\n")
    .build();
```

生成的代码如下:

```java
void main() {
  int total = 0;
  for (int i = 0; i < 10; i++) {
    total += i;
  }
}
```

人为的输入分号、换行和缩进是比较乏味的。所以JavaPoet提供了相关API使它变的容易。
`addStatement()` 负责分号和换行，`beginControlFlow()` + `endControlFlow()` 需要一起使用，提供换行符和缩进。

```java
MethodSpec main = MethodSpec.methodBuilder("main")
    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();
```

这个例子稍微有点差劲。生成的代码如下：

```java
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

调用`computeRange("multiply10to20", 10, 20, "*")`就生成如下代码:

```java
int multiply10to20() {
  int result = 0;
  for (int i = 10; i < 20; i++) {
    result = result * i;
  }
  return result;
}
```

方法生成方法！JavaPoet生成的是源代码而不是字节码，所以可以通过阅读源码确保正确。


### $L for Literals

字符串连接的方法`beginControlFlow()` 和 `addStatement`是分散开的，操作较多。
针对这个问题, JavaPoet 提供了一个语法但是有违[`String.format()`][formatter]语法. 通过 **`$L`** 来接受一个 **literal** 值。 这有点像 `Formatter`'s `%s`:

```java
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

Literals 直接写在输出代码中，没有转义。 它的类型可以是字符串、primitives和一些接下来要说的JavaPoet类型。

### $S for Strings

当输出的代码包含字符串的时候, 可以使用 **`$S`** 表示一个 **string**。 下面的代码包含三个方法，每个方法返回自己的名字:

```java
public static void main(String[] args) throws Exception {
  TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addMethod(whatsMyName("slimShady"))
      .addMethod(whatsMyName("eminem"))
      .addMethod(whatsMyName("marshallMathers"))
      .build();
      
  JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
      .build();
      
  javaFile.writeTo(System.out);
}

private static MethodSpec whatsMyName(String name) {
  return MethodSpec.methodBuilder(name)
      .returns(String.class)
      .addStatement("return $S", name)
      .build();
}
```

输出结果如下:

```java
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

### $T for Types

使用Java内置的类型会使代码比较容易理解。JavaPoet极大的支持这些类型，通过 **`$T`** 进行映射，会自动`import`声明。


```java
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
    
javaFile.writeTo(System.out);
```

自动完成import声明，生成代码如下:

```java
package com.example.helloworld;

import java.util.Date;

public final class HelloWorld {
  Date today() {
    return new Date();
  }
}
```

再举一个相似的例子，但是应用了一个不存在的类：

```java
ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");

MethodSpec today = MethodSpec.methodBuilder("tomorrow")
    .returns(hoverboard)
    .addStatement("return new $T()", hoverboard)
    .build();
```

类不存在，但是代码是完整的:

```java
package com.example.helloworld;

import com.mattel.Hoverboard;

public final class HelloWorld {
  Hoverboard tomorrow() {
    return new Hoverboard();
  }
}
```

`ClassName` 这个类非常重要, 当你使用JavaPoet的时候会频繁的使用它。
它可以识别任何声明类。具体看下面的例子:

```java
ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
ClassName list = ClassName.get("java.util", "List");
ClassName arrayList = ClassName.get("java.util", "ArrayList");
TypeName listOfHoverboards = ParameterizedTypeName.get(list, hoverboard);

MethodSpec beyond = MethodSpec.methodBuilder("beyond")
    .returns(listOfHoverboards)
    .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
    .addStatement("result.add(new $T())", hoverboard)
    .addStatement("result.add(new $T())", hoverboard)
    .addStatement("result.add(new $T())", hoverboard)
    .addStatement("return result")
    .build();
```

JavaPoet将每一种类型进行分解，并尽可能的导入其声明.

```java
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

#### Import static

JavaPoet支持`import static`。它显示的收集类型成员的名称。例子如下:

```java
...
ClassName namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards");

MethodSpec beyond = MethodSpec.methodBuilder("beyond")
    .returns(listOfHoverboards)
    .addStatement("$T result = new $T<>()", listOfHoverboards, arrayList)
    .addStatement("result.add($T.createNimbus(2000))", hoverboard)
    .addStatement("result.add($T.createNimbus(\"2001\"))", hoverboard)
    .addStatement("result.add($T.createNimbus($T.THUNDERBOLT))", hoverboard, namedBoards)
    .addStatement("$T.sort(result)", Collections.class)
    .addStatement("return result.isEmpty() $T.emptyList() : result", Collections.class)
    .build();

TypeSpec hello = TypeSpec.classBuilder("HelloWorld")
    .addMethod(beyond)
    .build();

JavaFile.builder("com.example.helloworld", hello)
    .addStaticImport(hoverboard, "createNimbus")
    .addStaticImport(namedBoards, "*")
    .addStaticImport(Collections.class, "*")
    .build();
```

JavaPoet将会首先添加 `import static` 代码块进行配置，当然也需要导入其他所需的类型引用。

```java
package com.example.helloworld;

import static com.mattel.Hoverboard.Boards.*;
import static com.mattel.Hoverboard.createNimbus;
import static java.util.Collections.*;

import com.mattel.Hoverboard;
import java.util.ArrayList;
import java.util.List;

class HelloWorld {
  List<Hoverboard> beyond() {
    List<Hoverboard> result = new ArrayList<>();
    result.add(createNimbus(2000));
    result.add(createNimbus("2001"));
    result.add(createNimbus(THUNDERBOLT));
    sort(result);
    return result.isEmpty() ? emptyList() : result;
  }
}
```

### $N for Names

使用 **`$N`** 可以引用另外一个通过名字生成的声明。

```java
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

生成的代码如下，在`byteToHex()`方法中通过`$N`来引用 `hexDigit()`方法作为一个参数：

```java
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

### Methods

上面的例子中的方法都有方法体。 使用 `Modifiers.ABSTRACT` 创建的方法是没有方法体的。通常用来创建一个抽象类或接口。

```java
MethodSpec flux = MethodSpec.methodBuilder("flux")
    .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
    .build();
    
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addMethod(flux)
    .build();
```

生成如下代码：

```java
public abstract class HelloWorld {
  protected abstract void flux();
}
```

当执行修饰符的时。JavaPoet用的是
[`javax.lang.model.element.Modifier`][modifier]类，这个类在android平台上不可用. 这只限制与生成代码阶段；输出的代码可运行在任何平台上: JVMs, Android,
and GWT。

方法可能会有参数，异常，可变参数，注释，注解，类型变量和一个返回类型。这些都可以通过 `MethodSpec.Builder` 来进行配置。

### Constructors

`MethodSpec` 也可以用来创建构造函数:

```java
MethodSpec flux = MethodSpec.constructorBuilder()
    .addModifiers(Modifier.PUBLIC)
    .addParameter(String.class, "greeting")
    .addStatement("this.$N = $N", "greeting", "greeting")
    .build();
    
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(String.class, "greeting", Modifier.PRIVATE, Modifier.FINAL)
    .addMethod(flux)
    .build();
```

生成如下代码:

```java
public class HelloWorld {
  private final String greeting;

  public HelloWorld(String greeting) {
    this.greeting = greeting;
  }
}
```

多数情况下，构造方法同普通方法一样。当生成代码时，构造函数会先于其他方法生成。

### Parameters

通过 `ParameterSpec.builder()` 可以创建参数，或者直接调用 `MethodSpec`类的 `addParameter()` 方法添加参数：

```java
ParameterSpec android = ParameterSpec.builder(String.class, "android")
    .addModifiers(Modifier.FINAL)
    .build();

MethodSpec welcomeOverlords = MethodSpec.methodBuilder("welcomeOverlords")
    .addParameter(android)
    .addParameter(String.class, "robot", Modifier.FINAL)
    .build();
```

虽然上面的代码生成`android` 和 `robot`这两个参数是不同的方式，但是输出是一样的：

```java
void welcomeOverlords(final String android, final String robot) {
}
```

当参数有注解(比如 `@Nullable`)的时候，通过扩展的 `Builder` 方式创建参数是比较方便的。

### Fields

字段通参数一样通过 `build` 方式创建:

```java
FieldSpec android = FieldSpec.builder(String.class, "android")
    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
    .build();
    
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(android)
    .addField(String.class, "robot", Modifier.PRIVATE, Modifier.FINAL)
    .build();
```

生成如下代码:

```java
public class HelloWorld {
  private final String android;

  private final String robot;
}
```

通关 `Builder` 方式很容易生成带注释、注解或者初始化的字段。
Field的初始化代码如下：

```java
FieldSpec android = FieldSpec.builder(String.class, "android")
    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
    .initializer("$S + $L", "Lollipop v.", 5.0d)
    .build();
```

生成代码：

```java
private final String android = "Lollipop v." + 5.0;
```

### Interfaces

JavaPoet同样可以生成接口。注意接口的方法必须是 `PUBLIC
ABSTRACT` 类型，接口的变量必须是 `PUBLIC STATIC FINAL` 类型。
创建接口的时候必须要添加上这些修饰符。

```java
TypeSpec helloWorld = TypeSpec.interfaceBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(FieldSpec.builder(String.class, "ONLY_THING_THAT_IS_CONSTANT")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", "change")
        .build())
    .addMethod(MethodSpec.methodBuilder("beep")
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .build())
    .build();
```

但是这些修饰符在生成的java文件中是找不到的。这些都是缺省值。

```java
public interface HelloWorld {
  String ONLY_THING_THAT_IS_CONSTANT = "change";

  void beep();
}
```

### Enums

通过 `enumBuilder` 可以创建枚举类型, 调用 `addEnumConstant()` 可添加枚举变量：

```java
TypeSpec helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("ROCK")
    .addEnumConstant("SCISSORS")
    .addEnumConstant("PAPER")
    .build();
```

生成如下代码:

```java
public enum Roshambo {
  ROCK,

  SCISSORS,

  PAPER
}
```

Fancy enums 也是支持的。

```java
TypeSpec helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("$S", "fist")
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return $S", "avalanche!")
            .build())
        .build())
    .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder("$S", "peace")
        .build())
    .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder("$S", "flat")
        .build())
    .addField(String.class, "handsign", Modifier.PRIVATE, Modifier.FINAL)
    .addMethod(MethodSpec.constructorBuilder()
        .addParameter(String.class, "handsign")
        .addStatement("this.$N = $N", "handsign", "handsign")
        .build())
    .build();
```

生成代码:

```java
public enum Roshambo {
  ROCK("fist") {
    @Override
    public void toString() {
      return "avalanche!";
    }
  },

  SCISSORS("peace"),

  PAPER("flat");

  private final String handsign;

  Roshambo(String handsign) {
    this.handsign = handsign;
  }
}
```

### Anonymous Inner Classes

在上面的枚举代码汇总，使用了 `Types.anonymousInnerClass()`。匿名内部类也可以在代码块中使用。 通过 `$L` 引用匿名内部类：

```java
TypeSpec comparator = TypeSpec.anonymousClassBuilder("")
    .addSuperinterface(ParameterizedTypeName.get(Comparator.class, String.class))
    .addMethod(MethodSpec.methodBuilder("compare")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(String.class, "a")
        .addParameter(String.class, "b")
        .returns(int.class)
        .addStatement("return $N.length() - $N.length()", "a", "b")
        .build())
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addMethod(MethodSpec.methodBuilder("sortByLength")
        .addParameter(ParameterizedTypeName.get(List.class, String.class), "strings")
        .addStatement("$T.sort($N, $L)", Collections.class, "strings", comparator)
        .build())
    .build();
```

生成的代码包含一个类和一个方法：

```java
void sortByLength(List<String> strings) {
  Collections.sort(strings, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  });
}
```

定义匿名内部类的一个特别棘手的问题是参数的构造。在上面的代码中我们传递了不带参数的空字符串。`TypeSpec.anonymousClassBuilder("")`。


### Annotations

对方法添加注解非常简单：

```java
MethodSpec toString = MethodSpec.methodBuilder("toString")
    .addAnnotation(Override.class)
    .returns(String.class)
    .addModifiers(Modifier.PUBLIC)
    .addStatement("return $S", "Hoverboard")
    .build();
```

生成如下代码：

```java
  @Override
  public String toString() {
    return "Hoverboard";
  }
```

通过 `AnnotationSpec.builder()` 可以对注解设置属性：

```java
MethodSpec logRecord = MethodSpec.methodBuilder("recordEvent")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(Headers.class)
        .addMember("accept", "$S", "application/json; charset=utf-8")
        .addMember("userAgent", "$S", "Square Cash")
        .build())
    .addParameter(LogRecord.class, "logRecord")
    .returns(LogReceipt.class)
    .build();
```

生成如下代码：

```java
@Headers(
    accept = "application/json; charset=utf-8",
    userAgent = "Square Cash"
)
LogReceipt recordEvent(LogRecord logRecord);
```

注解同样可以注解其它的注解。通过 `$L` 进行引用：

```java
MethodSpec logRecord = MethodSpec.methodBuilder("recordEvent")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(HeaderList.class)
        .addMember("value", "$L", AnnotationSpec.builder(Header.class)
            .addMember("name", "$S", "Accept")
            .addMember("value", "$S", "application/json; charset=utf-8")
            .build())
        .addMember("value", "$L", AnnotationSpec.builder(Header.class)
            .addMember("name", "$S", "User-Agent")
            .addMember("value", "$S", "Square Cash")
            .build())
        .build())
    .addParameter(LogRecord.class, "logRecord")
    .returns(LogReceipt.class)
    .build();
```

生成如下代码：

```java
@HeaderList({
    @Header(name = "Accept", value = "application/json; charset=utf-8"),
    @Header(name = "User-Agent", value = "Square Cash")
})
LogReceipt recordEvent(LogRecord logRecord);
```

 `addMember()` 可以调用多次。
 
### Javadoc

变量方法和类都可以添加注释:

```java
MethodSpec dismiss = MethodSpec.methodBuilder("dismiss")
    .addJavadoc("Hides {@code message} from the caller's history. Other\n"
        + "participants in the conversation will continue to see the\n"
        + "message in their own history unless they also delete it.\n")
    .addJavadoc("\n")
    .addJavadoc("<p>Use {@link #delete($T)} to delete the entire\n"
        + "conversation for all participants.\n", Conversation.class)
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addParameter(Message.class, "message")
    .build();
```

生成如下：

```java
  /**
   * Hides {@code message} from the caller's history. Other
   * participants in the conversation will continue to see the
   * message in their own history unless they also delete it.
   *
   * <p>Use {@link #delete(Conversation)} to delete the entire
   * conversation for all participants.
   */
  void dismiss(Message message);
```

使用 `$T` 可以自动导入类型的引用。

Download
--------

下载最新的 [the latest .jar][dl] 

或者添加Maven依赖：

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>javapoet</artifactId>
  <version>1.7.0</version>
</dependency>
```
或者Gradle依赖：

```groovy
compile 'com.squareup:javapoet:1.7.0'
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
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/com/squareup/javapoet/
 [javadoc]: https://square.github.io/javapoet/1.x/javapoet/
 [javawriter]: https://github.com/square/javapoet/tree/javawriter_2
 [javawriter_maven]: http://search.maven.org/#artifactdetails%7Ccom.squareup%7Cjavawriter%7C2.5.1%7Cjar
 [formatter]: http://developer.android.com/reference/java/util/Formatter.html
 [modifier]: http://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Modifier.html
