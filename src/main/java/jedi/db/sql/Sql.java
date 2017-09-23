package jedi.db.sql;

import java.util.HashMap;

/**
 * Classe utilitária responsável pela construção de instruções SQL.
 * 
 * @author thiago-amm
 * @version v1.0.0 15/05/2017
 * @version v1.0.1 22/09/2017
 * @since v1.0.0
 */
public class Sql extends HashMap<String, Object> {
   
   private static final long serialVersionUID = 1L;
   
   public static final String SELECT = "SELECT";
   public static final String SELECT_DISTINCT = "SELECT DISTINCT";
   public static final String FROM = "FROM";
   public static final String WHERE = "WHERE";
   public static final String AND = "AND";
   public static final String OR = "OR";
   public static final String NOT = "NOT";
   public static final String ORDER_BY = "ORDER BY";
   public static final String ASC = "ASC";
   public static final String DESC = "DESC";
   public static final String INSERT_INTO = "INSERT INTO";
   public static final String NULL = "NULL";
   public static final String IS_NULL = "IS NULL";
   public static final String IS_NOT_NULL = "IS NOT NULL";
   public static final String UPDATE = "UPDATE";
   public static final String SET = "SET";
   public static final String DELETE = "DELETE";
   public static final String LIMIT = "LIMIT";
   public static final String MIN = "MIN";
   public static final String MAX = "MAX";
   public static final String COUNT = "COUNT";
   public static final String AVG = "AVG";
   public static final String SUM = "SUM";
   public static final String LIKE = "LIKE";
   public static final String IN = "IN";
   public static final String BETWEEN = "BETWEEN";
   public static final String AS = "AS";
   public static final String LEFT_JOIN = "LEFT JOIN";
   public static final String INNER_JOIN = "INNER JOIN";
   public static final String RIGHT_JOIN = "LEFT JOIN";
   public static final String FULL_JOIN = "FULL OUTER JOIN";
   public static final String UNION = "UNION";
   public static final String GROUP_BY = "GROUP BY";
   public static final String HAVING = "HAVING";
   public static final String EXISTS = "EXISTS";
   public static final String ANY = "ANY";
   public static final String ALL = "ALL";
   public static final String EQUAL = "=";
   public static final String EQ = "=";
   public static final String NOT_EQUAL = "!=";
   public static final String NE = "!=";
   public static final String DIFFERENT = "<>";
   public static final String DIFF = "<>";
   public static final String GREATER_THAN = ">";
   public static final String GT = ">";
   public static final String GREATER_THAN_OR_EQUAL_TO = ">=";
   public static final String GTE = ">=";
   public static final String LESS_THAN = "<";
   public static final String LT = "<";
   public static final String LESS_THAN_OR_EQUAL_TO = "<=";
   public static final String LTE = "<=";
   public static final String SINGLE_LINE_COMMENT = "--";
   public static final String OPEN_MULTILINE_COMMENT = "/*";
   public static final String CLOSE_MULTILINE_COMMENT = "*/";
   
   public Sql() {
      
   }
   
   public static Sql sql() {
      return new Sql();
   }
   
   public static class QueryExpression {
      
      private StringBuilder expressions;
      
      public QueryExpression() {
         expressions = new StringBuilder();
      }
      
      public QueryExpression expression(String expression) {
         if (expression != null && !expression.isEmpty()) {
            this.expressions.append(expression);
            if (this.expressions.length() > 1) {
               this.expressions.append(" ");
            }
         }
         return this;
      }
      
      public QueryExpression expressions(String... expressions) {
         if (expressions != null) {
            for (String expression : expressions) {
               expression(expression);
            }
         }
         return this;
      }
      
      public QueryExpression exp(String expression) {
         return expression(expression);
      }
      
      public QueryExpression exp(String... expressions) {
         return expressions(expressions);
      }
      
      public QueryExpression ex(String expression) {
         return expression(expression);
      }
      
      public QueryExpression ex(String... expressions) {
         return expressions(expressions);
      }
      
      public QueryExpression e(String expression) {
         return exp(expression);
      }
      
      public QueryExpression e(String... expressions) {
         return exp(expressions);
      }
      
      public QueryExpression cv(String expression) {
         return e(expression);
      }
      
      public QueryExpression and() {
         return exp("AND");
      }
      
      public QueryExpression or() {
         return exp("OR");
      }
      
      public QueryExpression eq() {
         return exp("=");
      }
      
      public QueryExpression neq() {
         return exp("<>");
      }
      
      public QueryExpression gt() {
         return exp(">");
      }
      
      public QueryExpression gte() {
         return exp(">=");
      }
      
      public QueryExpression lt() {
         return exp("<");
      }
      
      public QueryExpression lte() {
         return exp("<=");
      }
      
