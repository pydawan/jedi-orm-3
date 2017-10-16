package jedi.db.models;

import java.util.Date;

import org.junit.Test;

/**
 * @author thiago-amm
 * @version v1.0.0 15/10/2017
 * @since v1.0.0
 */
public class QueryFieldTest {
   
   @Test
   public void testExact() {
      QueryField id = new QueryField("id");
      System.out.println(id.exact(10));
   }
   
   @Test
   public void testNotExact() {
      QueryField id = new QueryField("id");
      System.out.println(id.notExact(10));
   }
   
   @Test
   public void testStartsWith() {
      QueryField nome = new QueryField("nome");
      System.out.println(nome.startsWith("Thi"));
      System.out.println(nome.istartsWith("Thi"));
      System.out.println(nome.notStartsWith("Thi"));
      System.out.println(nome.iNotStartsWith("Thi"));
   }
   
   @Test
   public void testContains() {
      QueryField nome = new QueryField("nome");
      QueryField sobrenome = new QueryField("sobrenome");
      System.out.println(nome.contains("Thi"));
      System.out.println(nome.icontains("Thi"));
      System.out.println(nome.notContains("Thi"));
      System.out.println(nome.iNotContains("Thi"));
      System.out.println(nome.contains("Thiago").and(sobrenome.contains("Alexandre")));
   }
   
   @Test
   public void testEndsWith() {
      QueryField nome = new QueryField("nome");
      System.out.println(nome.endsWith("Thi"));
      System.out.println(nome.iendsWith("Thi"));
      System.out.println(nome.notEndsWith("Thi"));
      System.out.println(nome.iNotEndsWith("Thi"));
   }
   
   @Test
   public void testIn() {
      QueryField id = new QueryField("id");
      System.out.println(id.in(1, 2, 3));
      System.out.println(id.notIn(1, 2, 3));
      System.out.println(id.lessThan(2));
   }
   
   @Test
   public void testYear() {
      QueryField dataNascimento = new QueryField("dataNascimento");
      System.out.println(dataNascimento.yearLessThan(2005));
      System.out.println(dataNascimento.yearLT(2005));
      System.out.println(dataNascimento.yearLessThan(new Date()));
      System.out.println(dataNascimento.yearLessThanOrEqual(2005));
      System.out.println(dataNascimento.yearEqual(2005));
      System.out.println(dataNascimento.yearNotEqual(new Date()));
   }
   
}
