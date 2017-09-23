package jedi.db.models;

/**
 * Informa o tipo de tratamento de exceções.
 * 
 * @author thiago
 * @version v1.0.0 08/03/2017
 * @since v1.0.0
 */
public enum ExceptionHandling {
   
   THROW("throw"),
   PRINT("print");
   
   private final String strategy;
   
   private ExceptionHandling(final String strategy) {
      this.strategy = strategy;
   }
   
   public String getStrategy() {
      return strategy;
   }
   
   @Override
   public String toString() {
      return strategy;
   }
   
}
