package jedi.db.engine;

import org.junit.Test;

import jedi.types.DateTime;

public class JediEngineTest {
   
   @Test
   public void test() {
//      JediEngine.APP_SRC_DIR = "/C:\\Users\\gustavo-of\\workspace\\transporte-regular\\src\\main\\java";
//      JediEngine.APP_SRC_DIR = "/C:/Users/gustavo-of/workspace/transporte-regular/src/main/java";
//      JediEngine.convertFilePathToClassPath("C:\\Users\\gustavo-of\\workspace\\transporte-regular\\src\\main\\java\\br\\gov\\go\\agr\\transporte\\regular\\model\\Operador.java");
//      JediEngine.createdb();
//      JediEngine.dropdb();
//      String caminho = "Thiago";
//      JediEngine.convertFilePathToClassPath("thiago");
//      JediEngine.convertFilePathToClassPath(caminho);
      
//      System.out.println(DateTime.of("2017-09-27 17:07:45"));
//      System.out.println(DateTime.of("2017-09-27"));
      
      System.out.println(new DateTime("2017-09-27 17:07:45"));
      System.out.println(DateTime.datetime("2017-09-27 17:07:45"));
      System.out.println(DateTime.of("2017-09-27 17:07:45"));
      
      System.out.println(new DateTime("2017-09-27"));
      System.out.println(DateTime.datetime("2017-09-27"));
      System.out.println(DateTime.of("2017-09-27"));
      
      System.out.println(new DateTime());
      System.out.println(DateTime.datetime());
      System.out.println(DateTime.of());
   }
   
}
