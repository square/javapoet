import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.IOException;

public class RecordExample {
    public static void main(String[] args) throws IOException {
        TypeSpec record = TypeSpec.recordBuilder("Employee")
                .addModifiers(Modifier.PUBLIC)
                .addField(int.class, "empToken", Modifier.STATIC)
                .addRecord(int.class, "id")
                .addRecord(String.class, "firstName")
                .addRecord(String.class, "lastName")
                .addMethod(MethodSpec.CompactConstructorBuilder()
                        .addCode("if (id < 100) {\n" +
                                "    throw new IllegalArgumentException(" +
                                "    \"Employee Id cannot be below 100.\");\n" +
                                "}\n" +
                                "if (firstName.length() < 2) {\n" +
                                "    throw new IllegalArgumentException(\n" +
                                "    \"First name must be 2 characters or more.\");\n" +
                                "}")
                        .addModifiers(Modifier.PUBLIC).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(int.class, "id")
                        .addParameter(String.class, "lastName")
                        .addStatement("this(id, firstName, null)").build())
                .addMethod(MethodSpec.methodBuilder("getFullName")
                        .addCode("if (lastName == null)\n" +
                                "    System.out.println(firstName());\n" +
                                "\n" +
                                "else\n" +
                                "    System.out.println(firstName() + \" \"\n" +
                                "                       + lastName());")
                        .build())
                .addMethod(MethodSpec.methodBuilder("generateEmployeeToken")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(int.class)
                        .addCode("return ++empToken;")
                        .build())
                .build();

        JavaFile javaFile = JavaFile.builder("com.example.record", record)
                .build();

        javaFile.writeTo(System.out);
    }
}
