package jedi.db.models;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 */
public enum Engine {
   
   MYSQL("MYSQL"), 
   POSTGRESQL("POSTGRESQL"), 
   ORACLE("ORACLE"), 
   SQL_SERVER("SQL_SERVER"), 
   H2("H2"), 
   SQLITE("SQLITE");
   
   private final String value;
   
   private Engine(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
}
