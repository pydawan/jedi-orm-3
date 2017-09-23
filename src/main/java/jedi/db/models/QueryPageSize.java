package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 23/03/2017
 * @since v1.0.0
 */
public class QueryPageSize {
   
   private Integer value;
   
   public QueryPageSize(Integer value) {
      this.value = value;
   }
   
   public QueryPageSize(int value) {
      this(new Integer(value));
   }
   
   public QueryPageSize() {
      this(new Integer(0));
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
      this.value = value;
   }
   
   public QueryPageSize value(Integer value) {
      setValue(value);
      return this;
   }
   
   public QueryPageSize value(int value) {
      setValue(value);
      return this;
   }
   
   public QueryPageSize set(Integer value) {
      return value(value);
   }
   
   public QueryPageSize set(int value) {
      return value(value);
   }
   
}
