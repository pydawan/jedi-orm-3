package jedi.db.exceptions;

/**
 * @author thiago
 * @version v1.0.0
 */
public class ValueException extends Exception {
   
   private static final long serialVersionUID = -1704658038013659162L;
   
   public ValueException() {
      super();
   }
   
   public ValueException(String message) {
      super(message);
   }
   
   public ValueException(Throwable cause) {
      super(cause);
   }
   
   public ValueException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
