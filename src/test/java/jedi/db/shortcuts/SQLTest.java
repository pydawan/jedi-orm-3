package jedi.db.shortcuts;

import static jedi.db.sql.Sql.ex;
import static jedi.db.sql.Sql.sql;

import org.junit.Test;

/**
 * @author thiago
 * @version v1.0.0 15/05/2017
 * @since v1.0.0
 */
public class SQLTest {
   
   @Test
   public void test() {
      System.out.println(
         sql()
         .select("d.nome", "g.nome")
         .from("departamentos d", "gerencias g")
         .where("d.gerencia_id = g.id", "AND", "d.nome LIKE 'Co%'")
      );
      System.out.println(ex().cv("nome = 'thiago'").and().cv("idade > 18").notIn().between());
   }
   
}
