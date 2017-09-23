package jedi.db.sql;

/**
 * @author thiago
 * @version v1.0.0 15/05/2017
 * @since v1.0.0
 */
public enum SqlOperator {
   
   EQUAL("="),
   DIFFERENT("<>"),
   GREATER_THAN(">"),
   GREATER_THAN_OR_EQUAL_TO(">="),
   LESS_THAN("<"),
   LESS_THAN_OR_EQUAL_TO("<="),
   IN("IN"),
   BETWEEN("BETWEEN"),
   LIKE("LIKE"),
   NOT("NOT LIKE"),
   IS_NULL("IS NULL"),
   IS_NOT_NULL("IS NOT NULL");
   
   private final String value;
   
   private SqlOperator( String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
   public String value() {
      return value;
   }
   
}
