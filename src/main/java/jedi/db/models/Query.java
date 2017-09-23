package jedi.db.models;

import static java.lang.String.format;

import jedi.db.util.TableUtil;

/**
 * DSL (Domain Specific Language) para definição de consultas
 * seguras, uma vez que substitui a manipulação direta de texto por
 * métodos que tornam o código type-safe.
 * 
 * @author thiago-amm
 * @version v1.0.0 18/07/2017
 * @version v1.0.1 22/09/2017
 * @since v1.0.0
 */
public class Query {
   
   public static final String FROM = "FROM %s";
   public static final String ASTERISK = "*";
   public static final String DISTINCT = "DISTINCT %s";
   public static final String WHERE = "WHERE %s";
   public static final String AND = "%s AND %s";
   public static final String OR = "%s OR %s";
   public static final String NOT = "NOT %s";
   public static final String ORDER_BY = "ORDER BY %s";
   public static final String SELECT_COLUMNS_FROM = "SELECT %s FROM %s";
   public static final String SELECT_ALL_COLUMNS_FROM = "SELECT * FROM %s";
   public static final String SELECT_DISTINCT = "SELECT DISTINCT %s FROM %s";
   public static final String SELECT_WHERE = "SELECT %s FROM %s WHERE %s";
   public static final String SELECT_ORDER_BY = "SELECT %s FROM %s ORDER BY %s";
   public static final String SELECT_ORDER_BY_ASC = "SELECT %s FROM %s ORDER BY %s ASC";
   public static final String SELECT_ORDER_BY_DESC = "SELECT %s FROM %s ORDER BY %s DESC";
   public static final String HAVING = "HAVING %s";
   public static final String SELECT_HAVING = "SELECT %s FROM %s WHERE %s GROUP BY %s HAVING %s";
   public static final String SELECT_HAVING_ORDER_BY = "SELECT %s FROM %s WHERE %s GROUP BY %s HAVING %s ORDER BY %s";
   public static final String INSERT_INTO = "INSERT INTO %s (%s) VALUES (%s)";
   public static final String IS_NULL = "%s IS NULL";
   public static final String IS_NOT_NULL = "%s IS NOT NULL";
   public static final String SELECT_IS_NULL = "SELECT %s FROM %s WHERE %s IS NULL";
   public static final String SELECT_IS_NOT_NULL = "SELECT %s FROM %s WHERE %s IS NOT NULL";
   public static final String UPDATE = "UPDATE %s SET %s WHERE %s";
   public static final String DELETE = "DELETE FROM %s WHERE %s";
   public static final String LIMIT = "LIMIT %d";
   public static final String SELECT_LIMIT = "SELECT %s FROM %s WHERE %s LIMIT %d";
   public static final String MIN = "MIN(%s)";
   public static final String SELECT_MIN = "SELECT MIN(%s) FROM %s WHERE %s";
   public static final String MAX = "MAX(%s)";
   public static final String SELECT_MAX = "SELECT MAX(%s) FROM %s WHERE %s";
   public static final String COUNT = "COUNT(%s)";
   public static final String SELECT_COUNT = "SELECT COUNT(%s) FROM %s WHERE %s";
   public static final String AVG = "AVG(%s)";
   public static final String SELECT_AVG = "SELECT AVG(%s) FROM %s WHERE %s";
   public static final String SUM = "SUM(%s)";
   public static final String SELECT_SUM = "SELECT SUM(%s) FROM %s WHERE %s";
   public static final String LIKE = "LIKE %s";
   public static final String SELECT_LIKE = "SELECT %s FROM %s WHERE %s LIKE %s";
   public static final String PERCENT_WILDCARD = "%%";
   public static final String UNDERSCORE_WILDCARD = "_";
   public static final String LIKE_STARTS_WITH = "LIKE %s%%";
   public static final String STARTS_WITH = "LIKE %s%%";
   public static final String NOT_LIKE_STARTS_WITH = "NOT LIKE %s%%";
   public static final String NOT_STARTS_WITH = "NOT LIKE %s%%";
   public static final String ENDS_WITH = "LIKE %%%s";
   public static final String NOT_ENDS_WITH = "NOT LIKE %%%s";
   public static final String LIKE_CONTAINS = "LIKE %%%s%%";
   public static final String CONTAINS = "LIKE %%%s%%";
   public static final String NOT_LIKE_CONTAINS = "NOT LIKE %%%s%%";
   public static final String NOT_CONTAINS = "NOT LIKE %%%s%%";
   public static final String LIKE_STARTS_WITH_AND_ENDS_WITH = "LIKE %s%%%s";
   public static final String STARTS_WITH_AND_ENDS_WITH = "LIKE %s%%%s";
   public static final String NOT_LIKE_STARTS_WITH_AND_ENDS_WITH = "NOT LIKE %s%%%s";
   public static final String NOT_STARTS_WITH_AND_ENDS_WITH = "NOT LIKE %s%%%s";
   public static final String SELECT_LIKE_STARTS_WITH = "SELECT %s FROM %s WHERE %s LIKE %s%%";
   public static final String SELECT_STARTS_WITH = "SELECT %s FROM %s WHERE %s LIKE %s%%";
   public static final String SELECT_NOT_LIKE_STARTS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %s%%";
   public static final String SELECT_NOT_STARTS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %s%%";
   public static final String SELECT_LIKE_ENDS_WITH = "SELECT %s FROM %s WHERE %s LIKE %%%s";
   public static final String SELECT__ENDS_WITH = "SELECT %s FROM %s WHERE %s LIKE %%%s";
   public static final String SELECT_LIKE_NOT_ENDS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %%%s";
   public static final String SELECT_NOT_ENDS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %%%s";
   public static final String SELECT_LIKE_CONTAINS = "SELECT %s FROM %s WHERE %s LIKE %%%s%%";
   public static final String SELECT_CONTAINS = "SELECT %s FROM %s WHERE %s LIKE %%%s%%";
   public static final String SELECT_NOT_LIKE_CONTAINS = "SELECT %s FROM %s WHERE %s NOT LIKE %%%s%%";
   public static final String SELECT_NOT_CONTAINS = "SELECT %s FROM %s WHERE %s NOT LIKE %%%s%%";
   public static final String SELECT_LIKE_STARTS_WITH_AND_ENDS_WITH = "SELECT %s FROM %s WHERE %s LIKE %s%%%s";
   public static final String SELECT_STARTS_WITH_AND_ENDS_WITH = "SELECT %s FROM %s WHERE %s LIKE %s%%%s";
   public static final String SELECT_NOT_LIKE_STARTS_WITH_AND_ENDS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %s%%%s";
   public static final String SELECT_NOT_STARTS_WITH_AND_ENDS_WITH = "SELECT %s FROM %s WHERE %s NOT LIKE %s%%%s";
   public static final String IN = "IN (%s)";
   public static final String SELECT_IN = "SELECT %s FROM %s WHERE %s IN (%s)";
   public static final String BETWEEN = "BETWEEN %s AND %s";
   public static final String SELECT_BETWEEN = "SELECT %s FROM %s WHERE %s BETWEEN %s AND %s";
   public static final String AS = "%s AS %s";
   public static final String INNER_JOIN = "%s INNER JOIN %s";
   public static final String ON = "ON %s.%s = %s.%s";
   public static final String SELECT_INNER_JOIN = "SELECT %s FROM %s INNER JOIN %s ON %s.%s = %s.%s";
   public static final String LEFT_JOIN = "%s LEFT JOIN %s";
   public static final String SELECT_LEFT_JOIN = "SELECT %s FROM %s LEFT JOIN %s ON %s.%s = %s.%s";
   public static final String RIGHT_JOIN = "%s RIGHT JOIN %s";
   public static final String SELECT_RIGHT_JOIN = "SELECT %s FROM %s RIGHT JOIN %s ON %s.%s = %s.%s";
   public static final String FULL_OUTER_JOIN = "%s FULL OUTER JOIN %s";
   public static final String SELECT_FULL_OUTER_JOIN = "SELECT %s FROM %s FULL OUTER JOIN %s ON %s.%s = %s.%s";
   public static final String UNION = "%s UNION %s";
   public static final String SELECT_UNION = "SELECT %s FROM %s UNION SELECT %s FROM %s";
   public static final String EXISTS = "EXISTS (%s)";
   public static final String SELECT_EXISTS = "SELECT %s FROM %s WHERE EXISTS (%s)";
   public static final String ANY = "ANY (%s)";
   public static final String SELECT_ANY = "SELECT %s FROM %s WHERE %s ANY (%s)";
   public static final String ALL = "ALL (%s)";
   public static final String SELECT_ALL = "SELECT %s FROM %s WHERE %s ALL (%s)";
   public static final String SELECT_INTO = "SELECT * INTO %s FROM %s";
   public static final String SELECT_INTO_IN = "SELECT * INTO %s IN %s FROM %s";
   public static final String SELECT_INTO_WHERE = "SELECT * INTO %s FROM %s WHERE %s";
   public static final String SELECT_INTO_IN_WHERE = "SELECT * INTO %s IN %s FROM %s WHERE %s";
   public static final String INSERT_INTO_SELECT = "INSERT INTO %s SELECT * FROM %s";
   public static final String INSERT_INTO_SELECT_WHERE = "INSERT INTO %s SELECT * FROM %s WHERE %s";
   public static final String SINGLE_LINE_COMMENT = "-- %s\n";
   public static final String MULTIPLE_LINE_COMMENT = "/*\n%s*\n/";
   public static final String USE_DATABASE = "USE %s";
   public static final String CREATE_DATABASE = "CREATE DATABASE %s";
   public static final String DROP_DATABASE = "DROP DATABASE %s";
   public static final String CREATE_TABLE = "CREATE TABLE %s (%s)";
   public static final String CREATE_TABLE_AS_SELECT_FROM = "CREATE TABLE %s AS SELECT %s FROM %s";
   public static final String CREATE_TABLE_AS_SELECT_FROM_WHERE = "CREATE TABLE %s AS SELECT %s FROM %s WHERE %s";
   public static final String DROP_TABLE = "DROP TABLE %s";
   public static final String TRUNCATE_TABLE = "TRUNCATE TABLE %s";
   public static final String ALTER_TABLE_ADD_COLUMN = "ALTER TABLE %s ADD %s %s";
   public static final String ALTER_TABLE_DROP_COLUMN = "ALTER TABLE %s DROP COLUMN %s";
   public static final String ALTER_TABLE_MODIFY_COLUMN = "ALTER TABLE %s MODIFY COLUMN %s %s";
   // Constraints
   public static final String NOT_NULL = "NOT NULL";
   public static final String UNIQUE = "UNIQUE";
   public static final String PRIMARY_KEY = "PRIMARY KEY";
   public static final String PRIMARY_KEY_COLUMN = "PRIMARY KEY (%s)";
   public static final String COLUMN_DECLARATION_PRIMARY_KEY_CONSTRAINT = "%s %s %s PRIMARY KEY";
   public static final String ALTER_TABLE_ADD_PRIMARY_KEY = "ALTER TABLE %s ADD PRIMARY KEY (%s)";
   public static final String ALTER_TABLE_ADD_PRIMARY_KEY_CONSTRAINT = "ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY (%s)";
   public static final String ALTER_TABLE_DROP_PRIMARY_KEY = "ALTER TABLE %s DROP PRIMARY KEY";
   public static final String ALTER_TABLE_DROP_PRIMARY_KEY_CONSTRAINT = "ALTER TABLE %s DROP CONSTRAINT %s";
   public static final String NOT_NULL_COLUMN = "%s NOT NULL";
   public static final String COLUMN_DECLARATION = "%s %s %s";
   public static final String UNIQUE_COLUMN = "UNIQUE (%s)";
   public static final String COLUMN_DECLARATION_UNIQUE_CONSTRAINT = "%s %s %s UNIQUE";
   public static final String ALTER_TABLE_ADD_UNIQUE_CONSTRAINT = "ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)";
   public static final String ALTER_TABLE_DROP_UNIQUE_CONSTRAINT = "ALTER TABLE %s DROP INDEX %s";
   public static final String FOREIGN_KEY = "FOREIGN KEY";
   public static final String COLUMN_DECLARATION_FOREIGN_KEY = "%s %s FOREIGN KEY REFERENCES %s (%s)"; 
   public static final String FOREIGN_KEY_COLUMN = "FOREIGN KEY (%s) REFERENCES %s (%s)";
   public static final String COLUMN_DECLARATION_FOREIGN_KEY_CONSTRAINT = "CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)";
   public static final String ALTER_TABLE_ADD_FOREIGN_KEY = "ALTER TABLE %s ADD FOREIGN KEY (%s) REFERENCES %s (%s)";
   public static final String ALTER_TABLE_ADD_FOREIGN_KEY_CONSTRAINT = "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)";
   public static final String ALTER_TABLE_DROP_FOREIGN_KEY = "ALTER TABLE %s DROP FOREIGN KEY %s";
   public static final String ALTER_TABLE_DROP_FOREIGN_KEY_CONSTRAINT = "ALTER TABLE %s DROP CONSTRAINT %s";
   public static final String COLUMN_CHECK_DECLARATION = "CHECK (%s)";
   public static final String COLUMN_CHECK_CONSTRAINT_DECLARATION = "CONSTRAINT %s CHECK (%s)";
   public static final String ALTER_TABLE_ADD_CHECK = "ALTER TABLE %s ADD CHECK (%s)";
   public static final String ALTER_TABLE_ADD_CHECK_CONSTRAINT = "ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s)";
   public static final String ALTER_TABLE_DROP_CHECK = "ALTER TABLE %s DROP CHECK %s";
   public static final String ALTER_TABLE_DROP_CHECK_CONSTRAINT = "ALTER TABLE %s DROP CONSTRAINT %s";
   public static final String COLUMN_DECLARATION_DEFAULT = "%s DEFAULT %s";
   public static final String ALTER_TABLE_SET_DEFAULT = "ALTER TABLE %s ALTER %s SET DEFAULT %s";
   public static final String ALTER_TABLE_ALTER_COLUMN_SET_DEFAULT = "ALTER TABLE %s ALTER COLUMN %s SET DEFAULT %s";
   public static final String ALTER_TABLE_MODIFY_COLUMN_SET_DEFAULT = "ALTER TABLE %s MODIFY %s DEFAULT %s";
   public static final String ALTER_TABLE_DROP_DEFAULT = "ALTER TABLE %s ALTER %s DROP DEFAULT";
   public static final String ALTER_TABLE_ALTER_COLUMN_DROP_DEFAULT = "ALTER TABLE %s ALTER COLUMN %s DROP DEFAULT";
   public static final String CREATE_INDEX = "CREATE INDEX %s ON %s (%s)";
   public static final String CREATE_UNIQUE_INDEX = "CREATE UNIQUE INDEX %s ON %s (%s)";
   public static final String ALTER_TABLE_DROP_INDEX = "ALTER TABLE %s DROP INDEX %s";
   public static final String DROP_INDEX = "DROP INDEX %s";
   public static final String COLUMN_DECLARATION_AUTO_INCREMENT = "%s AUTO_INCREMENT";
   public static final String ALTER_TABLE_CHANGE_AUTO_INCREMENT = "ALTER TABLE %s AUTO_INCREMENT=%d";
   
