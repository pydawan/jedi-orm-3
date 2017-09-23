package jedi.db.exceptions;

/**
 * @author thiago
 * @version v1.0.0
 */
public class IndexException extends Exception {
   
   private static final long serialVersionUID = 1833617649397053868L;
   
   public IndexException() {
      super("list index out of range");
   }
   
   public IndexException(String message) {
      super(message);
   }
   
   public IndexException(Throwable cause) {
      super(cause);
   }
   
   public IndexException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
