package jedi.db.models;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 */
public enum JoinType {
   
   INNER("INNER"), 
   LEFT("LEFT"), 
   RIGHT("RIGHT");
   
   private final String value;
   
   private JoinType(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
