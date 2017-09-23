package jedi.types;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 * @param <T>
 */
public class Function<T> implements Runnable {
   
   public int index;
   public T value;
   public List<T> values = new ArrayList<T>();
   public Object[] objects = null;
   public Object object = null;
   
   public Function() {
      
   }
   
   @SuppressWarnings("unchecked")
   public <E> Function(E... objects) {
      this.objects = objects;
   }
   
   public <E> Function(E object) {
      this.object = object;
   }
   
   public void run() {
   }
   
}
