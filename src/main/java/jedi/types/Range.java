package jedi.types;

import java.util.ArrayList;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
public class Range extends ArrayList<Integer> {
   
   private static final long serialVersionUID = 1L;
   
   public int start;
   public int end;
   public int increment;
   public int[] values;
   
   public Range() {
      
   }
   
   public Range(int start, int end) {
      this(start, end, 1);
   }
   
   public Range(int start, int end, int increment) {
      this.start = start;
      this.end = end;
      this.increment = increment;
      
      for (int i = start; i < end; i += increment) {
         this.add(i);
      }
   }
   
   public Range(String range) {
      this(range, 0);
   }
   
   public Range(String range, int increment) {
      System.out.println(range);
   }
   
   public Range reverse() {
      Range r = new Range();
      
      for (int i = this.size() - 1; i >= 0; i--) {
         r.add(this.get(i));
      }
      return r;
   }
   
   public void each(Function<Integer> function) {
      int index = 0;
      
      if (function != null) {
         for (Integer object : this) {
            function.index = index++;
            function.value = object;
            function.run();
         }
      }
   }
   
}
