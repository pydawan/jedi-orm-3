package jedi.db.models;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author thiago
 * @version v1.0.0 12/05/2017
 * @since v1.0.0
 */
public class Row extends ArrayList<Column> {
   
   private static final long serialVersionUID = 1L;
   
   public Row(Column... columns) {
      this.addAll(Arrays.asList(columns));
   }
   
   public static Row of(Column... columns) {
      return new Row(columns);
   }
}
