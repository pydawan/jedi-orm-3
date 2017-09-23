package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 20/03/2017
 * @since v1.0.0
 */
public enum AutoClose {
   
   YES(true),
   NO(false);
   
   private final boolean value;
   
   private AutoClose(final boolean value) {
      this.value = value;
   }
   
   public static AutoClose of(boolean value) {
      return value == true ? YES : NO;
   }
   
   public static AutoClose of(String value) {
      if (value != null && value.matches("false|true")) {
         boolean value_ = Boolean.parseBoolean(value);
         return AutoClose.of(value_);
      } else {
         throw new IllegalArgumentException("A propriedade auto_close de jedi.properties só admite os valores false ou true!");
      }
   }
   
   public static boolean of(AutoCommit autoClose) {
      return autoClose.value();
   }
   
   public boolean isValue() {
      return value;
   }
   
   public boolean value() {
      return value;
   }
   
}
