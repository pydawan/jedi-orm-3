package jedi.types;

import java.util.HashMap;

/**
 * Representa um dicionário do Python.
 * 
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
@SuppressWarnings({ "serial" })
public class Dict extends HashMap<String, Object> {
   
   @SuppressWarnings("unchecked")
   public <T> T get(String key) {
      return (T) super.get(key);
   }
   
}
