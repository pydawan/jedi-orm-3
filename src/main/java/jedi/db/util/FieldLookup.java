package jedi.db.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jedi.db.engine.JediEngine;
import jedi.db.models.Model;
import jedi.db.models.Regex;
import jedi.db.models.Table;

/**
 * @author Thiago Monteiro
 * @version 1.0
 */
public class FieldLookup {
   
   private static final Map<String, List<String>> EMPTY_TRANSLATION_MAP = new HashMap<>();
   
   /**
    * Método que valida uma condição de pesquisa (field lookup).
    * 
    * @param lookup
    *           condição de pesquisa.
    * @return false ou true.
    */
   public static boolean isValid(String lookup) {
      boolean valid = false;
      if (lookup != null) {
         lookup = lookup.trim();
         if (lookup.matches(Regex.FIELD_LOOKUP_EXACT.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_EXACT_DATE.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_EXACT_TIME.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_EXACT_DATETIME.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_CONTAINS.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_IN.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_GT.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_LT.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_STARTSWITH.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_ENDSWITH.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_RANGE_NUMBER.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_RANGE_DATE.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_RANGE_TIME.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_RANGE_DATETIME.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_YEAR.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_MONTH.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_DAY.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_WEEK_DAY.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_HOUR.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_MINUTE.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_SECOND.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_ISNULL.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_AND.getValue())) {
            return true;
         } else if (lookup.matches(Regex.FIELD_LOOKUP_OR.getValue())) {
            return true;
         } else {
            
         }
      }
      return valid;
   }
   
