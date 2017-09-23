package jedi.db.validators;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
public class IPAddressFieldValidator extends Validator {
   
   @Override
   public void validate() {
      Object value = this.getValue();
      String pattern = "\\d{1,3}+.\\d{1,3}+.\\d{1,3}+.\\d{1,3}+";
      if (value instanceof String && ((String) value).matches(pattern)) {
         this.isValid = true;
      } else {
         this.isValid = false;
      }
   }
   
}
