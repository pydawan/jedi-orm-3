package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 23/03/2017
 * @since v1.0.0
 */
public class QueryPageStart {
   
   private Integer value;
   
   public QueryPageStart(Integer value) {
      this.value = value;
   }
   
   public QueryPageStart(int value) {
      this(new Integer(value));
   }
   
   public QueryPageStart() {
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
      this.value = value;
   }
   
   public QueryPageStart value(Integer value) {
      setValue(value);
      return this;
   }
   
   public QueryPageStart value(int value) {
      setValue(value);
      return this;
   }
   
   public QueryPageStart set(Integer value) {
      return value(value);
   }
   
   public QueryPageStart set(int value) {
      return value(value);
   }
   
}
