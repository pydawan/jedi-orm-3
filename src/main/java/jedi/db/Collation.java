package jedi.db;

/**
 * @author thiagoamm
 * @version v1.0.0 11/08/2017
 * @since v1.0.0
 */
public enum Collation {
   
   LATIN1_SWEDISH_CI("latin1_swedish_ci"), 
   UTF8_GENERAL_CI("utf8_general_ci");
      
   private final String value;
   
   private Collation(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
