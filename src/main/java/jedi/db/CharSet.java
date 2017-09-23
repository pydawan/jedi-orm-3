package jedi.db;

import static jedi.db.Collation.LATIN1_SWEDISH_CI;
import static jedi.db.Collation.UTF8_GENERAL_CI;

/**
 * @author thiagoamm
 * @version v1.0.0 11/08/2017
 * @since v1.0.0
 */
public enum CharSet {
   
   UTF8("utf8", "UTF-8 Unicode", UTF8_GENERAL_CI),
   LATIN1("latin1", "cp1252 West European", LATIN1_SWEDISH_CI);
   
   private final String value;
   private final String description;
   private final Collation collation;
   
   private CharSet(final String value, String description, Collation collation) {
      this.value = value;
      this.description = description;
      this.collation = collation;
   }
   
   private CharSet(final String value, Collation collation) {
      this.value = value;
      this.collation = collation;
      this.description = "";
   }
   
   public String getValue() {
      return value;
   }
   
   public String value() {
      return value;
   }
   
   public String getDescription() {
      return description;
   }
   
   public String description() {
      return description;
   }
   
   public Collation getCollation() {
      return collation;
   }
   
   public Collation collation() {
      return collation;
   }
   
}