      public QueryExpression in() {
         return exp("IN");
      }
      
      public QueryExpression notIn() {
         return exp("NOT IN");
      }
      
      public QueryExpression isNull() {
         return exp("IS NULL");
      }
      
      public QueryExpression isNotNull() {
         return exp("IS NOT NULL");
      }
      
      public QueryExpression between() {
         return exp("BETWEEN");
      }
      
      public void clear() {
         expressions.setLength(0); // limpa o buffer.
      }
      
      @Override
      public String toString() {
         return expressions.toString();
      }
      
   }
   
   public class Select extends SqlStatement {
      
      private static final long serialVersionUID = 1L;
      private StringBuilder sql = new StringBuilder();
      
      public Select(String... columns) {
         sql.setLength(0); // limpa o builder.
         if (columns != null) {
            for (int i = 0; i < columns.length; i++) {
               String column = columns[i];
               if (column != null && !column.trim().isEmpty()) {
                  sql.append(column);
                  if (i < columns.length - 1) {
                     sql.append(", ");
                  }
               }
            }
         }
         this.put("columns", sql.toString());
         sql.setLength(0);
      }
      
      public Select from(String... tables) {
         sql.setLength(0);
         if (tables != null) {
            sql.append(" FROM ");
            for (int i = 0; i < tables.length; i++) {
               String table = tables[i];
               if (table != null && !table.trim().isEmpty()) {
                  sql.append(table);
                  if (i < tables.length - 1) {
                     sql.append(", ");
                  }
               }
            }
         }
         this.put("tables", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      public Select where(String... conditions) {
         sql.setLength(0);
         if (conditions != null) {
            sql.append(" WHERE ");
            for (int i = 0; i < conditions.length; i++) {
               String condition = conditions[i];
               if (condition != null && !condition.trim().isEmpty()) {
                  sql.append(condition);
                  if (i < conditions.length - 1) {
                     sql.append(" ");
                  }
               }
            }
         }
         this.put("where", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      public Select groupBy(String... columns) {
         sql.setLength(0);
         if (columns != null) {
            sql.append(" GROUP BY ");
            for (int i = 0; i < columns.length; i++) {
               String column = columns[i];
               if (column != null && !column.trim().isEmpty()) {
                  sql.append(column);
                  if (i < columns.length - 1) {
                     sql.append(", ");
                  }
               }
            }
         }
         this.put("groupBy", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      public Select orderBy(String... columns) {
         sql.setLength(0);
         if (columns != null) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < columns.length; i++) {
               String column = columns[i];
               if (column != null && !column.trim().isEmpty()) {
                  sql.append(column);
                  if (i < columns.length - 1) {
                     sql.append(", ");
                  }
               }
            }
         }
         this.put("orderBy", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      public Select limit(int number) {
         sql.setLength(0);
         if (number >= 0) {
            sql.append(" LIMIT ");
            sql.append(number);
         }
         this.put("limit", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      public Select offset(int number) {
         sql.setLength(0);
         if (number >= 0) {
            sql.append(" OFFSET ");
            sql.append(number);
         }
         this.put("offset", sql.toString());
         sql.setLength(0);
         return this;
      }
      
      @Override
      public String toString() {
         sql.setLength(0);
         String columns = this.get("columns") == null ? "" : "" + this.get("columns");
         String tables = this.get("tables") == null ? "" : "" + this.get("tables");
         String where = this.get("where") == null ? "" : "" + this.get("where");
         String groupBy = this.get("groupBy") == null ? "" : "" + this.get("groupBy");
         String orderBy = this.get("orderBy") == null ? "" : "" + this.get("orderBy");
         String limit = this.get("limit") == null ? "" : "" + this.get("limit");
         String offset = this.get("offset") == null ? "" : "" + this.get("offset");
         if (!columns.isEmpty() && !tables.isEmpty() && !where.isEmpty()) {
            sql.append("SELECT ");
            sql.append(columns);
            sql.append(tables);
            sql.append(where);
            sql.append(groupBy);
            sql.append(orderBy);
            sql.append(limit);
            sql.append(offset);
            sql.append(";");
         }
         this.put("sql", sql.toString());
         return "" + this.get("sql");
      }
      
   }
   
   public Select select(String... columns) {
      return new Select(columns);
   }
   
   public static QueryExpression expression() {
      return new QueryExpression();
   }
   
   public static QueryExpression exp() {
      return new QueryExpression();
   }
   
   public static QueryExpression ex() {
      return new QueryExpression();
   }
   
   public static QueryExpression e() {
      return new QueryExpression();
   }
   
   public static String expression(String format, Object... o) {
      return String.format(format, o);
   }
   
   public static String exp(String format, Object... o) {
      return String.format(format, o);
   }
   
}
