package jedi.db.validators;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
public class CharFieldValidator extends Validator {
   
   @Override
   public void validate() {
      if (this.getValue() instanceof String) {
         this.isValid = true;
      } else {
         this.isValid = false;
      }
   }
   
}
