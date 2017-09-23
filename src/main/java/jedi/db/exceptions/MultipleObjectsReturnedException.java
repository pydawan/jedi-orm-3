package jedi.db.exceptions;

/**
 * Exceção lançada quando mais de um objeto é retornado
 * por uma consulta no banco de dados.
 * 
 * @author thiago
 * @version v1.0.0
 */
public class MultipleObjectsReturnedException extends RuntimeException {
   
   private static final long serialVersionUID = 271849121410861140L;
   
   public MultipleObjectsReturnedException() {
      super();
   }
   
   public MultipleObjectsReturnedException(String message) {
      super(message);
   }
   
   public MultipleObjectsReturnedException(Throwable cause) {
      super(cause);
   }
   
   public MultipleObjectsReturnedException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
