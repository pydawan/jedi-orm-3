package jedi.db.sql;

/**
 * @author thiago-amm
 * @version v1.0.0 23/09/2017
 * @since v1.0.0
 */
public class SQLBuilder {
   
   private StringBuffer sql;
   
   private SQLBuilder() {
      sql = new StringBuffer();
   }
   
   public static SQLBuilder sql() {
      return new SQLBuilder();
   }
   
   public void clear() {
      sql.delete(0, sql.length());
   }
   
   public String build() {
      String build = sql.toString();
      clear();
      return build;
   }
   
   public SQLBuilder select() {
      clear();
      sql.append("SELECT *");
      return this;
   }
   
   public SQLBuilder select(String... columns) {
      clear();
      sql.append("SELECT ");
      sql.append(String.join(", ", columns));
      return this;
   }
   
   public SQLBuilder from(String... tables) {
      sql.append(" FROM ");
      sql.append(String.join(", ", tables));
      return this;
   }
   
   public static String as(String table, String alias) {
      return String.format(table, alias);
   }
   
}
