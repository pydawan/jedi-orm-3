package jedi.db.exceptions;

/**
 * @author thiago
 * @version v1.0.0
 */
public class DoesNotExistException extends ObjectDoesNotExistException {
   
   private static final long serialVersionUID = 8663492663108944727L;
   
   public DoesNotExistException() {
      super();
   }
   
   public DoesNotExistException(String message) {
      super(message);
   }
   
   public DoesNotExistException(Throwable cause) {
      super(cause);
   }
   
   public DoesNotExistException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
