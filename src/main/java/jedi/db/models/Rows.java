package jedi.db.models;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author thiago
 * @version v1.0.0 12/05/2017
 * @since v1.0.0
 */
public class Rows extends ArrayList<Row> {
   
   private static final long serialVersionUID = 1L;
   
   public Rows(Row... rows) {
      this.addAll(Arrays.asList(rows));
   }
   
   public static Rows of(Row... rows) {
      return new Rows(rows);
   }
}
