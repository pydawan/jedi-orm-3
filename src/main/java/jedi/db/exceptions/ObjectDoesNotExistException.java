package jedi.db.exceptions;

/**
 * Exceção que é lançada quando nenhum objeto é
 * retornado por uma consulta ao banco de dados.
 *
 * @author thiago
 * @version v1.0.0
 */
public class ObjectDoesNotExistException extends RuntimeException {
   
   private static final long serialVersionUID = 5830724599642403525L;
   
   public ObjectDoesNotExistException() {
      super();
   }
   
   public ObjectDoesNotExistException(String message) {
      super(message);
   }
   
   public ObjectDoesNotExistException(String message, Throwable cause) {
      super(message, cause);
   }
   
   public ObjectDoesNotExistException(Throwable cause) {
      super(cause);
   }
   
}
