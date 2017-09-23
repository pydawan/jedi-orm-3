package jedi.db;

import jedi.db.engine.JediEngine;

/**
 * Classe principal ou de execução do Jedi ORM Framework.
 *
 * @author thiago
 * @version v1.0.0
 */
public abstract class Jedi {
   
   /**
    * @param args
    *           command line arguments
    */
   public static void main(String[] args) {
      try {
         if (args.length == 0) {
            JediEngine.syncdb(JediEngine.APP_SRC_DIR);
         } else if (args.length > 1) {
            if (args[0].equals("manage")) {
               JediEngine.manage(args);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
