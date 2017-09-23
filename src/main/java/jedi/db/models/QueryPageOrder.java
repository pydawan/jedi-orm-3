package jedi.db.models;

/**
 * Representa a ordem de classificação dos resultados de uma página.
 * 
 * @author thiago
 * @version v1.0.0 23/03/2017
 * @since v1.0.0
 */
public class QueryPageOrder {
   
   private String field;
   private SortOrder sortOrder;
   
   public QueryPageOrder(String field) {
      this(field, (SortOrder) null);
   }
   
   public QueryPageOrder(String field, SortOrder sortOrder) {
      try {
         field = field == null ? "" : field;
         if (field.isEmpty()) {
            throw new IllegalArgumentException("O atributo field não deve ser nulo ou vazio!");
         } else {
            this.field = field;
            this.sortOrder = sortOrder;
         }
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
   }
   
   public QueryPageOrder(String field, String sortOrder) {
      this(field, SortOrder.valueOf(sortOrder));
   }
   
   public String getField() {
      return field;
   }
   
   public String field() {
      return field;
   }
   
   public void setField(String field) {
      this.field = field;
   }
   
   public QueryPageOrder field(String field) {
      this.field = field;
      return this;
   }
   
   public SortOrder getSortOrder() {
      return sortOrder;
   }
   
   public SortOrder sortOrder() {
      return sortOrder;
   }
   
   public void setSortOrder(SortOrder sortOrder) {
      this.sortOrder = sortOrder;
   }
   
   public QueryPageOrder sortOrder(SortOrder sortOrder) {
      this.sortOrder = sortOrder;
      return this;
   }
   
   @Override
   public String toString() {
      return QueryPageOrder.get(this);
   }
   
   public static String get(QueryPageOrder orderBy) {
      String sorting = SortOrder.UNSORTED.toString();
      if (orderBy != null) {
         if (orderBy.getField() != null && !orderBy.getField().isEmpty()) {
            if (orderBy.getSortOrder() == null) {
               if (orderBy.getField().startsWith("-")) {
                  sorting = String.format("ORDER BY %s DESC", orderBy.getField().replace("-", ""));
               } else {
                  sorting = String.format("ORDER BY %s ASC", orderBy.getField());
               }
            } else {
               sorting = String.format("ORDER BY %s %s", orderBy.getField(), orderBy.getSortOrder().getValue());
            }
         }
      }
      return sorting;
   }
   
}
