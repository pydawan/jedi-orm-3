package jedi.db.models;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 */
public enum FetchType {
   
   NONE("NONE"), 
   EAGER("EAGER"), 
   LAZY("LAZY");
   
   private final String value;
   
   private FetchType(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
