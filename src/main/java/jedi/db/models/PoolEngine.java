package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 07/03/2017
 * @since v1.0.0
 */
public enum PoolEngine {
   
   C3P0("c3p0"), 
   HIKARI("hikari"), 
   JEDI("jedi");
   
   private final String value;
   
   private PoolEngine(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
   public String value() {
      return value;
   }
   
}
