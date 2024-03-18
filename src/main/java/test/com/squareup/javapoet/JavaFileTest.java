package test.com.squareup.javapoet; 

import com.squareup.javapoet.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.lang.model.element.Modifier;
import java.util.Date;


/** 
* JavaFile Tester. 
* 
* @author <Authors name> 
* @since <pre>5�� 30, 2020</pre> 
* @version 1.0 
*/
public class JavaFileTest {
    /**
    *
    * Method: addPkgAnnotation(AnnotationSpec annotationSpec)
    *
    */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

   @Test
  public void test1()  throws Throwable  {

       TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
               .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .build();

       AnnotationSpec visibleForTesting = AnnotationSpec
               .builder(Date.class)
               .addMember("year", "2020")
               .build();

       JavaFile.Builder builder = JavaFile.builder("", helloWorld);
       builder = builder.addPackageAnno(visibleForTesting);
       JavaFile javaFile = builder.build();
       javaFile.writeTo(System.out);
   }

   @Test
   public void test2() throws Throwable  {
       TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
               .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .build();

       JavaFile.Builder builder = JavaFile.builder("", helloWorld);
       thrown.expect(java.lang.IllegalArgumentException.class);
       builder = builder.addPackageAnno((AnnotationSpec)null);

       JavaFile javaFile = builder.build();
       javaFile.writeTo(System.out);
   }

}
