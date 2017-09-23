package jedi.db.models;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 */
public enum Models {
   
   CASCADE("CASCADE"), 
   PROTECT("PROTECT"), 
   SET_NULL("SET NULL"), 
   SET_DEFAULT("SET DEFAULT"), 
   DO_NOTHING("");
   
   private final String value;
   
   private Models(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
