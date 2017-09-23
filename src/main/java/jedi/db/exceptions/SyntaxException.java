package jedi.db.exceptions;

/**
 * @author thiago
 * @version v1.0.0
 */
public class SyntaxException extends Exception {
   
   private static final long serialVersionUID = -737222008288985000L;
   
   public SyntaxException() {
      super();
   }
   
   public SyntaxException(String message) {
      super(message);
   }
   
   public SyntaxException(Throwable cause) {
      super(cause);
   }
   
   public SyntaxException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