   // REFERÊNCIA: https://www.w3schools.com/sql/default.asp
   
   public static String selectFrom(String table) {
      String sql = "";
      table = table == null ? "" : table;
      if (!table.isEmpty()) {
         sql = String.format(SELECT_ALL_COLUMNS_FROM, table);
      }
      return sql;
   }
   
   public static <T extends Model> String selectFrom(Class<T> clazz) {
      return selectFrom(TableUtil.getTableName(clazz));
   }
   
   private static void checkArgument(String field, Object value) {
      if (value == null) {
         String message = format("O parâmetro %s não pode ser null!", field);
         throw new IllegalArgumentException(message);
      }
   }
   
   public static boolean ignoreCase(boolean value) {
      return value;
   }
   
   public static boolean ignoreCase() {
      return true;
   }
   
   public static boolean notIgnoreCase() {
      return false;
   }
   
   public static String and(String leftOperand, String rightOperand) {
      return format("%s AND %s", leftOperand, rightOperand);
   }
   
   public static String and() {
      return " AND";
   }
   
   public static String or(String leftOperand, String rightOperand) {
      return format("%s OR %s", leftOperand, rightOperand);
   }
   
   public static String or() {
      return " OR";
   }
   
   public static String not() {
      return " NOT";
   }
   
   public static String not(String operand) {
      return format(" NOT %s", operand);
   }
   
