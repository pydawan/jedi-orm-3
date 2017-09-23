package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 28/07/2017
 * @since v1.0.0
 */
public class QueryPageNumber {
   
   private Integer value;
   
   public QueryPageNumber(Integer value) {
      this.value = value;
   }
   
   public QueryPageNumber(int value) {
      this(new Integer(value));
   }
   
   public QueryPageNumber() {
      this(0);
   }
   
   public Integer getValue() {
      return value;
   }
   
   public Integer value() {
      return getValue();
   }
   
   public Integer get() {
      return value();
   }
   
   public void setValue(Integer value) {
      this.value = value;
   }
   
   public void setValue(int value) {
      this.value = new Integer(value);
   }
   
   public QueryPageNumber value(Integer value) {
      setValue(value);
      return this;
   }
   
   public QueryPageNumber value(int value) {
      setValue(value);
      return this;
   }
   
   public QueryPageNumber set(Integer value) {
      return value(value);
   }
   
   public QueryPageNumber set(int value) {
      return value(value);
   }
   
}