   /**
    * Método que traduz uma condição de pesquisa (field lookup) para SQL.
    * 
    * @param lookup
    *           condição de pesquisa.
    * @return condição em SQL.
    */
   public static String translateWhere(String lookup) {
      String sql = "";
      if (isValid(lookup)) {
         String _lookup = lookup.trim();
         String operator = "";
         _lookup = _lookup.replaceFirst(" *< *", "<");
         _lookup = _lookup.replaceFirst(" *= *", "=");
         _lookup = _lookup.replaceFirst(" *> *", ">");
         _lookup = _lookup.replaceFirst("^pk", "id");
         _lookup = _lookup.replaceAll("%", "\\%%");
         if (_lookup.matches("^.*(=|__i?exact=).*$")) {
            _lookup = _lookup.replaceFirst("__i?exact", "");
            if (_lookup.endsWith("null")) {
               operator = "is";
            } else {
               operator = "=";
            }
         }
         if (_lookup.matches("^.*<\\d+.*$")) {
            operator = "<";
         } else if (_lookup.matches("^.*<=\\d+.*$")) {
            operator = "<=";
         } else if (_lookup.matches("^.*>=\\d+.*$")) {
            operator = ">=";
         } else if (_lookup.matches("^.*>\\d+.*$")) {
            operator = ">";
         } else if (_lookup.matches("^.*__gt=\\d+.*$")) {
            operator = "gt";
            _lookup = _lookup.replace("__gt", "");
         } else if (_lookup.matches("^.*__gte=\\d+.*$")) {
            operator = "gte";
            _lookup = _lookup.replace("__gte", "");
         } else if (_lookup.matches("^.*__lt=\\d+.*$")) {
            operator = "lt";
            _lookup = _lookup.replace("__lt", "");
         } else if (_lookup.matches("^.*__lte=\\d+.*$")) {
            operator = "lte";
            _lookup = _lookup.replace("__lte", "");
         } else if (_lookup.matches("^.*__i?(startswith|contains|endswith)=.*$")) {
            operator = "like";
            _lookup = _lookup.replaceFirst("__i?(startswith|contains|endswith)", "");
         } else if (_lookup.matches("^.*__range=.*$")) {
            operator = "between";
            _lookup = _lookup.replace("__range", "");
         } else if (_lookup.matches("^.*__isnull=.*$")) {
            operator = "is";
            _lookup = _lookup.replace("__isnull", "");
         } else if (_lookup.matches("^.*__in=.*$")) {
            operator = "in";
            _lookup = _lookup.replace("__in", "");
         } else {
            _lookup = _lookup.replace("__year", "");
            _lookup = _lookup.replace("__month", "");
            _lookup = _lookup.replace("__day", "");
            _lookup = _lookup.replace("__weekday", "");
            _lookup = _lookup.replace("__hour", "");
            _lookup = _lookup.replace("__minute", "");
            _lookup = _lookup.replace("__second", "");
         }
         Object[] lookupComponents = new Object[2];
         if (operator.matches("<|<=|=|>=|>")) {
            lookupComponents = _lookup.split(operator);
         } else {
            lookupComponents = _lookup.split("=");
            switch (operator) {
               case "lt":
                  operator = "<";
                  break;
               case "lte":
                  operator = "<=";
                  break;
               case "gte":
                  operator = ">=";
                  break;
               case "gt":
                  operator = ">";
                  break;
            }
         }
         Object field = lookupComponents[0];
         Object value = lookupComponents[1];
         field = TableUtil.getColumnName(field.toString());
         value = value.toString().trim();
         if (!value.toString().matches("^\\d+|(false|true)$")) {
            if (!value.equals("null")) {
               if (value.toString().matches("[\\w ]+")) {
                  operator = "like";
               }
               if (lookup.matches(".*__iexact.*")) {
                  field = String.format("LOWER(%s)", field);
                  value = String.format("LOWER('%s')", value);
               } else if (lookup.matches(".*__i?contains.*")) {
                  if (lookup.contains("__icontains")) {
                     field = String.format("LOWER(%s)", field);
                     value = String.format("LOWER('%%%s%%')", value);
                  } else {
                     value = String.format("'%%%s%%'", value);
                  }
               } else if (lookup.contains("__in")) {
                  operator = "in";
                  String _value = value.toString();
                  _value = _value.replaceAll(" *\\[ *| *\\] *", "");
                  _value = _value.replaceAll(", *", ", ");
                  if (!_value.matches("\\d+(, \\d+)*")) {
                     _value = _value.replaceAll(", ", ",");
                     _value = _value.replaceAll("([\\p{L}\\d ]+)", "'$1'");
                     _value = _value.replaceAll(",", ", ");
                  }
                  value = _value;
                  value = String.format("(%s)", value);
               } else if (lookup.contains("__startswith")) {
                  value = String.format("'%s%%'", value);
               } else if (lookup.contains("__istartswith")) {
                  field = String.format("LOWER(%s)", field);
                  value = String.format("LOWER('%s%%')", value);
               } else if (lookup.contains("__endswith")) {
                  value = String.format("'%%%s'", value);
               } else if (lookup.contains("__iendswith")) {
                  field = String.format("LOWER(%s)", field);
                  value = String.format("LOWER('%%%s')", value);
               } else if (lookup.contains("__range")) {
                  String _value = value.toString();
                  _value = _value.replaceFirst(" *, *", ",");
                  _value = _value.replaceAll("\\(|\\)", "");
                  String[] values = _value.split(",");
                  if (!values[0].matches("\\d+")) {
                     values[0] = String.format("'%s'", values[0]);
                  }
                  if (!values[1].matches("\\d+")) {
                     values[1] = String.format("'%s'", values[1]);
                  }
                  value = String.format("%s and %s", values[0], values[1]);
               } else {
                  value = String.format("'%s'", value);
               }
            }
         } else {
            if (lookup.contains("__isnull")) {
               value = "NULL";
               if (lookup.endsWith("false")) {
                  operator = "IS NOT";
               }
            }
            if (operator.equalsIgnoreCase("like")) {
               value = String.format("'%s'", value);
            }
         }
         if (lookup.matches("and|or")) {
            sql = lookup.toUpperCase();
         } else {
            sql = String.format("%s %s %s", field, operator.toUpperCase(), value);
         }
      }
      return sql;
   }
   
