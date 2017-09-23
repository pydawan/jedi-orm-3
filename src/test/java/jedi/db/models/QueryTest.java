package jedi.db.models;

import static jedi.db.models.Query.between;
import static jedi.db.models.Query.ignoreCase;
import static jedi.db.models.Query.in;
import static jedi.db.models.Query.inRange;
import static jedi.db.models.Query.notBetween;
import static jedi.db.models.Query.notIgnoreCase;
import static jedi.db.models.Query.notIn;
import static jedi.db.models.Query.notInRange;
import static jedi.db.models.Query.notStartsWith;
import static jedi.db.models.Query.startsWith;
import static jedi.db.sql.SQLBuilder.as;

import org.junit.Assert;
import org.junit.Test;

import jedi.db.sql.SQLBuilder;

/**
 * @author thiago
 * @version v1.0.0 18/07/2017
 * @since v1.0.0
 */
public class QueryTest {
   
   @Test
   public void testStartsWith() {
      String esperado = "nome__startswith='Thiago'";
      String obtido = Query.startsWith("nome", "Thiago");
      System.out.println("Teste startsWith");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
//      and("id", in(1, 2, 3));
//      in("id", 1, 2, 3);
   }
   
   @Test
   public void testStartsWithIgnoreCase() {
      String esperado = "nome__istartswith='Thiago'";
      String obtido = startsWith("nome", "Thiago", ignoreCase());
      System.out.println("Teste startsWithIgnoreCase");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
   }
   
   @Test
   public void testStartsWithNotIgnoreCase() {
      String esperado = "nome__startswith='Thiago'";
      String obtido = startsWith("nome", "Thiago", notIgnoreCase());
      System.out.println("Teste startsWithNotIgnoreCase");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
   }
   
   @Test
   public void testNotStartsWith() {
      String esperado = "nome__!startswith='Thiago'";
      String obtido = notStartsWith("nome", "Thiago");
      System.out.println("Teste notStartsWith");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
   }
   
   @Test
   public void testNotStartsWithIgnoreCase() {
      String esperado = "nome__!istartswith='Thiago'";
      String obtido = notStartsWith("nome", "Thiago", ignoreCase());
      System.out.println("Teste notStartsWithIgnoreCase");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
   }
   
   @Test
   public void testNotStartsWithNotIgnoreCase() {
      String esperado = "nome__!startswith='Thiago'";
      String obtido = notStartsWith("nome", "Thiago", notIgnoreCase());
      System.out.println("Teste notStartsWithNotIgnoreCase");
      System.out.println("Valor esperado: " + esperado);
      System.out.println("Valor obtido: " + obtido);
      System.out.println();
      Assert.assertEquals(esperado, obtido);
   }
   
   @Test
   public void testIn() {
      System.out.println(in("id", 1, 2, 3));
      System.out.println(notIn("id", 1, 2, 3));
      System.out.println(inRange("nome", "a", "b"));
      System.out.println(notInRange("nota", 0.0, 5.0));
      System.out.println(between("nascimento", "1982-11-19", "1986-02-04"));
      System.out.println(notBetween("nascimento", "1982-11-19", "1986-02-04"));
   }
   
   @Test
   public void testQuery() {
      SQLBuilder sql = SQLBuilder.sql();
      System.out.println(sql.select().build());
      System.out.println(sql.select("id", "nome", "cpf").build());
      System.out.println(sql.select("cidade.nome, estado.sigla").from("cidades cidade", "estados estado").build());
      System.out.println(
         sql
         .select("cidade.nome, estado.sigla")
         .from(as("cidades", "cidade"), as("estados", "estado"))
         .build());
   }
   
}
