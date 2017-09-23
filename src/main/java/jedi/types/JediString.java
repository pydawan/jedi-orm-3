package jedi.types;

import java.text.SimpleDateFormat;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
public class JediString {
   
   public static String capitalize(String text) {
      String s = "";
      if (text != null && !text.isEmpty()) {
         s = String.valueOf(text.charAt(0)).toUpperCase() + text.substring(1).toLowerCase();
      }
      return s;
   }
   
   public static String upper(String text) {
      String s = "";
      
      if (text != null && !text.isEmpty()) {
         s = text.toUpperCase();
      }
      return s;
   }
   
   public static String lower(String text) {
      String s = "";
      
      if (text != null && !text.isEmpty()) {
         s = text.toLowerCase();
      }
      return s;
   }
   
   public static String repeat(Object object, int amount) {
      String s = "";
      s = repeat(object, amount, false);
      return s;
   }
   
   public static String repeat(Object object, int amount, boolean vertical) {
      String s = "";
      s = JediString.repeat("", object, amount, vertical);
      return s;
   }
   
   public static String repeat(String format, Object object, int amount, boolean vertical) {
      String s = "";
      
      if (object != null && !object.toString().isEmpty() && amount > 0) {
         for (int i = 0; i < amount; i++) {
            if (vertical) {
               if (format == null || format.isEmpty()) {
                  s += object + "\n";
               } else {
                  s += String.format(format, object) + "\n";
               }
            } else {
               if (format == null || format.isEmpty()) {
                  s += object;
               } else {
                  s += String.format(format, object);
               }
            }
         }
         
         if (vertical) {
            s = s.substring(0, s.lastIndexOf("\n"));
         }
      }
      return s;
   }
   
   public static String toString(java.util.Date date, String format) {
      String _date = "";
      
      if (date != null) {
         if (format != null && !format.trim().isEmpty()) {
            SimpleDateFormat date_formatter = new SimpleDateFormat(format);
            _date = date_formatter.format(date);
         }
      }
      return _date;
   }
   
}
