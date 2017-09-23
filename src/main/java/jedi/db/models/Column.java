package jedi.db.models;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thiago
 * @version v1.0.0 12/05/2017
 * @since v1.0.0
 */
public class Column extends HashMap<String, Object> {
   
   private static final long serialVersionUID = 1L;
   
   public Column(Map<String, Object> map) {
      if (map == null) {
         throw new IllegalArgumentException("O mapa passado como argumento não pode ser nulo!");
      } else {
         for (Entry<String, Object> e : map.entrySet()) {
            this.put(e.getKey(), e.getValue());
         }
      }
   }
   
   public static Column of(Map<String, Object> map) {
      return new Column(map);
   }
   
   public Column(String name, Object value) {
      if (name != null) {
         this.put(name, value);
      } else {
         throw new IllegalArgumentException("O nome da coluna não pode ser null!");
      }
   }
   
   public static Column of(String name, Object value) {
      return new Column(name, value);
   }
}