   /**
    * @param fieldLookup
    * @return
    */
   public static String translateJoin(String fieldLookup) {
      StringBuilder join = new StringBuilder();
      fieldLookup = fieldLookup == null ? "" : fieldLookup.trim();
      if (!fieldLookup.isEmpty()) {
         if (fieldLookup.contains(".") && !fieldLookup.matches("^\\w+\\.(and|And|AND|or|Or|OR)$")) {
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+ *(=|<|<=|>|>=|!=) *.*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(startswith|istartswith|!startswith|!istartswith).*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(contains|icontains|!contains|!icontains).*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(endswith|iendswith|!endswith|!iendswith).*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(in|!in|range|!range).*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(lt|!lt|lte|!lte|gt|!gt|gte|!gte|exact|!exact).*)", "");
            fieldLookup = fieldLookup.replaceAll("(\\.\\w+__(isnull).*)", "");
            String[] entities = fieldLookup.split("\\.");
            for (int i = 0; i < entities.length - 1; i++) {
               join.append(FieldLookup.getJoin(String.format("%s.%s", entities[i], entities[i + 1])));
               if (i != entities.length - 2) {
                  join.append("\n");
               }
            }
         }
      }
      return join.toString();
   }
   
   public static String translateJoins(List<String> fieldLookups) {
      StringBuffer join = new StringBuffer();
      String sql = "";
      if (fieldLookups != null) {
         for (String fieldLookup : fieldLookups) {
            fieldLookup = fieldLookup == null ? "" : fieldLookup.trim();
            if (!fieldLookups.isEmpty() && fieldLookup.contains(".")) {
               String[] entities = fieldLookup.split("\\.");
               for (int i = 0; i < entities.length - 1; i++) {
                  sql = FieldLookup.getJoin(String.format("%s.%s", entities[i], entities[i + 1]));
                  join.append(sql);
               }
            }
         }
      }
      return join.toString();
   }
   
   /**
    * @param clazz
    * @param expression
    * @return
    */
   @SuppressWarnings("unchecked")
   public static <T extends Model> boolean isValidNavigation(Class<T> clazz, String expression) {
      boolean isValid = false;
      expression = expression == null ? "" : expression.trim();
      if (!expression.isEmpty()) {
         String attribute = expression.substring(0, expression.indexOf("."));
         expression = expression.replace(String.format("%s.", attribute), "");
         Field field = JediEngine.getField(attribute, clazz);
         Class<T> _clazz = null;
         if (JediEngine.isJediField(field)) {
            _clazz = (Class<T>) field.getDeclaringClass();
            System.out.println(_clazz);
         }
      }
      return isValid;
   }
   
   /**
    * @param lookups
    * @return
    */
   public static Map<String, List<String>> translate(String... lookups) {
      if (lookups != null) {
         Map<String, List<String>> map = new HashMap<>();
         List<String> joins = new ArrayList<>();
         map.put("join", new ArrayList<>());
         map.put("where", new ArrayList<>());
         String previousJoin = "";
         for (String lookup : lookups) {
            lookup = lookup.trim();
            if (lookup.matches("(and|AND|or|OR)")) {
               map.get("where").add(lookup.toUpperCase());
            } else {
               if (lookup.contains(".")) {
                  String join = lookup.substring(0, lookup.lastIndexOf("."));
                  String attribute = lookup.replace(String.format("%s.", join), "");
                  String table = lookup.replace(String.format(".%s", attribute), "");
                  table = table.substring(table.lastIndexOf(".") + 1);
                  String where = String.format("%s.%s", table, attribute);
                  if (!previousJoin.isEmpty() && join.contains(previousJoin)) {
                     String s = previousJoin.replaceFirst("\\.\\w+", "");
                     join = join.replace(String.format("%s.", s), "");
                  }
                  if (!joins.contains(join)) {
                     joins.add(join);
                     previousJoin = join;
                  }
                  if (!map.containsValue(where)) {
                     map.get("where").add(translateWhere(where));
                  }
               } else {
                  map.get("where").add(translateWhere(lookup));
               }
            }
         }
         joins.forEach(join -> map.get("join").add(translateJoin(join)));
         return map;
      }
      return EMPTY_TRANSLATION_MAP;
   }
   
   /**
    * @param clazz
    * @param lookups
    * @return
    */
   @Deprecated
   public static <T extends Model> Map<String, List<String>> _translate(Class<T> clazz, String... lookups) {
      if (lookups != null) {
         for (int i = 0; i < lookups.length; i++) {
            if (lookups[i].matches("^.*\\..*$")) {
               lookups[i] = String.format("%s.%s", clazz.getSimpleName().toLowerCase(), lookups[i]);
            } else if (lookups[i].matches("^.*=.*$")) {
               String[] array = lookups[i].trim().split("=");
               String field = null;
               if (array[0].contains("__")) {
                  field = array[0].split("__")[0].trim();
               } else {
                  field = array[0].trim();
               }
               Field f = JediEngine.getField(field, clazz);
               if (JediEngine.isOneToOneField(f) || JediEngine.isForeignKeyField(f)) {
                  lookups[i] = lookups[i].replaceFirst(field, String.format("%sId", field));
               }
            }
         }
      }
      return translate(lookups);
   }
   
   public static <T extends Model> Map<String, List<String>> translate(Class<T> clazz, String... lookups) {
      if (lookups != null) {
         Pattern pattern1 = Pattern.compile("^(\\w+\\.\\w+.*)+$");
         Pattern pattern2 = Pattern.compile("^.*=.*$");
         for (int i = 0; i < lookups.length; i++) {
            Matcher matcher1 = pattern1.matcher(lookups[i]);
            Matcher matcher2 = pattern2.matcher(lookups[i]);
            if (matcher1.matches()) {
               String className = clazz.getSimpleName().toLowerCase();
               lookups[i] = lookups[i].replaceAll("(\\w+\\.\\w+)", className + ".$1");
            } else if (matcher2.matches()) {
               String[] array = lookups[i].trim().split("=");
               String field = null;
               if (array[0].contains("__")) {
                  field = array[0].split("__")[0].trim();
               } else {
                  field = array[0].trim();
               }
               Field f = JediEngine.getField(field, clazz);
               if (JediEngine.isOneToOneField(f) || JediEngine.isForeignKeyField(f)) {
                  lookups[i] = lookups[i].replaceFirst(field, String.format("%sId", field));
               }
            }
         }
      }
      return translate(lookups);
   }
   
   @Deprecated
   public static String _getJoin(String fieldLookup) {
      fieldLookup = fieldLookup == null ? "" : fieldLookup.trim();
      if (!fieldLookup.isEmpty()) {
         String regex = "(\\w+)\\.(\\w+)";
         Pattern pattern = Pattern.compile(regex);
         Matcher matcher = pattern.matcher(fieldLookup);
         String format = "INNER JOIN\n\t%s AS %s ON $1.$2_id = $2.id";
         if (matcher.matches()) {
            String entity = matcher.group(2);
            String table = TableUtil.getTableName(JediEngine.getModel(entity));
            fieldLookup = fieldLookup.replaceFirst(regex, String.format(format, table, entity));
         }
      }
      return fieldLookup;
   }
   
   public static String getJoin(String fieldLookup) {
      fieldLookup = fieldLookup == null ? "" : fieldLookup.trim();
      if (!fieldLookup.isEmpty()) {
         String regex = "(\\w+)\\.(\\w+)";
         Pattern pattern = Pattern.compile(regex);
         Matcher matcher = pattern.matcher(fieldLookup);
         String format = "INNER JOIN\n\t%s AS %s ON %s.%s_id = %s.id\n";
         if (matcher.matches()) {
            String parentName = matcher.group(1);
            String childName = matcher.group(2);
            Table table = JediEngine.getTable(parentName, childName);
            String childTable = table != null ? table.name() : childName + "s";
            fieldLookup = String.format(format, childTable, childName, parentName, childName, childName);
         }
      }
      return fieldLookup;
   }
   
   /**
    * Decompõe uma EL (Expression Language) em partes.
    * 
    * @param el
    * @return
    */
   public static String[] decompose(String el) {
      el = el == null ? "" : el;
      if (el.matches("^\\w+.*(<|<=|=|>|>=|__).*$")) {
         return el.split("(<|<=|=|>|>=|__)");
      }
      return new String[] {};
   }
   
}
