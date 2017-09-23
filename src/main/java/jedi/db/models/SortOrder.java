package jedi.db.models;

/**
 * Define a ordem de classificação de uma coluna em consultas SQL.
 * 
 * @author thiago
 * @version v1.0.0 23/03/2017
 * @since v1.0.0
 */
public enum SortOrder {
   
   ASC("Asc", "ASC"),
   DESC("Desc", "DESC"),
   ASCENDING("Ascending", "ASC"),
   DESCENDING("Descending", "DESC"),
   UNSORTED("Unsorted", "");
   
   private String label;
   private String value;
   
   private SortOrder(String label, String value) {
      this.label = label;
      this.value = value;
   }
   
   public String getLabel() {
      return label;
   }
   
   public String getValue() {
      return value;
   }
      
   public static boolean isAscending(QueryPageOrder orderBy) {
      if (orderBy == null || orderBy.getSortOrder() == null) {
         return false;
      } else if (orderBy.getSortOrder().equals(ASC) || orderBy.getSortOrder().equals(ASCENDING)) {
         return true;
      } else {
         return false;
      }
   }
   
   public static boolean isDescending(QueryPageOrder orderBy) {
      if (orderBy == null || orderBy.getSortOrder() == null) {
         return false;
      } else if (orderBy.getSortOrder().equals(DESC) || orderBy.getSortOrder().equals(DESCENDING)) {
         return true;
      } else {
         return false;
      }
   }
   
   public static boolean isAsc(QueryPageOrder orderBy) {
      return isAscending(orderBy);
   }
   
   public static boolean isDesc(QueryPageOrder orderBy) {
      return isDescending(orderBy);
   }
   
   public static boolean isUnsorted(QueryPageOrder orderBy) {
      if (orderBy == null || orderBy.getSortOrder().equals(UNSORTED)) {
         return true;
      }
      return false;
   }
   
}
