package jedi.db.models;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 */
public enum CascadeType {
   
   NONE("NONE"), 
   INSERT("INSERT"), 
   UPDATE("UPDATE"), 
   SAVE("SAVE"), 
   DELETE("DELETE"), 
   ALL("ALL");
   
   private final String value;
   
   private CascadeType(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
