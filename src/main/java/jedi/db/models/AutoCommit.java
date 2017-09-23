package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 20/03/2017
 * @since v1.0.0
 */
public enum AutoCommit {
   
   YES(true),
   NO(false);
   
   private final boolean value;
   
   private AutoCommit(final boolean value) {
      this.value = value;
   }
   
   public static AutoCommit of(boolean value) {
      return value == true ? YES : NO;
   }
   
   public static AutoCommit of(String value) {
      if (value != null && value.matches("false|true")) {
         boolean value_ = Boolean.parseBoolean(value);
         return AutoCommit.of(value_);
      } else {
         throw new IllegalArgumentException("ATENÇÃO: A propriedade auto_commit de jedi.properties só admite os valores false ou true!");
      }
   }
   
   public static boolean of(AutoCommit autoCommit) {
      return autoCommit.value();
   }
   
   public boolean isValue() {
      return value;
   }
   
   public boolean value() {
      return value;
   }
   
}
