package jedi.db.exceptions;

/**
 * @author thiago
 * @version v1.0.0
 */
public class InvalidFieldLookupException extends Exception {
   
   private static final long serialVersionUID = 1833617649397053868L;
   
   public InvalidFieldLookupException() {
      super("invalid field lookup");
   }
   
   public InvalidFieldLookupException(String message) {
      super(message);
   }
   
   public InvalidFieldLookupException(Throwable cause) {
      super(cause);
   }
   
   public InvalidFieldLookupException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