   public static String startsWith(String field, Object value, boolean ignoreCase) throws IllegalArgumentException {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__%sstartswith='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String startsWith(String field, Object value) {
      return startsWith(field, value, false);
   }
   
   public static String notStartsWith(String field, Object value, boolean ignoreCase) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__!%sstartswith='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String notStartsWith(String field, Object value) {
      return notStartsWith(field, value, false);
   }
   
   public static String contains(String field, Object value, boolean ignoreCase) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__%scontains='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String contains(String field, Object value) {
      return contains(field, value, false);
   }
   
   public static String notContains(String field, Object value, boolean ignoreCase) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__!%scontains='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String notContains(String field, Object value) {
      return contains(field, value, false);
   }
   
   public static String endsWith(String field, Object value, boolean ignoreCase) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__%sendswith='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String endsWith(String field, Object value) {
      return endsWith(field, value);
   }
   
   public static String notEndsWith(String field, Object value, boolean ignoreCase) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__%sendswith='%s'", field, ignoreCase ? "i" : "", value);
   }
   
   public static String notEndsWith(String field, Object value) {
      return notEndsWith(field, value, false);
   }
   
   public static String in(String field, Object... list) {
      checkArgument("field", field);
      checkArgument("list", list);
      String values = "";
      for (Object value : list) {
         value = value == null ? "" : value;
         values += value + ", ";
      }
      values = values.endsWith(", ") ? values.substring(0, values.length() - 2) : values;
      return format("%s__in=[%s]", field, values);
   }
   
   public static String notIn(String field, Object... list) {
      return in(field, list).replace("__in", "__!in");
   }
   
   public static String inRange(String field, Object start, Object end) {
      checkArgument("field", field);
      checkArgument("start", start);
      checkArgument("end", end);
      return format("%s__range=[%s, %s]", field, start, end);
   }
   
   public static String notInRange(String field, Object start, Object end) {
      return inRange(field, start, end).replace("__range", "__!range");
   }
   
   public static String between(String field, Object start, Object end) {
      return inRange(field, start, end);
   }
   
   public static String notBetween(String field, Object start, Object end) {
      return notInRange(field, start, end);
   }
   
   public static String lessThan(String field, Object value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__lt=%s", field, value);
   }
   
   public static String notLessThan(String field, Object value) {
      return lessThan(field, value).replace("__lt", "!__lt");
   }
   
   public static String lessThanOrEqual(String field, Object value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__lte=%s", field, value);
   }
   
   public static String notLessThanOrEqual(String field, Object value) {
      return lessThanOrEqual(field, value).replace("__lte", "__!lte");
   }
   
   public static String greatherThan(String field, Object value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__gt=%s", field, value);
   }
   
   public static String greatherThanOrEqual(String field, Object value) {
      return greatherThan(field, value).replace("__gt", "__gte");
   }
   
   public static String notGreatherThan(String field, Object value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__!gt=%s", field, value);
   }
   
   public static String notGreatherThanOrEqual(String field, Object value) {
      return greatherThanOrEqual(field, value).replace("__gte", "__!gte");
   }
   
   public static String lt(String field, Object value) {
      return lessThan(field, value);
   }
   
   public static String nlt(String field, Object value) {
      return notLessThan(field, value);
   }
   
   public static String lte(String field, Object value) {
      return lessThanOrEqual(field, value);
   }
   
   public static String nlte(String field, Object value) {
      return notLessThanOrEqual(field, value);
   }
   
   public static String gt(String field, Object value) {
      return greatherThan(field, value);
   }
   
   public static String ngt(String field, Object value) {
      return notGreatherThan(field, value);
   }
   
   public static String gte(String field, Object value) {
      return greatherThanOrEqual(field, value);
   }
   
   public static String ngte(String field, Object value) {
      return notGreatherThanOrEqual(field, value);
   }
   
   public static String exact(String field, Object value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__exact=%s", field, value);
   }
   
   public static String exact(String field, Model value) {
      checkArgument("field", field);
      checkArgument("value", value);
      return format("%s__exact=%s", field, value.id());
   }
   
   public static String notExact(String field, Object value) {
      return exact(field, value).replace("__exact", "__!exact");
   }
   
   public static String notExact(String field, Model value) {
      return exact(field, value).replace("__exact", "__!exact");
   }
   
   public static String equal(String field, Object value) {
      return exact(field, value);
   }
   
   public static String notEqual(String field, Object value) {
      return notExact(field, value);
   }
   
   public static String eq(String field, Object value) {
      return equal(field, value);
   }
   
   public static String neq(String field, Object value) {
      return notEqual(field, value);
   }
   
   public static String isNull(String field) {
      checkArgument("field", field);
      return format("%s__isnull=true", field);
   }
   
   public static String isNotNull(String field) {
      checkArgument("field", field);
      return format("%s__isnull=false", field);
   }
   
}
