package jedi.db.exceptions;

/**
 * Responsável por empacotar exceções de banco de dados
 * em uma implementação única e independente do driver de conexão 
 * com o banco de dados.
 * 
 * @author thiago
 * @version v1.0.0 08/03/2017
 * @since v1.0.0
 */
public class DatabaseException extends RuntimeException {
   
   private static final long serialVersionUID = 1L;
   
   public DatabaseException() {}
   
   public DatabaseException(String message) {
      super(message);
   }
   
   public DatabaseException(Throwable cause) {
      super(cause);
   }
   
   public DatabaseException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
