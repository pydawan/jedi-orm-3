package jedi.db.models;

/**
 * @author thiago
 * @version v1.0.0 23/03/2017
 * @since v1.0.0
 */
public class QueryPage {
   
   private QueryPageStart start;
   private QueryPageSize size;
   private QueryPageOrder sort;
   private String[] filters;
   
   public QueryPage(QueryPageStart start, QueryPageSize size, QueryPageOrder sort, String... filters) {
      this.start = start;
      this.size = size;
      this.sort = sort;
      this.filters = filters;
   }
   
   public QueryPageStart getStart() {
      return start;
   }
   
   public void setStart(QueryPageStart start) {
      this.start = start;
   }
   
   public QueryPageSize getSize() {
      return size;
   }
   
   public void setSize(QueryPageSize size) {
      this.size = size;
   }
   
   public QueryPageOrder getSort() {
      return sort;
   }
   
   public void setSort(QueryPageOrder sort) {
      this.sort = sort;
   }
   
   public String[] getFilters() {
      return filters;
   }
   
   public void setFilters(String[] filters) {
      this.filters = filters;
   }
   
   public static QueryPageNumber number(Integer value) {
      return new QueryPageNumber(value);
   }
   
   public static QueryPageNumber pageNumber(Integer value) {
      return new QueryPageNumber(value);
   }
   
   public static QueryPageStart start(Integer value) {
      return new QueryPageStart(value);
   }
   
   public static QueryPageStart pageStart(Integer value) {
      return new QueryPageStart(value);
   }
   
   public static QueryPageSize size(Integer value) {
      return new QueryPageSize(value);
   }
   
   public static QueryPageSize pageSize(Integer value) {
      return new QueryPageSize(value);
   }
   
   public static QueryPageOrder orderBy(String field) {
      field = field == null ? "" : field;
      return new QueryPageOrder(field);
   }
   
   public static QueryPageOrder orderBy(String field, SortOrder order) {
      return new QueryPageOrder(field, order);
   }
   
   public static QueryPageOrder orderBy(String field, String sortOrder) {
      return new QueryPageOrder(field, sortOrder);
   }
   
   public static String[] filters(String... filters) {
      return PageFilter.filters(filters);
   }
   
   public static String[] pageFilters(String... filters) {
      return PageFilter.pageFilters(filters);
   }
   
}
