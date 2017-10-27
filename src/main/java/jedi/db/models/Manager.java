package jedi.db.models;

import static jedi.db.engine.JediEngine.AUTO_CLOSE;
import static jedi.db.engine.JediEngine.AUTO_COMMIT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT;
import static jedi.db.models.QueryPage.pageSize;
import static jedi.db.models.QueryPage.pageStart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jedi.db.connection.DataSource;
import jedi.db.engine.JediEngine;
import jedi.db.exceptions.DoesNotExistException;
import jedi.db.exceptions.MultipleObjectsReturnedException;
import jedi.db.exceptions.ObjectDoesNotExistException;
import jedi.db.util.FieldLookup;
import jedi.db.util.TableUtil;
import jedi.types.DateTime;

/**
 * Gerencia as consultas por registros de uma entidade (modelo)
 * no banco de dados.
 * 
 * @author thiago
 * @version v1.0.0
 * @version v1.0.1 16/02/2017
 * @version v1.0.2 25/05/2017
 * @since v1.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
public class Manager implements IManager {
   
   private static final QuerySet<? extends Model> EMPTY_QUERYSET = new QuerySet<>();
   private static final List<?> EMPTY_LIST = new ArrayList<>();
   private static final List<List<Map<String, Object>>> EMPTY_RESULT_SET = new ArrayList<>();
   
   private boolean autoCloseConnection = JediEngine.AUTO_CLOSE.isValue();
   
   private String tableName;
   private String entityName;
   private String exceptionMessage;
   private Connection connection;
   private List<List<Map<String, Object>>> resultSet = new ArrayList<>();
   private StringBuilder sql = new StringBuilder();
   
   public Class<? extends Model> entity;
   
   public Manager() {
      this(null, null);
   }
   
   public Manager(Connection connection) {
      this(null, connection);
   }
   
   public Manager(Class entity) {
      this(entity, null);
   }
   
   public Manager(Class entity, Connection connection) {
      this(entity, connection, true);
   }
   
   public Manager(Class<? extends Model> entity, Connection connection, boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
      if (entity != null && Model.class.isAssignableFrom(entity)) {
         this.entity = entity;
         tableName = TableUtil.getTableName(this.entity);
         entityName = this.entity.getSimpleName().toLowerCase();
      }
      if (connected()) {
         this.connection = connection;
      } else {
         if (!JediEngine.JEDI_PROPERTIES_LOADED) {
            JediEngine.loadJediProperties();
         }
         this.connection = DataSource.getConnection();
      }
   }
   
   public Manager(Class<? extends Model> entity, boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
      if (entity != null && Model.class.isAssignableFrom(entity)) {
         this.entity = entity;
         tableName = TableUtil.getTableName(this.entity);
      }
   }
   
   public Connection getConnection() {
      return connection;
   }
   
   public void setConnection(Connection connection) {
      this.connection = connection;
   }
   
   public Connection connection() {
      return connection;
   }
   
   public void connection(Connection connection) {
      this.connection = connection;
   }
   
   public String getTableName() {
      return tableName;
   }
   
   public void setTableName(String tableName) {
      this.tableName = TableUtil.getTableName(tableName);
   }
   
   public boolean getAutoCloseConnection() {
      return autoCloseConnection;
   }
   
   public void setAutoCloseConnection(boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
   }
   
   /**
    * Busca por todos os registros de uma entidade no banco de dados.
    * 
    * @param modelClass
    * @return
    */
   private <T extends Model> QuerySet<T> all(Class<T> modelClass) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      try {
         String sql = "SELECT * FROM";
         tableName = TableUtil.getTableName(modelClass);
         sql = String.format("%s %s", sql, tableName);
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         if (!resultSet.next()) {
            return querySet;
         }
         resultSet.beforeFirst();
         while (resultSet.next()) {
            Object obj = entity.newInstance();
            Field id = jedi.db.models.Model.class.getDeclaredField("id");
            id.setAccessible(true);
            // Oracle returns BigDecimal object.
            if (connection.toString().startsWith("oracle")) {
               id.set(
                     obj,
                     ((java.math.BigDecimal) resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1))).intValue());
            } else {
               // MySQL and PostgreSQL returns a Integer object.
               id.set(obj, resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)));
            }
            List<Field> _fields = JediEngine.getAllFields(this.entity);
            for (Field field : _fields) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.toString().substring(field.toString().lastIndexOf('.') + 1).equals("serialVersionUID")) {
                  continue;
               }
               if (field.getName().equalsIgnoreCase("objects")) {
                  continue;
               }
               oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
               foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
               manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
               FetchType fetchType = JediEngine.FETCH_TYPE;
               Manager manager = null;
               if (manyToManyFieldAnnotation != null) {
                  fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                  if (fetchType.equals(FetchType.EAGER)) {
                     String packageName = this.entity.getPackage().getName();
                     String model = manyToManyFieldAnnotation.model().getSimpleName();
                     model = Model.class.getSimpleName().equals(model) ? "" : model;
                     Class superClazz = null;
                     Class clazz = null;
                     if (model.isEmpty()) {
                        ParameterizedType genericType = null;
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           genericType = (ParameterizedType) field.getGenericType();
                           superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                           if (superClazz == Model.class) {
                              clazz = (Class) genericType.getActualTypeArguments()[0];
                              model = clazz.getSimpleName();
                           }
                        }
                     }
                     String references = manyToManyFieldAnnotation.references();
                     if (references == null || references.trim().isEmpty()) {
                        if (clazz != null) {
                           references = TableUtil.getTableName(clazz);
                        } else {
                           references = TableUtil.getTableName(model);
                        }
                     }
                     String intermediateModelclassName = String.format("%s.%s", packageName, model);
                     Class associatedModelClass = Class.forName(intermediateModelclassName);
                     manager = new Manager(associatedModelClass);
                     QuerySet querySetAssociatedModels = null;
                     String intermediateModelName = manyToManyFieldAnnotation.through().getSimpleName();
                     intermediateModelName = Model.class.getSimpleName().equals(intermediateModelName) ? "" : intermediateModelName;
                     if (intermediateModelName != null && !intermediateModelName.trim().isEmpty()) {
                        intermediateModelclassName = String.format("%s.%s", packageName, intermediateModelName);
                        Class intermediateModelClass = Class.forName(intermediateModelclassName);
                        String intermediateTableName = ((Model) intermediateModelClass.newInstance()).getTableName();
                        querySetAssociatedModels = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    intermediateTableName,
                                    TableUtil.getColumnName(obj.getClass()),
                                    ((Model) obj).getId()),
                              associatedModelClass);
                     } else {
                        querySetAssociatedModels = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    tableName,
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(obj.getClass()),
                                    ((Model) obj).getId()),
                              associatedModelClass);
                     }
                     field.set(obj, querySetAssociatedModels);
                  } else {
                     field.set(obj, null);
                  }
               } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                  if (oneToOneFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                  } else {
                     fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                  }
                  if (fetchType.equals(FetchType.EAGER)) {
                     // Recovers the attribute class.
                     Class associatedModelClass = Class.forName(field.getType().getName());
                     manager = new Manager(associatedModelClass);
                     String s = String.format("%s_id", TableUtil.getColumnName(field));
                     Object o = resultSet.getObject(s);
                     Model associatedModel = manager.get("id", o);
                     // References a model associated by a foreign key.
                     field.set(obj, associatedModel);
                  } else {
                     field.set(obj, null);
                  }
               } else {
                  // Sets the fields that aren't Model instances.
                  if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                        connection.toString().startsWith("oracle")) {
                     if (resultSet.getObject(TableUtil.getColumnName(field)) == null) {
                        field.set(obj, 0);
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        BigDecimal bigDecimal = (BigDecimal) resultSet.getObject(columnName);
                        field.set(obj, bigDecimal.intValue());
                     }
                  } else {
                     Object columnValue = resultSet.getObject(TableUtil.getColumnName(field));
                     columnValue = convertZeroDateToNull(columnValue);
                     if (columnValue instanceof java.sql.Date) {
                        java.sql.Date date = (java.sql.Date) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(date.getTime());
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof java.sql.Time) {
                        java.sql.Time time = (java.sql.Time) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time.getTime());
                        calendar.set(Calendar.YEAR, 0);
                        calendar.set(Calendar.MONTH, 0);
                        calendar.set(Calendar.DAY_OF_MONTH, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof Timestamp) {
                        Timestamp timestamp = (Timestamp) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp.getTime());
                        columnValue = calendar.getTime();
                     }
                     field.set(obj, columnValue);
                  }
               }
               manager = null;
            }
            T model = (T) obj;
            if (model != null) {
               model.setPersisted(true);
            }
            querySet.add(model);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(resultSet, statement, connection);
      }
      return querySet;
   }
   
   public <T extends Model> QuerySet<T> all() {
      return (QuerySet<T>) this.all(this.entity);
   }
   
   /**
    * Busca no banco de dados por todos os registros de uma entidade que
    * satisfaçam as condições informadas como parâmetro.
    * 
    * @param modelClass
    * @param fields
    * @return
    */
   private <T extends Model> QuerySet<T> _filter(Class<T> modelClass, String... fields) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      if (fields != null) {
         try {
            String sql = String.format("SELECT * FROM %s WHERE", tableName);
            String where = "";
            String fieldName = "";
            String fieldValue = "";
            // Iterates through the pairs field=value passed.
            for (int i = 0; i < fields.length; i++) {
               fields[i] = fields[i] == null ? "" : fields[i].trim();
               if (fields[i].isEmpty()) {
                  continue;
               }
               if (fields[i].equalsIgnoreCase("AND")) {
                  fields[i] = "AND";
               }
               if (fields[i].equalsIgnoreCase("OR")) {
                  fields[i] = "OR";
               }
               // Changes the name of the field to the corresponding pattern
               // name on the database.
               if (fields[i].contains("=")) {
                  fieldName = fields[i].substring(0, fields[i].lastIndexOf("="));
                  fieldName = TableUtil.getColumnName(fieldName);
                  fieldValue = fields[i].substring(fields[i].lastIndexOf("="));
                  fields[i] = String.format("%s%s", fieldName, fieldValue);
               }
               // Adds a blank space between the field name and value.
               fields[i] = fields[i].replace("=", " = ");
               // Replaces % by \%
               fields[i] = fields[i].replace("%", "\\%");
               // Adds a blank space between the values separated by commas.
               fields[i] = fields[i].replace(",", ", ");
               // Checks if the current pair contains __startswith, __contains
               // or __endswith.
               if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__!startswith") > -1 ||
                     fields[i].indexOf("__istartswith") > -1 || fields[i].indexOf("__!istartswith") > -1 ||
                     fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__icontains") > -1 || fields[i].indexOf("__!contains") > -1 ||
                     fields[i].indexOf("__!icontains") > -1 || fields[i].indexOf("__endswith") > -1 || fields[i].indexOf("__!endswith") > -1 ||
                     fields[i].indexOf("__iendswith") > -1 || fields[i].indexOf("__!iendswith") > -1) {
                  // Creates a LIKE statement in SQL.
                  if (fields[i].indexOf("__startswith") > -1) {
                     fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!startswith") > -1) {
                     fields[i] = fields[i].replace("__!startswith = ", " NOT LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__contains") > -1) {
                     fields[i] = fields[i].replace("__contains = ", " LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!contains") > -1) {
                     fields[i] = fields[i].replace("__!contains = ", " NOT LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__endswith") > -1) {
                     fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!endswith") > -1) {
                     fields[i] = fields[i].replace("__!endswith = ", " NOT LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  }
               }
               if (fields[i].indexOf("__in") > -1) {
                  // Creates a IN statement in SQL.
                  fields[i] = fields[i].replace("__in = ", " IN ");
                  // Replaces [] by ()
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__!in") > -1) {
                  // Creates a IN statement in SQL.
                  fields[i] = fields[i].replace("__!in = ", " NOT IN ");
                  // Replaces [] by ()
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__range") > -1) {
                  // Creates a BETWEEN statement in SQL.
                  fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                  // Removes [ or ] characters.
                  fields[i] = fields[i].replace("[", "");
                  fields[i] = fields[i].replace("]", "");
                  // Replaces , (comma character) by AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__!range") > -1) {
                  // Creates a BETWEEN statement in SQL.
                  fields[i] = fields[i].replace("__!range = ", " NOT BETWEEN ");
                  // Removes [ or ] characters.
                  fields[i] = fields[i].replace("[", "");
                  fields[i] = fields[i].replace("]", "");
                  // Replaces , (comma character) by AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__lt") > -1) {
                  fields[i] = fields[i].replace("__lt = ", " < ");
               }
               if (fields[i].indexOf("__!lt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
               }
               if (fields[i].indexOf("__lte") > -1) {
                  fields[i] = fields[i].replace("__lte = ", " <= ");
               }
               if (fields[i].indexOf("__!lte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
               }
               if (fields[i].indexOf("__gt") > -1) {
                  fields[i] = fields[i].replace("__gt = ", " > ");
               }
               if (fields[i].indexOf("__!gt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
               }
               if (fields[i].indexOf("__gte") > -1) {
                  fields[i] = fields[i].replace("__gte = ", " >= ");
               }
               if (fields[i].indexOf("__!gte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
               }
               if (fields[i].indexOf("__exact") > -1) {
                  fields[i] = fields[i].replace("__exact = ", " = ");
               }
               if (fields[i].indexOf("__!exact") > -1) {
                  fields[i] = fields[i].replace("__!exact = ", " != ");
               }
               if (fields[i].indexOf("__isnull") > -1) {
                  String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length()).trim();
                  if (bool.equalsIgnoreCase("true")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
                  }
                  if (bool.equalsIgnoreCase("false")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
                  }
                  fields[i] = fields[i].replace(bool, "");
               }
               where += fields[i] + " AND ";
               where = where.replace(" AND OR AND", " OR");
               where = where.replace(" AND AND AND", " AND");
            }
            where = where.substring(0, where.lastIndexOf("AND"));
            where = where.trim();
            sql = String.format("%s %s", sql, where);
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
               return querySet;
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               Object obj = entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  if (connection.toString().startsWith("oracle")) {
                     id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
                  } else {
                     id.set(obj, resultSet.getObject(id.getName()));
                  }
               }
               // Iterates through the fields of the model.
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field field : _fields) {
                  // Sets private or protected fields as accessible.
                  field.setAccessible(true);
                  // Discards non annotated fields
                  if (!JediEngine.isJediField(field)) {
                     continue;
                  }
                  // Discards the serialVersionUID field.
                  if (field.getName().equals("serialVersionUID"))
                     continue;
                  // Discards the objects field.
                  if (field.getName().equalsIgnoreCase("objects"))
                     continue;
                  // Checks if the field are annotated as OneToOneField,
                  // ForeignKeyField or ManyToManyField.
                  oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class superClazz = null;
                        Class clazz = null;
                        String packageName = this.entity.getPackage().getName();
                        String model = manyToManyFieldAnnotation.model().getSimpleName();
                        model = Model.class.getSimpleName().equals(model) ? "" : model;
                        if (model.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 model = clazz.getSimpleName();
                              }
                           }
                        }
                        String references = manyToManyFieldAnnotation.references();
                        if (references == null || references.trim().isEmpty()) {
                           if (clazz != null) {
                              references = TableUtil.getTableName(clazz);
                           } else {
                              references = TableUtil.getTableName(model);
                           }
                        }
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                        manager = new Manager(associatedModelClass);
                        List<List<Map<String, Object>>> recordSet = null;
                        // Performs a SQL query.
                        recordSet = manager.raw(
                              String.format(
                                    "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                                    TableUtil.getColumnName(model),
                                    tableName,
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(this.entity),
                                    ((Model) obj).id()));
                        String args = recordSet.toString();
                        args = args.replace("[", "");
                        args = args.replace("{", "");
                        args = args.replace("]", "");
                        args = args.replace("}", "");
                        args = args.replace("=", "");
                        args = args.replace(", ", ",");
                        args = args.replace(String.format("%s_id", TableUtil.getColumnName(model)), "");
                        args = String.format("id__in=[%s]", args);
                        QuerySet querySetAssociatedModels = manager._filter(args);
                        field.set(obj, querySetAssociatedModels);
                     } else {
                        field.set(obj, null);
                     }
                  } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                     if (oneToOneFieldAnnotation != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     }
                     if (fetchType.equals(FetchType.EAGER)) {
                        // If it's recovers the field's class.
                        Class associatedModelClass = Class.forName(field.getType().getName());
                        // Instanciates a Model Manager.
                        manager = new Manager(associatedModelClass);
                        String columnName = TableUtil.getColumnName(field);
                        Object id = resultSet.getObject(String.format("%s_id", columnName));
                        Model associatedModel = manager.get("id", id);
                        // Calls the get method recursivelly.
                        // References the model associated by foreign key
                        // annotation.
                        field.set(obj, associatedModel);
                     } else {
                        field.set(obj, null);
                     }
                  } else {
                     // Sets fields the aren't Model's instances.
                     if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        if (resultSet.getObject(TableUtil.getColumnName(field.getName())) == null) {
                           field.set(obj, 0);
                        } else {
                           String columnName = TableUtil.getColumnName(field.getName());
                           BigDecimal columnValue = (BigDecimal) resultSet.getObject(columnName);
                           field.set(obj, columnValue.intValue());
                        }
                     } else {
                        String columnName = TableUtil.getColumnName(field.getName());
                        Object columnValue = resultSet.getObject(columnName);
                        if (columnValue instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           columnValue = calendar.getTime();
                        }
                        field.set(obj, columnValue);
                     }
                  }
                  manager = null;
               }
               T model = (T) obj;
               if (model != null) {
                  model.setPersisted(true);
               }
               querySet.add(model);
            }
            if (querySet != null) {
               querySet.setPersisted(true);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(resultSet, statement, connection);
         }
      }
      return querySet;
   }
   
   public <T extends Model> QuerySet<T> _filter(String... fields) {
      return (QuerySet<T>) this._filter(this.entity, fields);
   }
   
   /**
    * Cria no banco de dados um novo registro da entidade.
    * Os campos e valores são passados como parâmetro.
    * 
    * @param modelClass
    * @param list
    * @return
    */
   private <T extends Model> T create(Class<T> modelClass, String... list) {
      Object obj = null;
      PreparedStatement statement = null;
      // TODO: verificar como funciona o create com OnetoOneField,
      // ForeignKeyField e ManyToManyField.
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      // ManyToManyField manyToManyFieldAnnotation = null;
      if (list != null) {
         try {
            String sql = String.format("INSERT INTO %s", tableName);
            String fields = "";
            String field = "";
            String values = "";
            String value = "";
            // Instanciates a model object managed by this Manager.
            obj = this.entity.newInstance();
            for (int i = 0; i < list.length; i++) {
               list[i] = list[i] == null ? "" : list[i].trim();
               if (list[i].isEmpty()) {
                  continue;
               }
               field = list[i].split("=")[0];
               value = list[i].split("=")[1];
               Field f = null;
               if (field.endsWith("_id")) {
                  f = JediEngine.getField(field.replace("_id", ""), this.entity);
               } else {
                  f = JediEngine.getField(field, this.entity);
               }
               // Changes the field name to reflect the pattern to the table
               // column names.
               field = String.format("%s", TableUtil.getColumnName(field));
               // Handles the insertion of the OneToOneField, ForeignKeyField or
               // ManyToManyField.
               oneToOneFieldAnnotation = f.getAnnotation(OneToOneField.class);
               foreignKeyFieldAnnotation = f.getAnnotation(ForeignKeyField.class);
               // manyToManyFieldAnnotation =
               // f.getAnnotation(ManyToManyField.class);
               // Allows access to the private and protected fields
               // (attributes).
               f.setAccessible(true);
               if (!JediEngine.isJediField(f)) {
                  continue;
               }
               // Discards serialVersionUID field.
               if (f.getName().equals("serialVersionUID"))
                  continue;
               // Discards objects field.
               if (f.getName().equalsIgnoreCase("objects"))
                  continue;
               // Converts the data to the appropriate type.
               if (value.matches("\\d+")) {
                  if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                     Manager manager = new Manager(f.getType());
                     f.set(obj, manager.get("id", value));
                  } else {
                     f.set(obj, Integer.parseInt(value)); // Integer
                  }
               } else if (value.matches("\\d+f")) { // Float
                  f.set(obj, Float.parseFloat(value));
               } else if (value.matches("\\d+.d+")) { // Double
                  f.set(obj, Double.parseDouble(value));
               } else if (f.getType().getName().equals("java.util.Date") || f.getType().getName().equals("jedi.types.DateTime")) {
                  // TODO - corrigir tratamento da data.
                  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                  Date date = formatter.parse(list[i].split("=")[1].replace("'", ""));
                  f.set(obj, date);
               } else { // String
                  f.set(obj, list[i].split("=")[1]);
               }
               fields += field + ", ";
               values += value + ", ";
            }
            fields = fields.substring(0, fields.lastIndexOf(","));
            values = values.substring(0, values.lastIndexOf(","));
            sql = String.format("%s (%s) VALUES (%s)", sql, fields, values);
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            statement = connection.prepareStatement(sql);
            statement.execute();
            commit();
            Field f = jedi.db.models.Model.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(obj, this.lastId());
            T model = (T) obj;
            if (model != null) {
               model.setPersisted(true);
            }
         } catch (Exception e) {
            e.printStackTrace();
            rollback();
         } finally {
            close(statement, connection);
         }
      }
      return (T) obj;
   }
   
   public <T extends Model> T create(String... list) {
      return (T) this.create(entity, list);
   }
   
   /**
    * Retorna o id do último registro persistido da entidade.
    */
   public int getLastInsertedID() {
      int id = 0;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
         String sql = "";
         String databaseEngine = JediEngine.DATABASE_ENGINE;
         if (databaseEngine != null) {
            if (databaseEngine.trim().equalsIgnoreCase("mysql") || databaseEngine.trim().equalsIgnoreCase("postgresql") ||
                  databaseEngine.trim().equalsIgnoreCase("h2")) {
               sql = String.format("SELECT id FROM %s ORDER BY id DESC LIMIT 1", tableName);
            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
               sql = String.format("SELECT MAX(id) AS id FROM %s", tableName);
            } else {
               
            }
         } else {
            return id;
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         while (resultSet.next()) {
            id = resultSet.getInt("id");
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(resultSet, statement, connection);
      }
      return id;
   }
   
   public int getLastId() {
      return getLastInsertedID();
   }
   
   public int lastId() {
      return getLastId();
   }
   
   /**
    * Conta a quantidade de registros da entidade persistidos
    * no banco de dados e que satisfação as condições passadas como parâmetro.
    */
   public int _count(String... conditions) {
      int rows = 0;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
         String sql = String.format("SELECT COUNT(id) AS \"rows\" FROM %s", tableName);
         if (conditions != null && conditions.length > 0) {
            String where = "WHERE";
            for (int i = 0; i < conditions.length; i++) {
               conditions[i] = conditions[i] == null ? "" : conditions[i].trim();
               if (!conditions[i].isEmpty()) {
                  if (conditions[i].equalsIgnoreCase("AND")) {
                     conditions[i] = "AND";
                  }
                  if (conditions[i].equalsIgnoreCase("OR")) {
                     conditions[i] = "OR";
                  }
                  /*
                   * Changes the name of the field to reflect the name
                   * pattern of the table columns.
                   */
                  if (conditions[i].contains("=")) {
                     String fieldName = conditions[i].substring(0, conditions[i].lastIndexOf("="));
                     String fieldValue = conditions[i].substring(conditions[i].lastIndexOf("="));
                     conditions[i] = String.format("%s%s", TableUtil.getColumnName(fieldName), fieldValue);
                  }
                  // Adds a blank space between the field's name and value.
                  conditions[i] = conditions[i].replace("=", " = ");
                  // Replaces % by \%
                  conditions[i] = conditions[i].replace("%", "\\%");
                  // Adds a blank space between the values separated by comma
                  // character.
                  conditions[i] = conditions[i].replace(",", ", ");
                  // Checks if the current pair contains __startswith,
                  // __contains or __endswith.
                  if (conditions[i].indexOf("__startswith") > -1 || conditions[i].indexOf("__!startswith") > -1 ||
                        conditions[i].indexOf("__istartswith") > -1 || conditions[i].indexOf("__!istartswith") > -1 ||
                        conditions[i].indexOf("__contains") > -1 || conditions[i].indexOf("__!contains") > -1 ||
                        conditions[i].indexOf("__icontains") > -1 || conditions[i].indexOf("__!icontains") > -1 ||
                        conditions[i].indexOf("__endswith") > -1 || conditions[i].indexOf("__!endswith") > -1 ||
                        conditions[i].indexOf("__iendswith") > -1 || conditions[i].indexOf("__!iendswith") > -1) {
                     // Creates the LIKE SQL statement.
                     if (conditions[i].indexOf("__startswith") > -1) {
                        conditions[i] = conditions[i].replace("__startswith = ", " LIKE ");
                        // Replaces 'value' by 'value%'.
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__!startswith") > -1) {
                        conditions[i] = conditions[i].replace("__!startswith = ", " NOT LIKE ");
                        // Replaces 'value' by 'value%'.
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__istartswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__!istartswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__contains") > -1) {
                        conditions[i] = conditions[i].replace("__contains = ", " LIKE ");
                        // Replaces 'value' by '%value%'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__!contains") > -1) {
                        conditions[i] = conditions[i].replace("__!contains = ", " NOT LIKE ");
                        // Replaces 'value' by '%value%'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__icontains") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__!icontains") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__endswith") > -1) {
                        conditions[i] = conditions[i].replace("__endswith = ", " LIKE ");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__!endswith") > -1) {
                        conditions[i] = conditions[i].replace("__!endswith = ", " NOT LIKE ");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__iendswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__!iendswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else {
                        
                     }
                  }
                  if (conditions[i].indexOf("__in") > -1) {
                     // Creates the IN SQL statement.
                     conditions[i] = conditions[i].replace("__in = ", " IN ");
                     // Replaces the [] characters by ().
                     conditions[i] = conditions[i].replace("[", "(");
                     conditions[i] = conditions[i].replace("]", ")");
                  }
                  if (conditions[i].indexOf("__!in") > -1) {
                     // Creates a IN statement in SQL.
                     conditions[i] = conditions[i].replace("__!in = ", " NOT IN ");
                     // Replaces [] by ()
                     conditions[i] = conditions[i].replace("[", "(");
                     conditions[i] = conditions[i].replace("]", ")");
                  }
                  if (conditions[i].indexOf("__range") > -1) {
                     // Creates the BETWEEN SQL statement.
                     conditions[i] = conditions[i].replace("__range = ", " BETWEEN ");
                     // Removes the [ or ] characters.
                     conditions[i] = conditions[i].replace("[", "");
                     conditions[i] = conditions[i].replace("]", "");
                     // Replaces the comma character by AND.
                     conditions[i] = conditions[i].replace(", ", " AND ");
                  }
                  if (conditions[i].indexOf("__!range") > -1) {
                     conditions[i] = conditions[i].replace("__!range = ", " NOT BETWEEN ");
                     conditions[i] = conditions[i].replace("[", "");
                     conditions[i] = conditions[i].replace("]", "");
                     conditions[i] = conditions[i].replace(", ", " AND ");
                  }
                  if (conditions[i].indexOf("__lt") > -1) {
                     conditions[i] = conditions[i].replace("__lt = ", " < ");
                  }
                  if (conditions[i].indexOf("__!lt") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
                  }
                  if (conditions[i].indexOf("__lte") > -1) {
                     conditions[i] = conditions[i].replace("__lte = ", " <= ");
                  }
                  if (conditions[i].indexOf("__!lte") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
                  }
                  if (conditions[i].indexOf("__gt") > -1) {
                     conditions[i] = conditions[i].replace("__gt = ", " > ");
                  }
                  if (conditions[i].indexOf("__!gt") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
                  }
                  if (conditions[i].indexOf("__gte") > -1) {
                     conditions[i] = conditions[i].replace("__gte = ", " >= ");
                  }
                  if (conditions[i].indexOf("__!gte") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
                  }
                  if (conditions[i].indexOf("__exact") > -1) {
                     conditions[i] = conditions[i].replace("__exact = ", " = ");
                  }
                  if (conditions[i].indexOf("__!exact") > -1) {
                     conditions[i] = conditions[i].replace("__!exact = ", " != ");
                  }
                  if (conditions[i].indexOf("__isnull") > -1) {
                     String bool = conditions[i].substring(conditions[i].indexOf("=") + 1, conditions[i].length()).trim();
                     if (bool.equalsIgnoreCase("true")) {
                        conditions[i] = conditions[i].replace("__isnull = ", " IS NULL ");
                     }
                     if (bool.equalsIgnoreCase("false")) {
                        conditions[i] = conditions[i].replace("__isnull = ", " IS NOT NULL ");
                     }
                     conditions[i] = conditions[i].replace(bool, "");
                  }
                  where += " " + conditions[i] + " AND";
                  where = where.replace(" AND OR AND", " OR");
                  where = where.replace(" AND AND AND", " AND");
               }
            }
            if (where.indexOf(" AND") > -1) {
               where = where.substring(0, where.lastIndexOf("AND"));
               where = where.trim();
               sql = String.format("%s %s", sql, where);
            }
         }
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         while (resultSet.next()) {
            rows = resultSet.getInt("rows");
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(resultSet, statement, connection);
      }
      return rows;
   }
   
   /**
    * Conta todos os registros da entidade
    * persistidos no banco de dados.
    */
   public int _count() {
      return _count("");
   }
   
   /**
    * Busca por todos os registros da entidade persistidos
    * no banco de dados que satisfaçam as condições passadas como parâmetro.
    * Funciona de forma similar a filter porém com lógica invertida.
    */
   public <T extends Model> QuerySet<T> _exclude(String... fields) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      if (fields != null) {
         try {
            String sql = String.format("SELECT * FROM %s WHERE", tableName);
            String where = "";
            // Iterates through the pairs field=value.
            for (int i = 0; i < fields.length; i++) {
               fields[i] = fields[i] == null ? "" : fields[i].trim();
               if (fields[i].isEmpty()) {
                  continue;
               }
               if (fields[i].equalsIgnoreCase("AND")) {
                  fields[i] = "AND";
               }
               if (fields[i].equalsIgnoreCase("OR")) {
                  fields[i] = "OR";
               }
               // Creates the column name.
               if (fields[i].contains("=")) {
                  fields[i] = String.format(
                        "%s%s",
                        TableUtil.getColumnName(fields[i].substring(0, fields[i].lastIndexOf("="))),
                        fields[i].substring(fields[i].lastIndexOf("=")));
               }
               // Adds a blank space between the field name and value.
               fields[i] = fields[i].replace("=", " = ");
               // Replaces % by \%
               fields[i] = fields[i].replace("%", "\\%");
               // Adds a blank space between the values separated by comma
               // character.
               fields[i] = fields[i].replace(",", ", ");
               // Checks if the current pair contains __startswith, __contains
               // ou __endswith.
               if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__!startswith") > -1 ||
                     fields[i].indexOf("__istartswith") > -1 || fields[i].indexOf("__!istartswith") > -1 ||
                     fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__!contains") > -1 || fields[i].indexOf("__icontains") > -1 ||
                     fields[i].indexOf("__!icontains") > -1 || fields[i].indexOf("__endswith") > -1 || fields[i].indexOf("__!endswith") > -1 ||
                     fields[i].indexOf("__iendswith") > -1 || fields[i].indexOf("__!iendswith") > -1) {
                  // Creates a LIKE SQL statement.
                  if (fields[i].indexOf("__startswith") > -1) {
                     fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!startswith") > -1) {
                     fields[i] = fields[i].replace("__!startswith = ", " NOT LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__contains") > -1) {
                     fields[i] = fields[i].replace("__contains = ", " LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!contains") > -1) {
                     fields[i] = fields[i].replace("__!contains = ", " NOT LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__endswith") > -1) {
                     fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!endswith") > -1) {
                     fields[i] = fields[i].replace("__!endswith = ", " NOT LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else {
                     
                  }
               }
               if (fields[i].indexOf("__in") > -1) {
                  // Creates a IN SQL statement.
                  fields[i] = fields[i].replace("__in = ", " IN ");
                  // Replaces [] characters by () characters.
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__!in") > -1) {
                  // Creates a IN SQL statement.
                  fields[i] = fields[i].replace("__!in = ", " NOT IN ");
                  // Replaces [] characters by () characters.
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__range") > -1) {
                  // Creates a BETWEEN SQL statement.
                  fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                  // Removes the [ character.
                  fields[i] = fields[i].replace("[", "");
                  // Removes the ] character.
                  fields[i] = fields[i].replace("]", "");
                  // Substituindo o caracter , por AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__!range") > -1) {
                  // Creates a BETWEEN SQL statement.
                  fields[i] = fields[i].replace("__!range = ", " NOT BETWEEN ");
                  // Removes the [ character.
                  fields[i] = fields[i].replace("[", "");
                  // Removes the ] character.
                  fields[i] = fields[i].replace("]", "");
                  // Substituindo o caracter , por AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__lt") > -1) {
                  fields[i] = fields[i].replace("__lt = ", " < ");
               }
               if (fields[i].indexOf("__!lt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
               }
               if (fields[i].indexOf("__lte") > -1) {
                  fields[i] = fields[i].replace("__lte = ", " <= ");
               }
               if (fields[i].indexOf("__!lte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
               }
               if (fields[i].indexOf("__gt") > -1) {
                  fields[i] = fields[i].replace("__gt = ", " > ");
               }
               if (fields[i].indexOf("__!gt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
               }
               if (fields[i].indexOf("__gte") > -1) {
                  fields[i] = fields[i].replace("__gte = ", " >= ");
               }
               if (fields[i].indexOf("__!gte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
               }
               if (fields[i].indexOf("__exact") > -1) {
                  fields[i] = fields[i].replace("__exact = ", " = ");
               }
               if (fields[i].indexOf("__!exact") > -1) {
                  fields[i] = fields[i].replace("__!exact = ", " != ");
               }
               if (fields[i].indexOf("__isnull") > -1) {
                  String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length()).trim();
                  if (bool.equalsIgnoreCase("true")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
                  }
                  if (bool.equalsIgnoreCase("false")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
                  }
                  fields[i] = fields[i].replace(bool, "");
               }
               where += fields[i] + " AND ";
               where = where.replace(" AND OR AND", " OR");
               where = where.replace(" AND AND AND", " AND");
            }
            where = where.substring(0, where.lastIndexOf("AND"));
            sql = String.format("%s NOT (%s)", sql, where);
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
               return querySet;
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               Object obj = entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  if (connection.toString().startsWith("oracle")) {
                     id.set(
                           obj,
                           ((java.math.BigDecimal) resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)))
                                 .intValue());
                  } else {
                     id.set(obj, resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)));
                  }
               }
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field field : _fields) {
                  field.setAccessible(true);
                  if (!JediEngine.isJediField(field)) {
                     continue;
                  }
                  if (field.getName().equals("serialVersionUID")) {
                     continue;
                  }
                  if (field.getName().equals("objects")) {
                     continue;
                  }
                  oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class superClazz = null;
                        Class clazz = null;
                        String model = manyToManyFieldAnnotation.model().getSimpleName();
                        model = Model.class.getSimpleName().equals(model) ? "" : model;
                        if (model.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 model = clazz.getSimpleName();
                              }
                           }
                        }
                        String references = manyToManyFieldAnnotation.references();
                        if (references == null || references.trim().isEmpty()) {
                           if (clazz != null) {
                              references = TableUtil.getTableName(clazz);
                           } else {
                              references = TableUtil.getTableName(model);
                           }
                        }
                        String packageName = this.entity.getPackage().getName();
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                        manager = new Manager(associatedModelClass);
                        QuerySet querySetAssociatedModels = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    references,
                                    model,
                                    tableName,
                                    references,
                                    TableUtil.getColumnName(obj.getClass()),
                                    ((Model) obj).getId()),
                              associatedModelClass);
                        field.set(obj, querySetAssociatedModels);
                     } else {
                        field.set(obj, null);
                     }
                  } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                     if (oneToOneFieldAnnotation != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     }
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(field.getType().getName());
                        manager = new Manager(associatedModelClass);
                        Model associatedModel = manager
                              .get(String.format("id"), resultSet.getObject(String.format("%s_id", TableUtil.getColumnName(field))));
                        field.set(obj, associatedModel);
                     } else {
                        field.set(obj, null);
                     }
                  } else {
                     if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        if (resultSet.getObject(TableUtil.getColumnName(field)) == null) {
                           field.set(obj, 0);
                        } else {
                           String columnName = TableUtil.getColumnName(field);
                           BigDecimal bigDecimal = (BigDecimal) resultSet.getObject(columnName);
                           field.set(obj, bigDecimal.intValue());
                        }
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        Object columnValue = resultSet.getObject(columnName);
                        if (columnValue instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           columnValue = calendar.getTime();
                        }
                        field.set(obj, columnValue);
                     }
                  }
               }
               T model = (T) obj;
               if (model != null) {
                  model.setPersisted(true);
               }
               querySet.add(model);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(resultSet, statement, connection);
         }
      }
      return querySet;
   }
   
   /**
    * Executa uma instrução SQL pura independente da entidade.
    */
   public List<List<Map<String, Object>>> raw(String sql) {
      // tabela: lista de linhas ou registros.
      // linha: lista de colunas.
      // colunas: mapa contendo chave e valor.
      List<List<Map<String, Object>>> recordSet = new ArrayList<>();
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      sql = sql == null ? "" : sql.trim();
      if (!sql.isEmpty()) {
         try {
            if (JediEngine.DEBUG) {
               if (!sql.equals("SELECT VERSION()")) {
                  if (sql.endsWith(";")) {
                     System.out.println(sql + "\n");
                  } else {
                     System.out.println(sql + ";\n");
                  }
               }
            }
            // DQL - Data Query Language (SELECT).
            if (sql.startsWith("select") || sql.startsWith("SELECT")) {
               String _sql = sql.toLowerCase();
               // Returns a navigable ResultSet.
               connect();
               statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
               resultSet = statement.executeQuery();
               ResultSetMetaData tableMetadata = null;
               if (resultSet != null) {
                  tableMetadata = resultSet.getMetaData();
                  if (tableMetadata != null) {
                     // Deslocando o cursor até o último registro.
                     recordSet = new ArrayList<List<Map<String, Object>>>();
                     String key = null;
                     while (resultSet.next()) {
                        List<Map<String, Object>> tableRow = new ArrayList<Map<String, Object>>();
                        HashMap<String, Object> tableColumn = new HashMap<String, Object>();
                        for (int i = 1; i <= tableMetadata.getColumnCount(); i++) {
                           if (_sql.contains("inner join") || _sql.contains("left join") || _sql.contains("right join") ||
                                 _sql.contains("full outer join")) {
                              key = String.format("%s.%s", tableMetadata.getTableName(i), tableMetadata.getColumnName(i));
                              tableColumn.put(key, resultSet.getObject(key));
                           } else {
                              tableColumn.put(tableMetadata.getColumnLabel(i), resultSet.getObject(tableMetadata.getColumnLabel(i)));
                           }
                        }
                        tableRow.add(tableColumn);
                        recordSet.add(tableRow);
                     }
                  }
               }
            } else {
               // DML - Data Manipulation Language (INSERT, UPDATE or DELETE).
               connect();
               statement = connection.prepareStatement(sql);
               statement.executeUpdate();
               commit();
            }
         } catch (Exception e) {
            e.printStackTrace();
            rollback();
         } finally {
            close(resultSet, statement, connection);
         }
      }
      return recordSet;
   }
   
   /**
    * Executa uma instrução SQL utilizando os recursos da entidade.
    */
   public <T extends Model> QuerySet<T> raw(String sql, Class<T> clazz) {
      QuerySet<T> qs = new QuerySet<T>();
      PreparedStatement stmt = null;
      ResultSet rs = null;
      OneToOneField oofa = null;
      ForeignKeyField fkfa = null;
      ManyToManyField mmfa = null;
      sql = sql == null ? "" : sql.trim();
      if (!sql.isEmpty()) {
         try {
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            qs = new QuerySet();
            qs.setEntity(clazz);
            while (rs.next()) {
               T o = clazz.newInstance();
               if (rs.getObject("id") != null) {
                  Field id = clazz.getSuperclass().getDeclaredField("id");
                  id.setAccessible(true);
                  if (connection.toString().startsWith("oracle")) {
                     id.set(o, ((java.math.BigDecimal) rs.getObject(id.getName())).intValue());
                  } else {
                     id.set(o, rs.getObject(id.getName()));
                  }
               }
               if (o != null) {
                  o.setPersisted(true);
               }
               for (Field field : clazz.getDeclaredFields()) {
                  field.setAccessible(true);
                  if (!JediEngine.isJediField(field)) {
                     continue;
                  }
                  if (field.getName().equals("serialVersionUID")) {
                     continue;
                  }
                  if (field.getName().equalsIgnoreCase("objects")) {
                     continue;
                  }
                  oofa = field.getAnnotation(OneToOneField.class);
                  fkfa = field.getAnnotation(ForeignKeyField.class);
                  mmfa = field.getAnnotation(ManyToManyField.class);
                  FetchType ft = JediEngine.FETCH_TYPE;
                  String tableName = TableUtil.getTableName(clazz);
                  String model = null;
                  String references = null;
                  Manager manager = null;
                  if (oofa != null || fkfa != null) {
                     if (oofa != null) {
                        ft = ft.equals(FetchType.NONE) ? oofa.fetch_type() : ft;
                     } else {
                        ft = ft.equals(FetchType.NONE) ? fkfa.fetch_type() : ft;
                     }
                     if (ft.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(field.getType().getName());
                        manager = new Manager(associatedModelClass);
                        Model associatedModel = manager
                              .get(String.format("id"), rs.getObject(String.format("%s_id", TableUtil.getColumnName(field))));
                        field.set(o, associatedModel);
                     } else {
                        field.set(o, null);
                     }
                  } else if (mmfa != null) {
                     ft = ft.equals(FetchType.NONE) ? mmfa.fetch_type() : ft;
                     if (ft.equals(FetchType.EAGER)) {
                        Class sc = null; // super class
                        Class c = null; // class
                        model = mmfa.model().getSimpleName();
                        model = Model.class.getSimpleName().equals(model) ? "" : model;
                        if (model.isEmpty()) {
                           ParameterizedType pt = null; // generic type
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              pt = (ParameterizedType) field.getGenericType();
                              sc = ((Class) (pt.getActualTypeArguments()[0])).getSuperclass();
                              if (sc == Model.class) {
                                 c = (Class) pt.getActualTypeArguments()[0];
                                 model = c.getSimpleName();
                              }
                           }
                        }
                        references = mmfa.references();
                        if (references == null || references.trim().isEmpty()) {
                           if (c != null) {
                              references = TableUtil.getTableName(c);
                           } else {
                              references = TableUtil.getTableName(model);
                           }
                        }
                        // Associated model class.
                        Class amc = Class.forName(String.format("%s.%s", this.entity.getPackage().getName(), model));
                        manager = new Manager(amc);
                        // Associated model record set.
                        List<List<Map<String, Object>>> amrs = null;
                        amrs = manager.raw(
                              String.format(
                                    "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                                    TableUtil.getColumnName(model),
                                    tableName,
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(clazz),
                                    o.id()));
                        if (amrs != null) {
                           String args = amrs.toString().toLowerCase();
                           args = args.replace("[", "");
                           args = args.replace("{", "");
                           args = args.replace("]", "");
                           args = args.replace("}", "");
                           args = args.replace("=", "");
                           args = args.replace(", ", ",");
                           args = args.replace(String.format("%s_id", TableUtil.getColumnName(model)), "");
                           args = String.format("id__in=[%s]", args);
                           QuerySet qs1 = manager._filter(args);
                           field.set(o, qs1);
                        } else {
                           field.set(o, new QuerySet<T>());
                        }
                     } else {
                        field.set(o, new QuerySet<T>());
                     }
                  } else {
                     // Configura campos que não são instancias de model.
                     if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        if (rs.getObject(TableUtil.getColumnName(field)) == null) {
                           field.set(o, 0);
                        } else {
                           String columnName = TableUtil.getColumnName(field);
                           BigDecimal bigDecimal = (BigDecimal) rs.getObject(columnName);
                           field.set(o, bigDecimal.intValue());
                        }
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        Object columnValue = rs.getObject(columnName);
                        if (columnValue instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           columnValue = calendar.getTime();
                        }
                        field.set(o, columnValue);
                     }
                  }
                  manager = null;
               }
               T model = (T) o;
               if (model != null) {
                  model.setPersisted(true);
               }
               qs.add(model);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(rs, stmt, connection);
         }
      }
      return qs;
   }
   
   /**
    * Recupera um registro da entidade que satisfaça
    * a condição passada.
    * 
    * @param field
    * @param value
    * @param modelClass
    * @return
    * @throws ObjectDoesNotExistException
    * @throws MultipleObjectsReturnedException
    */
   private <T extends Model> T get(String field, Object value, Class<T> modelClass)
         throws ObjectDoesNotExistException, MultipleObjectsReturnedException {
      T model = null;
      String columnName = "";
      Object o = null;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      field = field == null ? "" : field.trim();
      if (!field.isEmpty()) {
         try {
            field = TableUtil.getColumnName(field);
            String sql = "SELECT * FROM";
            if (value != null) {
               sql = String.format("%s %s WHERE %s = '%s'", sql, tableName, field, value.toString());
            } else {
               if (field.equals("id")) {
                  return null;
               }
               sql = String.format("%s %s WHERE %s IS NULL", sql, tableName, field);
            }
            /*
             * Se o tipo de dado do valor passado é numérico
             * o apóstrofe é retirado.
             */
            if (Integer.class.isInstance(value) || Float.class.isInstance(value) || Double.class.isInstance(value)) {
               sql = sql.replaceAll("\'", "");
            }
            connect();
            statement = connection.prepareStatement(sql);
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            resultSet = statement.executeQuery();
            int rowCount = resultSet.last() ? resultSet.getRow() : 0;
            if (rowCount <= 0) {
               throw new DoesNotExistException(String.format("%s matching query does not exist.", this.entity.getSimpleName()));
            } else if (rowCount > 1) {
               throw new MultipleObjectsReturnedException(
                     String.format(
                           "get() returned more than one %s --\n    it returned %d! %s {'%s': '%s'}",
                           this.entity.getSimpleName(),
                           rowCount,
                           "Lookup parameters were",
                           field,
                           value));
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               model = (T) entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  o = resultSet.getObject(id.getName());
                  /*
                   * Trata o tipo de dado BigDecimal retornado pelo Oracle.
                   * No MySQL e no PostgreSQL o tipo do dado é Integer.
                   */
                  if (connection.toString().startsWith("oracle")) {
                     id.set(model, ((java.math.BigDecimal) o).intValue());
                  } else {
                     id.set(model, o);
                  }
               }
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field f : _fields) {
                  f.setAccessible(true);
                  if (!JediEngine.isJediField(f)) {
                     continue;
                  }
                  if (f.getName().equals("serialVersionUID"))
                     continue;
                  if (f.getName().equalsIgnoreCase("objects"))
                     continue;
                  oneToOneFieldAnnotation = f.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = f.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = f.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  String referencedModel = null;
                  String referencedTable = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        if (!manyToManyFieldAnnotation.through().getSimpleName().equals(Model.class.getSimpleName())) {
                           continue;
                        }
                        Class superClazz = null;
                        Class clazz = null;
                        referencedModel = manyToManyFieldAnnotation.model().getSimpleName();
                        referencedModel = Model.class.getSimpleName().equals(referencedModel) ? "" : referencedModel;
                        if (referencedModel.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(f.getGenericType().getClass())) {
                              genericType = (ParameterizedType) f.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 referencedModel = clazz.getSimpleName();
                              }
                           }
                        }
                        referencedTable = manyToManyFieldAnnotation.references();
                        if (referencedTable == null || referencedTable.trim().isEmpty()) {
                           if (clazz != null) {
                              referencedTable = TableUtil.getTableName(clazz);
                           } else {
                              referencedTable = TableUtil.getTableName(referencedModel);
                           }
                        }
                        String packageName = this.entity.getPackage().getName();
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, referencedModel));
                        manager = new Manager(associatedModelClass);
                        QuerySet associatedModelsQuerySet = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    referencedTable,
                                    referencedModel.toLowerCase(),
                                    tableName,
                                    referencedTable,
                                    TableUtil.getColumnName(model.getClass()),
                                    model.getId()),
                              associatedModelClass);
                        // Configurando o campo (atributo) com a referência
                        // para o queryset criado anteriormente.
                        f.set(model, associatedModelsQuerySet);
                     } else {
                        f.set(model, null);
                     }
                  } else if (foreignKeyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        // Caso seja recupera a classe do atributo.
                        Class associatedModelClass = Class.forName(f.getType().getName());
                        // Instanciando um model manager.
                        manager = new Manager(associatedModelClass);
                        // Chamando o método esse método (get)
                        // recursivamente.
                        Model associatedModel = manager.get("id", resultSet.getObject(String.format("%s_id", TableUtil.getColumnName(f))));
                        // Atributo (campo) referenciando o modelo anotado
                        // como ForeignKeyField.
                        f.set(model, associatedModel);
                     } else {
                        f.set(model, null);
                     }
                  } else if (oneToOneFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(f.getType().getName());
                        manager = new Manager(associatedModelClass);
                        columnName = TableUtil.getColumnName(f.getType().getSimpleName());
                        o = resultSet.getObject(String.format("%s_id", columnName));
                        Model associatedModel = manager.get("id", o);
                        f.set(model, associatedModel);
                     } else {
                        f.set(model, null);
                     }
                  } else {
                     // Configurando campos que não são instancias de
                     // Model.
                     if ((f.getType().getSimpleName().equals("int") || f.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        columnName = TableUtil.getColumnName(f.getName());
                        o = resultSet.getObject(columnName);
                        if (o == null) {
                           f.set(model, 0);
                        } else {
                           columnName = TableUtil.getColumnName(f.getName());
                           o = resultSet.getObject(columnName);
                           f.set(model, ((BigDecimal) o).intValue());
                        }
                     } else {
                        columnName = TableUtil.getColumnName(f.getName());
                        // Column´s value.
                        o = resultSet.getObject(columnName);
                        if (o instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           o = calendar.getTime();
                        }
                        if (o instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           o = calendar.getTime();
                        }
                        if (o instanceof Timestamp) {
                           // TODO - Refatoração mudança de jedi.types.DateTime
                           // para java.util.Date
                           // o = new DateTime(((Timestamp) o).getTime());
                           // o = new Date(((Timestamp) o).getTime());
                           Timestamp timestamp = (Timestamp) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           o = calendar.getTime();
                        }
                        f.set(model, o);
                     }
                  }
                  manager = null;
               }
            }
            if (model != null) {
               model.setPersisted(true);
            }
         } catch (SQLException e) {
            e.printStackTrace();
         } catch (InstantiationException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
         } catch (SecurityException e) {
            e.printStackTrace();
         } catch (ClassNotFoundException e) {
            e.printStackTrace();
         } finally {
            close(resultSet, statement, connection);
         }
      }
      return model;
   }
   
   public <T extends Model> T get(String field, Object value) throws ObjectDoesNotExistException, MultipleObjectsReturnedException {
      return (T) this.get(field, value, this.entity);
   }
   
   /**
    * Recupera um registro da entidade que satisfaça
    * as condições passadas como parâmetro.
    * 
    * @param clazz
    * @param fieldLookups
    * @return
    * @throws ObjectDoesNotExistException
    * @throws MultipleObjectsReturnedException
    */
   private <T extends Model> T get(Class<T> clazz, String... fieldLookups)
         throws ObjectDoesNotExistException, MultipleObjectsReturnedException {
      T model = null;
      if (fieldLookups != null) {
         StringBuilder _sql = new StringBuilder();
         _sql.append("select\n");
         _sql.append("    *\n");
         _sql.append("from\n");
         _sql.append(String.format("    %s\n", this.getTableName()));
         _sql.append("where\n");
         String _fieldLookups = "";
         for (String fieldLookup : fieldLookups) {
            _fieldLookups += String.format("%s, ", fieldLookup);
            if (fieldLookup.matches("and|or")) {
               _sql.append(String.format(" %s\n", fieldLookup));
            } else {
               _sql.append(String.format("    %s", FieldLookup.translate(fieldLookup).get("where").get(0)));
            }
         }
         String sql = _sql.toString();
         sql = sql.substring(0, sql.length());
         if (JediEngine.DEBUG) {
            System.out.println(sql);
         }
         _fieldLookups = _fieldLookups.substring(0, _fieldLookups.length() - 2);
         try {
            connect();
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            List<T> list = JediEngine.convert(rs, clazz);
            if (list.size() == 1) {
               model = list.get(0);
            } else if (list.size() > 1) {
               throw new MultipleObjectsReturnedException(
                     String.format(
                           "get() returned more than one %s --\n    it returned %d! %s {%s}",
                           this.entity.getSimpleName(),
                           list.size(),
                           "Lookup parameters were",
                           _fieldLookups));
            } else {
               throw new ObjectDoesNotExistException(String.format("%s matching query does not exist.", this.entity.getSimpleName()));
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return model;
   }
   
   public <T extends Model> T get(String... fieldLookups) throws ObjectDoesNotExistException, MultipleObjectsReturnedException {
      return (T) get(this.entity, fieldLookups);
   }
   
   /**
    * Recupera o último registro persistido da entidade.
    * 
    * @param field
    * @param modelClass
    * @return
    */
   private <T extends Model> T latest(String field, Class<T> modelClass) {
      T model = null;
      field = field == null ? "" : field.trim();
      if (!field.isEmpty()) {
         try {
            // Renomeando o atributo para ficar no mesmo padrão do nome da
            // coluna na tabela associada ao modelo.
            field = TableUtil.getColumnName(field);
            String sql = String.format("SELECT * FROM %s ORDER BY %s DESC LIMIT 1", tableName, field);
            if (connection.toString().startsWith("oracle")) {
               sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s DESC", tableName, field);
            }
            connect();
            QuerySet querySet = this.raw(sql, entity);
            if (querySet != null && !querySet.isEmpty()) {
               model = (T) querySet.get(0);
               if (model != null) {
                  model.setPersisted(true);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(connection);
         }
      }
      return model;
   }
   
   public <T extends Model> T latest(String field) {
      return (T) latest(field, entity);
   }
   
   public <T extends Model> T latest() {
      return (T) latest("id", entity);
   }
   
   /**
    * Recupera o primeiro registro persistido da entidade.
    * 
    * @param field
    * @param modelClass
    * @return
    */
   private <T extends Model> T earliest(String field, Class<T> modelClass) {
      T model = null;
      field = field == null ? "" : field.trim();
      if (!field.isEmpty()) {
         try {
            String sql = String.format("SELECT * FROM %s ORDER BY %s ASC LIMIT 1", tableName, field);
            if (this.connection.toString().startsWith("oracle")) {
               sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s ASC", tableName, field);
            }
            connect();
            QuerySet querySet = this.raw(sql, entity);
            if (querySet != null && !querySet.isEmpty()) {
               model = (T) querySet.get(0);
               if (model != null) {
                  model.setPersisted(true);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(connection);
         }
      }
      return (T) model;
   }
   
   public <T extends Model> T earliest(String field) {
      return (T) earliest(field, entity);
   }
   
   public <T extends Model> T earliest() {
      return (T) earliest("id", entity);
   }
   
   /**
    * Recupera pelo id os registros das entidades associadas.
    * 
    * @param associatedModelClass
    * @param id
    * @return
    *         Example: SELECT livros.* FROM livros, livros_autores WHERE
    *         livros.id =
    *         livros_autores.livro_id AND livros_autores.autor_id = 1;
    */
   public <S extends Model, T extends Model> QuerySet<S> getSet(Class<T> associatedModelClass, int id) {
      QuerySet<S> querySet = new QuerySet<S>();
      if (associatedModelClass != null && associatedModelClass.getSuperclass().getName().equals("jedi.db.models.Model")) {
         String sql = "";
         String tableNameAssociatedModel = TableUtil.getTableName(associatedModelClass);
         ForeignKeyField foreignKeyFieldAnnotation = null;
         List<Field> _fields = JediEngine.getAllFields(this.entity);
         for (Field field : _fields) {
            foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
            if (foreignKeyFieldAnnotation != null) {
               String model = foreignKeyFieldAnnotation.model().getSimpleName();
               model = Model.class.getSimpleName().equals(model) ? "" : model;
               if (model.isEmpty()) {
                  model = field.getType().getName().replace(field.getType().getPackage().getName() + ".", "");
               }
               if (model.equals(associatedModelClass.getSimpleName())) {
                  querySet = this._filter(String.format("%s_id=%d", field.getName(), id));
               }
            }
         }
         if (querySet == null) {
            sql = String.format(
                  "SELECT %s.* FROM %s, %s_%s WHERE %s.id = %s_%s.%s_id AND %s_%s.%s_id = %d",
                  tableName,
                  tableName,
                  tableName,
                  tableNameAssociatedModel,
                  tableName,
                  tableName,
                  tableNameAssociatedModel,
                  TableUtil.getColumnName(this.entity),
                  tableName,
                  tableNameAssociatedModel,
                  TableUtil.getColumnName(associatedModelClass),
                  id);
            querySet = (QuerySet<S>) this.raw(sql, this.entity);
         }
      }
      return (QuerySet<S>) querySet;
   }
   
   /**
    * Persiste no banco de dados a lista de entidades
    * passada como parâmetro.
    * 
    * @param models
    * @return A referência a esse manager.
    */
   public IManager bulkCreate(Model... models) {
      if (models != null) {
         for (Model model : models) {
            if (model != null) {
               model.save();
            }
         }
      }
      return this;
   }
   
   @Override
   public IManager save(Model... models) {
      if (models != null) {
         for (Model model : models) {
            if (model != null) {
               model.save();
            }
         }
      }
      return this;
   }
   
   @Override
   public IManager save(List<Model> models) {
      if (models != null) {
         for (Model model : models) {
            if (model != null) {
               model.save();
            }
         }
      }
      return this;
   }
   
   public <T extends Model> T getOrCreate(String... args) {
      T obj = null;
      try {
         obj = get(args);
      } catch (DoesNotExistException e) {
         obj = create(args);
      }
      return obj;
   }
   
   public <T extends Model> T updateOrCreate(String... args) {
      T obj = null;
      // TODO
      return obj;
   }
   
   /**
    * Recupera os registro persistidos da entidade
    * que satisfaçam as condições passadas como parâmetro.
    */
   public <T extends Model> QuerySet<T> where(String criteria, Object... values) {
      QuerySet<T> qs = new QuerySet<T>();
      criteria = criteria == null ? "" : criteria;
      if (!criteria.isEmpty()) {
         StringBuilder sql = new StringBuilder();
         sql.append(String.format("SELECT * FROM %s WHERE ", tableName));
         if (values != null && values.length > 0) {
            MessageFormat message = new MessageFormat(criteria);
            for (int i = 0; i < values.length; i++) {
               if (values[i].getClass() == String.class || values[i].getClass() == Date.class || values[i].getClass() == DateTime.class) {
                  values[i] = String.format("'%s'", values[i]);
               } else {
                  values[i] = String.format("%s", values[i]);
               }
            }
            sql.append(message.format(values));
         } else {
            sql.append(criteria);
         }
         qs = (QuerySet<T>) this.raw(sql.toString(), this.entity);
      }
      return qs;
   }
   
   public <T1 extends Model, T2 extends Model> Manager join(Class<T1> clazz1, Class<T2> clazz2, JoinType type) {
      if (clazz1 != null && clazz2 != null && type != null) {
         String tb1 = TableUtil.getTableName(clazz1);
         String tb2 = TableUtil.getTableName(clazz2);
         String cols1 = String.format("\t%s.*,\n", tb1);
         String cols2 = String.format("\t%s.*\n", tb2);
         String from = String.format("from\n\t%s\n", tb1);
         String join = "";
         String on = String.format("on\n\t%s.id = %s.%s_id;", tb1, tb2, clazz1.getSimpleName().toLowerCase());
         StringBuilder sql = new StringBuilder();
         sql.append("select\n");
         sql.append(cols1);
         sql.append(cols2);
         sql.append(from);
         switch (type.getValue()) {
            case "LEFT":
               join = String.format("left join\n\t%s\n", tb2);
               break;
            case "INNER":
               join = String.format("inner join\n\t%s\n", tb2);
               break;
            case "RIGHT":
               join = String.format("right join\n\t%s\n", tb2);
               break;
         }
         sql.append(join);
         sql.append(on);
         this.sql.append(sql.toString());
      }
      return this;
   }
   
   public <T1 extends Model, T2 extends Model> Manager join(Class<T1> clazz1, Class<T2> clazz2) {
      return this.join(clazz1, clazz2, JoinType.INNER);
   }
   
   public <T extends Model> Manager join(Class<T> clazz, JoinType type) {
      return this.join(this.entity, clazz, type);
   }
   
   public <T extends Model> Manager join(Class<T> clazz) {
      return this.join(this.entity, clazz, JoinType.INNER);
   }
   
   public List<List<Map<String, Object>>> fetch() {
      this.resultSet = this.raw(this.sql.toString());
      if (JediEngine.DEBUG) {
         System.out.println(this.sql);
      }
      this.sql.setLength(0);
      return this.resultSet;
   }
   
   public <T extends Model> Manager innerJoin(Class<T> clazz) {
      if (clazz != null) {
         String tb1 = TableUtil.getTableName(this.entity);
         String tb2 = TableUtil.getTableName(clazz);
         String join = "";
         String on = String.format("on\n\t%s.id = %s.%s_id", tb1, tb2, this.entity.getSimpleName().toLowerCase());
         StringBuilder sql = new StringBuilder();
         join = String.format("\ninner join\n\t%s\n", tb2);
         sql.append(join);
         sql.append(on);
         this.sql.append(sql.toString());
      }
      return this;
   }
   
   public <T extends Model> Manager leftJoin(Class<T> clazz) {
      if (clazz != null) {
         String tb1 = TableUtil.getTableName(this.entity);
         String tb2 = TableUtil.getTableName(clazz);
         String join = "";
         String on = String.format("on\n\t%s.id = %s.%s_id", tb1, tb2, this.entity.getSimpleName().toLowerCase());
         StringBuilder sql = new StringBuilder();
         join = String.format("\nleft join\n\t%s\n", tb2);
         sql.append(join);
         sql.append(on);
         this.sql.append(sql.toString());
      }
      return this;
   }
   
   public <T extends Model> Manager rightJoin(Class<T> clazz) {
      if (clazz != null) {
         String tb1 = TableUtil.getTableName(this.entity);
         String tb2 = TableUtil.getTableName(clazz);
         String join = "";
         String on = String.format("on\n\t%s.id = %s.%s_id", tb1, tb2, this.entity.getSimpleName().toLowerCase());
         StringBuilder sql = new StringBuilder();
         join = String.format("\nright join\n\t%s\n", tb2);
         sql.append(join);
         sql.append(on);
         this.sql.append(sql.toString());
      }
      return this;
   }
   
   public Manager select(String... fields) {
      String sql = "";
      if (fields != null) {
         for (String field : fields) {
            if (field.contains(".")) {
               sql += String.format("%s, ", field);
            } else {
               sql += String.format("%s.%s, ", this.getTableName(), field);
            }
         }
         sql = sql.substring(0, sql.length() - 2);
      } else {
         sql = "*";
      }
      sql = String.format("select\n\t%s\n", sql);
      sql = String.format("%s\nfrom\n\t%s", sql, this.getTableName());
      this.sql.append(sql);
      return this;
   }
   
   /**
    * Executa uma instrução SQL (DDL, DML ou DQL).
    * select irá retornar List<Object>
    * insert, update, delete, create etc irá retornar new Boolean("true") em
    * caso de sucesso
    * ou new Boolean("false") em caso de falha.
    * 
    * @param sql
    * @return
    */
   public Object execute(String sql) {
      Object result = false;
      sql = sql == null ? "" : sql.trim();
      if (!sql.isEmpty()) {
         if (connect() != null) {
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            PreparedStatement stmt = null;
            try {
               if (sql.toLowerCase().startsWith("select")) {
                  // Retorna um resultset navegável.
                  stmt = this.connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                  ResultSet rs = stmt.executeQuery();
                  ResultSetMetaData metadata = rs == null ? null : rs.getMetaData();
                  String _sql = sql.toLowerCase();
                  if (metadata != null) {
                     result = new ArrayList<List<HashMap<String, Object>>>();
                     String tb = null; // table.
                     String col = null; // column label.
                     Object val = null; // value.
                     while (rs.next()) {
                        List<HashMap<String, Object>> row = new ArrayList<>();
                        HashMap<String, Object> column = new HashMap<>();
                        for (int i = 1; i <= metadata.getColumnCount(); i++) {
                           col = metadata.getColumnLabel(i);
                           val = rs.getObject(col);
                           if (_sql.contains("inner join") || _sql.contains("left outer join") || _sql.contains("right outer join") ||
                                 _sql.contains("full outer join")) {
                              tb = metadata.getTableName(i);
                              column.put(String.format("%s_%s", tb, col), val);
                           } else {
                              column.put(col, val);
                           }
                        }
                        row.add(column);
                        ((List) result).add(row);
                     }
                     rs.close();
                  }
               } else {
                  stmt = this.connection.prepareStatement(sql);
                  stmt.executeUpdate();
                  commit();
                  result = true;
               }
               close(stmt, connection);
            } catch (SQLException e) {
               e.printStackTrace();
               rollback();
            }
         }
      }
      return result;
   }
   
   public void execute(File script) {
      if (script != null && script.exists()) {
         try {
            FileReader fr = new FileReader(script);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            if (disconnected()) {
               connect();
            }
            Statement stmt = connection.createStatement();
            while ((line = br.readLine()) != null) {
               line = line.trim();
               if (line.startsWith("--")) {
                  continue;
               }
               stmt.addBatch(line);
            }
            stmt.executeBatch();
            br.close();
            stmt.close();
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
   
   // SQL diferença entre distinct e group by:
   // distinct elimina duplicatas em uma coluna.
   // group by elimina duplicatas em várias colunas.
   private <T extends Model> QuerySet<T> distinct(Class<T> clazz, String... fields) {
      QuerySet<T> qs = new QuerySet<T>();
      qs.setEntity((Class<T>) this.entity);
      if (clazz == null)
         return qs;
      if (fields == null)
         return qs;
      if (fields.length == 0)
         return qs;
      if (connect() == null)
         return qs;
      try {
         StringBuilder sb = new StringBuilder();
         sb.append("select\n");
         sb.append("\t*\n");
         sb.append("from\n");
         sb.append(String.format("\t%s\n", this.getTableName()));
         sb.append("group by\n");
         String columns = "";
         if (fields.length == 1 && fields[0].equals("*")) {
            Object[] _fields = JediEngine.getFields(clazz).toArray();
            Field field = null;
            for (Object _f : _fields) {
               field = (Field) _f;
               columns += String.format("\t%s,\n", field.getName());
            }
         } else {
            for (String field : fields) {
               columns += String.format("\t%s,\n", field);
            }
         }
         columns = columns.substring(0, columns.length() - 2);
         sb.append(columns);
         String sql = sb.toString();
         System.out.println(sql);
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         PreparedStatement stmt = null;
         ResultSet rs = null;
         OneToOneField oofa = null;
         ForeignKeyField fkfa = null;
         ManyToManyField mmfa = null;
         stmt = this.connection.prepareStatement(sql);
         rs = stmt.executeQuery();
         if (!rs.next()) {
            return qs;
         }
         rs.beforeFirst();
         while (rs.next()) {
            Object o = entity.newInstance();
            Field id = jedi.db.models.Model.class.getDeclaredField("id");
            id.setAccessible(true);
            if (this.connection.toString().startsWith("oracle")) {
               id.set(o, ((java.math.BigDecimal) rs.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1))).intValue());
            } else {
               id.set(o, rs.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)));
            }
            List<Field> _fields = JediEngine.getAllFields(this.entity);
            for (Field field : _fields) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.toString().substring(field.toString().lastIndexOf('.') + 1).equals("serialVersionUID")) {
                  continue;
               }
               if (field.getName().equalsIgnoreCase("objects")) {
                  continue;
               }
               oofa = field.getAnnotation(OneToOneField.class);
               fkfa = field.getAnnotation(ForeignKeyField.class);
               mmfa = field.getAnnotation(ManyToManyField.class);
               FetchType ft = JediEngine.FETCH_TYPE;
               Manager manager = null;
               if (mmfa != null) {
                  ft = ft.equals(FetchType.NONE) ? mmfa.fetch_type() : ft;
                  if (ft.equals(FetchType.EAGER)) {
                     String packageName = this.entity.getPackage().getName();
                     String model = mmfa.model().getSimpleName();
                     model = Model.class.getSimpleName().equals(model) ? "" : model;
                     Class sc = null; // super class.
                     Class c = null; // class.
                     if (model.isEmpty()) {
                        ParameterizedType pt = null; // generic type.
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           pt = (ParameterizedType) field.getGenericType();
                           sc = ((Class) (pt.getActualTypeArguments()[0])).getSuperclass();
                           if (sc == Model.class) {
                              c = (Class) pt.getActualTypeArguments()[0];
                              model = c.getSimpleName();
                           }
                        }
                     }
                     String references = mmfa.references();
                     if (references == null || references.trim().isEmpty()) {
                        if (c != null) {
                           references = TableUtil.getTableName(c);
                        } else {
                           references = TableUtil.getTableName(model);
                        }
                     }
                     // Intermediate model class name.
                     String imcn = String.format("%s.%s", packageName, model);
                     // Associated model class.
                     Class amc = Class.forName(imcn);
                     manager = new Manager(amc);
                     // QuerySet associated model.
                     QuerySet qsam = null;
                     // Intermediate model name.
                     String imn = mmfa.through().getSimpleName();
                     imn = Model.class.getSimpleName().equals(imn) ? "" : imn;
                     if (imn != null && !imn.trim().isEmpty()) {
                        imcn = String.format("%s.%s", packageName, imn);
                        // Intermediate model class.
                        Class imc = Class.forName(imcn);
                        // Intermediate table name.
                        String itn = ((Model) imc.newInstance()).getTableName();
                        qsam = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    itn,
                                    TableUtil.getColumnName(o.getClass()),
                                    ((Model) o).getId()),
                              amc);
                     } else {
                        qsam = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    tableName,
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(o.getClass()),
                                    ((Model) o).getId()),
                              amc);
                     }
                     field.set(o, qsam);
                  } else {
                     field.set(o, null);
                  }
               } else if (oofa != null || fkfa != null) {
                  if (oofa != null) {
                     ft = ft.equals(FetchType.NONE) ? oofa.fetch_type() : ft;
                  } else {
                     ft = ft.equals(FetchType.NONE) ? fkfa.fetch_type() : ft;
                  }
                  if (ft.equals(FetchType.EAGER)) {
                     Class c = Class.forName(field.getType().getName());
                     manager = new Manager(c);
                     String s = String.format("%s_id", TableUtil.getColumnName(field));
                     Object o1 = rs.getObject(s);
                     Model m = manager.get("id", o1);
                     field.set(o, m);
                  } else {
                     field.set(o, null);
                  }
               } else {
                  if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                        this.connection.toString().startsWith("oracle")) {
                     if (rs.getObject(TableUtil.getColumnName(field)) == null) {
                        field.set(o, 0);
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        BigDecimal bigDecimal = (BigDecimal) rs.getObject(columnName);
                        field.set(o, bigDecimal.intValue());
                     }
                  } else {
                     Object columnValue = rs.getObject(TableUtil.getColumnName(field));
                     columnValue = convertZeroDateToNull(columnValue);
                     if (columnValue instanceof java.sql.Date) {
                        java.sql.Date date = (java.sql.Date) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(date.getTime());
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof java.sql.Time) {
                        java.sql.Time time = (java.sql.Time) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time.getTime());
                        calendar.set(Calendar.YEAR, 0);
                        calendar.set(Calendar.MONTH, 0);
                        calendar.set(Calendar.DAY_OF_MONTH, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof Timestamp) {
                        Timestamp timestamp = (Timestamp) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp.getTime());
                        columnValue = calendar.getTime();
                     }
                     field.set(o, columnValue);
                  }
               }
               manager = null;
            }
            T model = (T) o;
            if (model != null) {
               model.setPersisted(true);
            }
            qs.add(model);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return qs;
   }
   
   public <T extends Model> QuerySet<T> distinct(String... fields) {
      QuerySet<T> qs = (QuerySet<T>) this.distinct(this.entity, fields);
      return qs;
   }
   
   public <T extends Model> QuerySet<T> distinct() {
      QuerySet<T> qs = (QuerySet<T>) this.distinct(this.entity, "*");
      return qs;
   }
   
   /**
    * Recupera os registros de uma entidade de forma ordenada.
    * 
    * @param clazz
    *           Classe do modelo a ser pesquisado.
    * @param fields
    *           Campos a serem ordenados.
    * @return QuerySet lista de resultados.
    */
   private <T extends Model> QuerySet<T> orderBy(Class<T> clazz, String... fields) {
      QuerySet<T> qs = new QuerySet<T>();
      qs.setEntity((Class<T>) this.entity);
      if (clazz == null)
         return qs;
      if (fields == null)
         return qs;
      if (fields.length == 0)
         return qs;
      if (connect() == null)
         return qs;
      try {
         StringBuilder sb = new StringBuilder();
         sb.append("select\n");
         sb.append("\t*\n");
         sb.append("from\n");
         sb.append(String.format("\t%s\n", this.getTableName()));
         sb.append("order by\n");
         for (String field : fields) {
            if (field.startsWith("-")) {
               field = field.replace("-", "");
               sb.append(String.format("\t%s desc,\n", field));
            } else {
               sb.append(String.format("\t%s asc,\n", field));
            }
         }
         String sql = sb.toString();
         sql = sql.substring(0, sql.length() - 2);
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         PreparedStatement stmt = null;
         ResultSet rs = null;
         OneToOneField oofa = null;
         ForeignKeyField fkfa = null;
         ManyToManyField mmfa = null;
         stmt = this.connection.prepareStatement(sql);
         rs = stmt.executeQuery();
         if (!rs.next()) {
            return qs;
         }
         rs.beforeFirst();
         while (rs.next()) {
            Object o = entity.newInstance();
            Field id = jedi.db.models.Model.class.getDeclaredField("id");
            id.setAccessible(true);
            if (this.connection.toString().startsWith("oracle")) {
               id.set(o, ((java.math.BigDecimal) rs.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1))).intValue());
            } else {
               id.set(o, rs.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)));
            }
            List<Field> _fields = JediEngine.getAllFields(this.entity);
            for (Field field : _fields) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.toString().substring(field.toString().lastIndexOf('.') + 1).equals("serialVersionUID")) {
                  continue;
               }
               if (field.getName().equalsIgnoreCase("objects")) {
                  continue;
               }
               oofa = field.getAnnotation(OneToOneField.class);
               fkfa = field.getAnnotation(ForeignKeyField.class);
               mmfa = field.getAnnotation(ManyToManyField.class);
               FetchType ft = JediEngine.FETCH_TYPE;
               Manager manager = null;
               if (mmfa != null) {
                  ft = ft.equals(FetchType.NONE) ? mmfa.fetch_type() : ft;
                  if (ft.equals(FetchType.EAGER)) {
                     String packageName = this.entity.getPackage().getName();
                     String model = mmfa.model().getSimpleName();
                     model = Model.class.getSimpleName().equals(model) ? "" : model;
                     Class sc = null; // super class.
                     Class c = null; // class.
                     if (model.isEmpty()) {
                        ParameterizedType pt = null; // generic type.
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           pt = (ParameterizedType) field.getGenericType();
                           sc = ((Class) (pt.getActualTypeArguments()[0])).getSuperclass();
                           if (sc == Model.class) {
                              c = (Class) pt.getActualTypeArguments()[0];
                              model = c.getSimpleName();
                           }
                        }
                     }
                     String references = mmfa.references();
                     if (references == null || references.trim().isEmpty()) {
                        if (c != null) {
                           references = TableUtil.getTableName(c);
                        } else {
                           references = TableUtil.getTableName(model);
                        }
                     }
                     // Intermediate model class name.
                     String imcn = String.format("%s.%s", packageName, model);
                     // Associated model class.
                     Class amc = Class.forName(imcn);
                     manager = new Manager(amc);
                     // QuerySet associated model.
                     QuerySet qsam = null;
                     // Intermediate model name.
                     String imn = mmfa.through().getSimpleName();
                     imn = Model.class.getSimpleName().equals(imn) ? "" : imn;
                     if (imn != null && !imn.trim().isEmpty()) {
                        imcn = String.format("%s.%s", packageName, imn);
                        // Intermediate model class.
                        Class imc = Class.forName(imcn);
                        // Intermediate table name.
                        String itn = ((Model) imc.newInstance()).getTableName();
                        qsam = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    itn,
                                    TableUtil.getColumnName(o.getClass()),
                                    ((Model) o).getId()),
                              amc);
                     } else {
                        qsam = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(model),
                                    tableName,
                                    TableUtil.getTableName(references),
                                    TableUtil.getColumnName(o.getClass()),
                                    ((Model) o).getId()),
                              amc);
                     }
                     field.set(o, qsam);
                  } else {
                     field.set(o, null);
                  }
               } else if (oofa != null || fkfa != null) {
                  if (oofa != null) {
                     ft = ft.equals(FetchType.NONE) ? oofa.fetch_type() : ft;
                  } else {
                     ft = ft.equals(FetchType.NONE) ? fkfa.fetch_type() : ft;
                  }
                  if (ft.equals(FetchType.EAGER)) {
                     Class c = Class.forName(field.getType().getName());
                     manager = new Manager(c);
                     String s = String.format("%s_id", TableUtil.getColumnName(field));
                     Object o1 = rs.getObject(s);
                     Model m = manager.get("id", o1);
                     field.set(o, m);
                  } else {
                     field.set(o, null);
                  }
               } else {
                  if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                        this.connection.toString().startsWith("oracle")) {
                     if (rs.getObject(TableUtil.getColumnName(field)) == null) {
                        field.set(o, 0);
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        BigDecimal bigDecimal = (BigDecimal) rs.getObject(columnName);
                        field.set(o, bigDecimal.intValue());
                     }
                  } else {
                     Object columnValue = rs.getObject(TableUtil.getColumnName(field));
                     columnValue = convertZeroDateToNull(columnValue);
                     if (columnValue instanceof java.sql.Date) {
                        java.sql.Date date = (java.sql.Date) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(date.getTime());
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof java.sql.Time) {
                        java.sql.Time time = (java.sql.Time) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time.getTime());
                        calendar.set(Calendar.YEAR, 0);
                        calendar.set(Calendar.MONTH, 0);
                        calendar.set(Calendar.DAY_OF_MONTH, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof Timestamp) {
                        Timestamp timestamp = (Timestamp) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp.getTime());
                        columnValue = calendar.getTime();
                     }
                     field.set(o, columnValue);
                  }
               }
               manager = null;
            }
            T model = (T) o;
            if (model != null) {
               model.setPersisted(true);
            }
            qs.add(model);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return qs;
   }
   
   public <T extends Model> QuerySet<T> orderBy(String... fields) {
      QuerySet<T> qs = (QuerySet<T>) this.orderBy(this.entity, fields);
      return qs;
   }
   
   public <T extends Model> QuerySet<T> defer(String... fields) {
      QuerySet<T> qs = null;
      // TODO
      return qs;
   }
   
   public <T extends Model> QuerySet<T> only(String... fields) {
      QuerySet<T> qs = null;
      // TODO
      return qs;
   }
   
   public void using(String database) {
      // TODO
   }
   
   public <T extends Model> QuerySet<T> none() {
      QuerySet<T> qs = new QuerySet<T>();
      return qs;
   }
   
   public <T extends Model> T first() {
      return earliest();
   }
   
   public <T extends Model> T last() {
      return latest();
   }
   
   private Object convertZeroDateToNull(Object date) {
      if (date instanceof java.sql.Time) {
         date = date.toString().equals("00:00:00") ? null : date;
      } else if (date instanceof java.sql.Date) {
         date = date.toString().equals("0000-00-00 00:00:00") ? null : date;
      } else if (date instanceof java.sql.Timestamp) {
         date = date.toString().equals("00000000000000") ? null : date;
      }
      return date;
   }
   
   /**
    * Retorna se o Manager tem ou não uma conexão válida com o banco de dados.
    * 
    * @return boolean conectado ou não.
    */
   private boolean connected() {
      boolean connected = false;
      // a conexão existe?
      if (connection != null) {
         try {
            // a conexão é válida?
            connected = connection.isValid(DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT);
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return connected;
   }
   
   private boolean disconnected() {
      return !connected();
   }
   
   // private Connection connect() {
   // if (JediEngine.Pool.isNotActive()) {
   // if (disconnected()) {
   // connection = DataSource.getConnection();
   // }
   // } else {
   // connection = DataSource.getConnection();
   // }
   // return connection;
   // }
   
   private Connection connect() {
      if (disconnected()) {
         connection = DataSource.getConnection();
      }
      return connection;
   }
   
   private void disconnect() throws SQLException {
      if (connected()) {
         close(connection);
      }
   }
   
   /**
    * Fecha conexões, statements, etc.
    * 
    * @param o
    */
   private void close(Object o) {
      if (o != null && o instanceof AutoCloseable) {
         try {
            ((AutoCloseable) o).close();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
   
   private void close(Object... objects) {
      if (objects != null) {
         for (Object o : objects) {
            close(o);
         }
      }
   }
   
   private boolean autoCommit() {
      return AUTO_COMMIT.isValue();
   }
   
   private boolean autoClose() {
      return AUTO_CLOSE.isValue();
   }
   
   private void commit() {
      if (connected()) {
         try {
            if (!autoCommit()) {
               connection.commit();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
   
   private void rollback() {
      if (connected()) {
         try {
            connection.rollback();
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
   
   public <T extends Model> QuerySet<T> run(String sql) {
      return (QuerySet<T>) run(this.entity, sql);
   }
   
   private <T extends Model> QuerySet<T> run(Class<T> clazz, String sql) {
      QuerySet<T> qs = new QuerySet<>();
      PreparedStatement stmt = null;
      ResultSet rs = null;
      sql = sql == null ? "" : sql.trim();
      if (!sql.isEmpty()) {
         if (connect() != null) {
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            try {
               if (sql.toLowerCase().startsWith("select")) {
                  stmt = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                  rs = stmt.executeQuery();
                  qs = JediEngine.convert(rs, clazz);
               } else {
                  stmt = connection.prepareStatement(sql);
                  stmt.executeUpdate();
                  commit();
               }
            } catch (SQLException e) {
               e.printStackTrace();
               if (sql.toLowerCase().startsWith("select")) {
                  rollback();
               }
            } finally {
               close(stmt, rs, connection);
            }
         }
      }
      return qs;
   }
   
   @Override
   public <T extends Model> IManager delete(Integer... id) {
      if (id != null) {
         String sql = String.format("delete from %s where id in %s", tableName, Arrays.toString(id));
         sql = sql.replace("[", "(");
         sql = sql.replace("]", ")");
         this.run(sql);
      }
      return this;
   }
   
   @Override
   public <T extends Model> IManager delete(Model... models) {
      if (models != null) {
         for (Model model : models) {
            if (model != null) {
               model.delete();
            }
         }
      }
      return this;
   }
   
   @Override
   public <T extends Model> IManager delete() {
      List<T> records = this.all();
      for (T record : records) {
         record.delete();
      }
      return this;
   }
   
   /**
    * Recupera uma página de registros persistidos de uma entidade,
    * tendo como parâmetros o tamanho da página, a registro inicial e
    * a condição de pesquisa ou filtro.
    */
   @Override
   public <T extends Model> QuerySet<T> paginate(Integer limit, Integer offset, String where) {
      limit = limit == null ? 0 : limit;
      offset = offset == null ? 0 : offset;
      where = where == null || where.trim().isEmpty() ? "" : String.format(" where %s", where.trim());
      String sql = String.format("select * from %s%s order by id asc limit %d offset %d", this.getTableName(), where, limit, offset);
      return this.run(sql);
   }
   
   @Override
   public <T extends Model> QuerySet<T> paginate(Integer limit, Integer offset) {
      return paginate(limit, offset, null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> paginate(Integer limit) {
      return paginate(limit, null, null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> paginate() {
      return paginate(null, null, null);
   }
   
   /**
    * Recupera uma página de registros persistidos de uma entidade,
    * passando como parâmetros:
    * 1 - registro inicial.
    * 2 - tamanho da página.
    * 3 - ordenação.
    * 4 - filtro da pesquisa.
    */
   private <T extends Model> QuerySet<T> _page(Integer first, Integer pageSize, boolean reverseOrder, String... conditions) {
      first = first == null ? 0 : first;
      pageSize = pageSize == null ? 0 : pageSize;
      String orderBy = reverseOrder == false ? "ORDER BY id ASC" : "ORDER BY id DESC";
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      String sql = String.format("SELECT * FROM %s", tableName);
      String where = "";
      if (conditions != null && conditions.length > 0) {
         sql = String.format("SELECT * FROM %s WHERE", tableName);
         String fieldName = "";
         String fieldValue = "";
         // Iterates through the pairs field=value passed.
         for (int i = 0; i < conditions.length; i++) {
            conditions[i] = conditions[i] == null ? "" : conditions[i].trim();
            if (conditions[i].isEmpty()) {
               continue;
            }
            if (conditions[i].equalsIgnoreCase("AND")) {
               conditions[i] = "AND";
            }
            if (conditions[i].equalsIgnoreCase("OR")) {
               conditions[i] = "OR";
            }
            // Changes the name of the field to the corresponding pattern
            // name on the database.
            if (conditions[i].contains("=")) {
               fieldName = conditions[i].substring(0, conditions[i].lastIndexOf("="));
               fieldName = TableUtil.getColumnName(fieldName);
               fieldValue = conditions[i].substring(conditions[i].lastIndexOf("="));
               conditions[i] = String.format("%s%s", fieldName, fieldValue);
            }
            // Adds a blank space between the field name and value.
            conditions[i] = conditions[i].replace("=", " = ");
            // Replaces % by \%
            conditions[i] = conditions[i].replace("%", "\\%");
            // Adds a blank space between the values separated by commas.
            conditions[i] = conditions[i].replace(",", ", ");
            // Checks if the current pair contains __startswith, __contains
            // or __endswith.
            if (conditions[i].indexOf("__startswith") > -1 || conditions[i].indexOf("__!startswith") > -1 ||
                  conditions[i].indexOf("__istartswith") > -1 || conditions[i].indexOf("__!istartswith") > -1 ||
                  conditions[i].indexOf("__contains") > -1 || conditions[i].indexOf("__!contains") > -1 ||
                  conditions[i].indexOf("__icontains") > -1 || conditions[i].indexOf("__!icontains") > -1 ||
                  conditions[i].indexOf("__endswith") > -1 || conditions[i].indexOf("__!endswith") > -1 ||
                  conditions[i].indexOf("__iendswith") > -1 || conditions[i].indexOf("__!iendswith") > -1) {
               // Creates the LIKE SQL statement.
               if (conditions[i].indexOf("__startswith") > -1) {
                  conditions[i] = conditions[i].replace("__startswith = ", " LIKE ");
                  // Replaces 'value' by 'value%'.
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\'";
               } else if (conditions[i].indexOf("__!startswith") > -1) {
                  conditions[i] = conditions[i].replace("__!startswith = ", " NOT LIKE ");
                  // Replaces 'value' by 'value%'.
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\'";
               } else if (conditions[i].indexOf("__istartswith") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\')";
               } else if (conditions[i].indexOf("__!istartswith") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\')";
               } else if (conditions[i].indexOf("__contains") > -1) {
                  conditions[i] = conditions[i].replace("__contains = ", " LIKE ");
                  // Replaces 'value' by '%value%'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\'";
               } else if (conditions[i].indexOf("__!contains") > -1) {
                  conditions[i] = conditions[i].replace("__!contains = ", " NOT LIKE ");
                  // Replaces 'value' by '%value%'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\'";
               } else if (conditions[i].indexOf("__icontains") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\')";
               } else if (conditions[i].indexOf("__!icontains") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                  conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                  conditions[i] = conditions[i] + "%\')";
               } else if (conditions[i].indexOf("__endswith") > -1) {
                  conditions[i] = conditions[i].replace("__endswith = ", " LIKE ");
                  // Replaces 'value' by '%value'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
               } else if (conditions[i].indexOf("__!endswith") > -1) {
                  conditions[i] = conditions[i].replace("__!endswith = ", " NOT LIKE ");
                  // Replaces 'value' by '%value'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
               } else if (conditions[i].indexOf("__iendswith") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  // Replaces 'value' by '%value'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
               } else if (conditions[i].indexOf("__!iendswith") > -1) {
                  conditions[i] = conditions[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  // Replaces 'value' by '%value'.
                  conditions[i] = conditions[i].replaceFirst("\'", "\'%");
               } else {
                  
               }
            }
            if (conditions[i].indexOf("__in") > -1) {
               // Creates a IN statement in SQL.
               conditions[i] = conditions[i].replace("__in = ", " IN ");
               // Replaces [] by ()
               conditions[i] = conditions[i].replace("[", "(");
               conditions[i] = conditions[i].replace("]", ")");
            }
            if (conditions[i].indexOf("__!in") > -1) {
               // Creates a IN statement in SQL.
               conditions[i] = conditions[i].replace("__!in = ", " NOT IN ");
               // Replaces [] by ()
               conditions[i] = conditions[i].replace("[", "(");
               conditions[i] = conditions[i].replace("]", ")");
            }
            if (conditions[i].indexOf("__range") > -1) {
               // Creates a BETWEEN statement in SQL.
               conditions[i] = conditions[i].replace("__range = ", " BETWEEN ");
               // Removes [ or ] characters.
               conditions[i] = conditions[i].replace("[", "");
               conditions[i] = conditions[i].replace("]", "");
               // Replaces , (comma character) by AND.
               conditions[i] = conditions[i].replace(", ", " AND ");
            }
            if (conditions[i].indexOf("__!range") > -1) {
               // Creates a BETWEEN statement in SQL.
               conditions[i] = conditions[i].replace("__!range = ", " NOT BETWEEN ");
               // Removes [ or ] characters.
               conditions[i] = conditions[i].replace("[", "");
               conditions[i] = conditions[i].replace("]", "");
               // Replaces , (comma character) by AND.
               conditions[i] = conditions[i].replace(", ", " AND ");
            }
            if (conditions[i].indexOf("__lt") > -1) {
               conditions[i] = conditions[i].replace("__lt = ", " < ");
            }
            if (conditions[i].indexOf("__!lt") > -1) {
               conditions[i] = conditions[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
            }
            if (conditions[i].indexOf("__lte") > -1) {
               conditions[i] = conditions[i].replace("__lte = ", " <= ");
            }
            if (conditions[i].indexOf("__!lte") > -1) {
               conditions[i] = conditions[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
            }
            if (conditions[i].indexOf("__gt") > -1) {
               conditions[i] = conditions[i].replace("__gt = ", " > ");
            }
            if (conditions[i].indexOf("__!gt") > -1) {
               conditions[i] = conditions[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
            }
            if (conditions[i].indexOf("__gte") > -1) {
               conditions[i] = conditions[i].replace("__gte = ", " >= ");
            }
            if (conditions[i].indexOf("__!gte") > -1) {
               conditions[i] = conditions[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
            }
            if (conditions[i].indexOf("__exact") > -1) {
               conditions[i] = conditions[i].replace("__exact = ", " = ");
            }
            if (conditions[i].indexOf("__!exact") > -1) {
               conditions[i] = conditions[i].replace("__!exact = ", " != ");
            }
            if (conditions[i].indexOf("__isnull") > -1) {
               String bool = conditions[i].substring(conditions[i].indexOf("=") + 1, conditions[i].length()).trim();
               if (bool.equalsIgnoreCase("true")) {
                  conditions[i] = conditions[i].replace("__isnull = ", " IS NULL ");
               }
               if (bool.equalsIgnoreCase("false")) {
                  conditions[i] = conditions[i].replace("__isnull = ", " IS NOT NULL ");
               }
               conditions[i] = conditions[i].replace(bool, "");
            }
            where += conditions[i] + " AND ";
            where = where.replace(" AND OR AND", " OR");
            where = where.replace(" AND AND AND", " AND");
         }
      }
      try {
         if (where.lastIndexOf("AND") > -1) {
            where = where.substring(0, where.lastIndexOf("AND"));
         }
         where = where.trim();
         sql = String.format("%s %s %s LIMIT %d OFFSET %d", sql, where, orderBy, pageSize, first);
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         if (!resultSet.next()) {
            return querySet;
         }
         resultSet.beforeFirst();
         while (resultSet.next()) {
            Object obj = entity.newInstance();
            if (resultSet.getObject("id") != null) {
               Field id = jedi.db.models.Model.class.getDeclaredField("id");
               id.setAccessible(true);
               if (this.connection.toString().startsWith("oracle")) {
                  id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
               } else {
                  id.set(obj, resultSet.getObject(id.getName()));
               }
            }
            List<Field> _fields = JediEngine.getAllFields(this.entity);
            for (Field field : _fields) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.getName().equals("serialVersionUID"))
                  continue;
               if (field.getName().equalsIgnoreCase("objects"))
                  continue;
               oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
               foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
               manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
               FetchType fetchType = JediEngine.FETCH_TYPE;
               Manager manager = null;
               if (manyToManyFieldAnnotation != null) {
                  fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                  if (fetchType.equals(FetchType.EAGER)) {
                     Class superClazz = null;
                     Class clazz = null;
                     String packageName = this.entity.getPackage().getName();
                     String model = manyToManyFieldAnnotation.model().getSimpleName();
                     model = Model.class.getSimpleName().equals(model) ? "" : model;
                     if (model.isEmpty()) {
                        ParameterizedType genericType = null;
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           genericType = (ParameterizedType) field.getGenericType();
                           superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                           if (superClazz == Model.class) {
                              clazz = (Class) genericType.getActualTypeArguments()[0];
                              model = clazz.getSimpleName();
                           }
                        }
                     }
                     String references = manyToManyFieldAnnotation.references();
                     if (references == null || references.trim().isEmpty()) {
                        if (clazz != null) {
                           references = TableUtil.getTableName(clazz);
                        } else {
                           references = TableUtil.getTableName(model);
                        }
                     }
                     Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                     manager = new Manager(associatedModelClass);
                     List<List<Map<String, Object>>> recordSet = null;
                     recordSet = manager.raw(
                           String.format(
                                 "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                                 TableUtil.getColumnName(model),
                                 tableName,
                                 TableUtil.getTableName(references),
                                 TableUtil.getColumnName(this.entity),
                                 ((Model) obj).id()));
                     String args = recordSet.toString();
                     args = args.replace("[", "");
                     args = args.replace("{", "");
                     args = args.replace("]", "");
                     args = args.replace("}", "");
                     args = args.replace("=", "");
                     args = args.replace(", ", ",");
                     args = args.replace(String.format("%s_id", TableUtil.getColumnName(model)), "");
                     args = String.format("id__in=[%s]", args);
                     QuerySet querySetAssociatedModels = manager._filter(args);
                     field.set(obj, querySetAssociatedModels);
                  } else {
                     field.set(obj, null);
                  }
               } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                  if (oneToOneFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                  } else {
                     fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                  }
                  if (fetchType.equals(FetchType.EAGER)) {
                     Class associatedModelClass = Class.forName(field.getType().getName());
                     manager = new Manager(associatedModelClass);
                     String columnName = TableUtil.getColumnName(field);
                     Object id = resultSet.getObject(String.format("%s_id", columnName));
                     Model associatedModel = manager.get("id", id);
                     field.set(obj, associatedModel);
                  } else {
                     field.set(obj, null);
                  }
               } else {
                  if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                        this.connection.toString().startsWith("oracle")) {
                     if (resultSet.getObject(TableUtil.getColumnName(field.getName())) == null) {
                        field.set(obj, 0);
                     } else {
                        String columnName = TableUtil.getColumnName(field.getName());
                        BigDecimal columnValue = (BigDecimal) resultSet.getObject(columnName);
                        field.set(obj, columnValue.intValue());
                     }
                  } else {
                     String columnName = TableUtil.getColumnName(field.getName());
                     Object columnValue = resultSet.getObject(columnName);
                     if (columnValue instanceof java.sql.Date) {
                        java.sql.Date date = (java.sql.Date) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(date.getTime());
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof java.sql.Time) {
                        java.sql.Time time = (java.sql.Time) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time.getTime());
                        calendar.set(Calendar.YEAR, 0);
                        calendar.set(Calendar.MONTH, 0);
                        calendar.set(Calendar.DAY_OF_MONTH, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof Timestamp) {
                        Timestamp timestamp = (Timestamp) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp.getTime());
                        columnValue = calendar.getTime();
                     }
                     field.set(obj, columnValue);
                  }
               }
               manager = null;
            }
            T model = (T) obj;
            if (model != null) {
               model.setPersisted(true);
            }
            querySet.add(model);
         }
         if (querySet != null) {
            querySet.setPersisted(true);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(resultSet, statement, connection);
      }
      return querySet;
   }
   
   public <T extends Model> QuerySet<T> _page(Integer first, Integer pageSize, String... conditions) {
      return _page(first, pageSize, false, conditions);
   }
   
   public <T extends Model> QuerySet<T> _page(Integer first, Integer pageSize) {
      return _page(first, pageSize, false, (String[]) null);
   }
   
   public <T extends Model> QuerySet<T> _reversePage(Integer first, Integer pageSize, String... conditions) {
      return _page(first, pageSize, true, conditions);
   }
   
   public <T extends Model> QuerySet<T> _reversePage(Integer first, Integer pageSize) {
      return _page(first, pageSize, true, (String[]) null);
   }
   
   public static List<String> joins(String... lookups) {
      List<String> joins = (List<String>) EMPTY_LIST;
      if (lookups != null) {
         joins = new ArrayList<>();
         String previousJoin = "";
         for (String lookup : lookups) {
            lookup = lookup == null ? "" : lookup.trim();
            if (!lookup.isEmpty() && lookup.contains(".")) {
               String join = lookup.substring(0, lookup.lastIndexOf("."));
               if (!previousJoin.isEmpty() && join.contains(previousJoin)) {
                  String s = previousJoin.replaceFirst("\\.\\w+", "");
                  join = join.replace(String.format("%s.", s), "");
               }
               if (!joins.contains(join)) {
                  joins.add(join);
                  previousJoin = join;
               }
            }
         }
      }
      return joins;
   }
   
   // TODO - tornar fields como um array de Object de forma que ao ser passado
   // um Model
   // TODO - seja feito uma consulta join.
   public <T extends Model> QuerySet<T> filter(Class<T> modelClass, String... fields) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      if (fields != null) {
         try {
            String sql = String.format("SELECT * FROM %s WHERE", tableName);
            String format = "SELECT\n\t*\nFROM\n\t%s AS %s\n%s%s";
            String joins = "";
            String join = "";
            String where = "";
            String fieldName = "";
            String fieldValue = "";
            // Iterates through the pairs field=value passed.
            for (int i = 0; i < fields.length; i++) {
               fields[i] = fields[i] == null ? "" : fields[i].trim();
               if (fields[i].isEmpty()) {
                  continue;
               }
               join = FieldLookup.translateJoin(entityName + "." + fields[i]);
               if (!joins.contains(join)) {
                  joins += join;
                  if (fields[i].matches("(\\w+\\.)+\\w+__.*")) {
                     fields[i] = fields[i].replace(fields[i].replaceAll("\\w+\\.\\w+__.*", ""), "");
                  }
               }
               if (fields[i].equalsIgnoreCase("AND")) {
                  fields[i] = "AND";
               }
               if (fields[i].equalsIgnoreCase("OR")) {
                  fields[i] = "OR";
               }
               String[] decomposedEl = FieldLookup.decompose(fields[i]);
               if (decomposedEl.length > 0) {
                  Field associationField = JediEngine.getField(decomposedEl[0], entity);
                  if (JediEngine.isOneToOneField(associationField) || JediEngine.isForeignKeyField(associationField)) {
                     fields[i] = fields[i].replace(decomposedEl[0], decomposedEl[0] + "_id");
                  }
               }
               // Changes the name of the field to the corresponding pattern
               // name on the database.
               if (fields[i].contains("=")) {
                  fieldName = fields[i].substring(0, fields[i].lastIndexOf("="));
                  fieldName = TableUtil.getColumnName(fieldName);
                  fieldValue = fields[i].substring(fields[i].lastIndexOf("="));
                  fields[i] = String.format("%s%s", fieldName, fieldValue);
               }
               // Adds a blank space between the field name and value.
               fields[i] = fields[i].replace("=", " = ");
               // Replaces % by \%
               fields[i] = fields[i].replace("%", "\\%");
               // Adds a blank space between the values separated by commas.
               fields[i] = fields[i].replace(",", ", ");
               // Checks if the current pair contains __startswith, __contains
               // or __endswith.
               if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__!startswith") > -1 ||
                     fields[i].indexOf("__istartswith") > -1 || fields[i].indexOf("__!istartswith") > -1 ||
                     fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__icontains") > -1 || fields[i].indexOf("__!contains") > -1 ||
                     fields[i].indexOf("__!icontains") > -1 || fields[i].indexOf("__endswith") > -1 || fields[i].indexOf("__!endswith") > -1 ||
                     fields[i].indexOf("__iendswith") > -1 || fields[i].indexOf("__!iendswith") > -1) {
                  // Creates a LIKE statement in SQL.
                  if (fields[i].indexOf("__startswith") > -1) {
                     fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!startswith") > -1) {
                     fields[i] = fields[i].replace("__!startswith = ", " NOT LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__contains") > -1) {
                     fields[i] = fields[i].replace("__contains = ", " LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!contains") > -1) {
                     fields[i] = fields[i].replace("__!contains = ", " NOT LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__endswith") > -1) {
                     fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!endswith") > -1) {
                     fields[i] = fields[i].replace("__!endswith = ", " NOT LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  }
               }
               if (fields[i].indexOf("__in") > -1) {
                  // Creates a IN statement in SQL.
                  fields[i] = fields[i].replace("__in = ", " IN ");
                  // Replaces [] by ()
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__!in") > -1) {
                  // Creates a IN statement in SQL.
                  fields[i] = fields[i].replace("__!in = ", " NOT IN ");
                  // Replaces [] by ()
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__range") > -1) {
                  // Creates a BETWEEN statement in SQL.
                  fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                  // Removes [ or ] characters.
                  fields[i] = fields[i].replace("[", "");
                  fields[i] = fields[i].replace("]", "");
                  // Replaces , (comma character) by AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__!range") > -1) {
                  // Creates a BETWEEN statement in SQL.
                  fields[i] = fields[i].replace("__!range = ", " NOT BETWEEN ");
                  // Removes [ or ] characters.
                  fields[i] = fields[i].replace("[", "");
                  fields[i] = fields[i].replace("]", "");
                  // Replaces , (comma character) by AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__regex") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__regex ?= ?(.*)$", " $1 REGEXP BINARY '$2'");
               }
               if (fields[i].indexOf("__iregex") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__iregex ?= ?(.*)$", " $1 REGEXP '$2'");
               }
               if (fields[i].matches(".*__year__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__lt *= *(\\d{4})$", " $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?< ?(\\d{4})$", " $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!lt *= *(\\d{4})$", " NOT $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__lte *= *(\\d{4})$", " $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?<= ?(\\d{4})$", " $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!lte *= *(\\d{4})$", " NOT $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__gt *= *(\\d{4})$", " $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?> ?(\\d{4})$", " $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!gt *= *(\\d{4})$", " NOT $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__gte *= *(\\d{4})$", " $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?>= ?(\\d{4})$", " $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!gte *= *(\\d{4})$", " NOT $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__exact *= *(\\d{4})$", " $1 = '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!exact *= *(\\d{4})$", " NOT $1 = '$2-01-01'");
               }
               if (fields[i].matches(".*__year *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year *= *(\\d{4})$", " $1 BETWEEN '$2-01-01' AND '$2-12-31'");
               }
               if (fields[i].matches(".*__!year *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!year *= *(\\d{4})$", " $1 NOT BETWEEN '$2-01-01' AND '$2-12-31'");
               }
               if (fields[i].matches(".*__year ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?!= ?(\\d{4})$", " $1 != '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?<> ?(\\d{4})$", " $1 <> '$2-01-01'");
               }
               if (fields[i].matches(".*__month__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__lt *= *(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?< ?(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!lt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__lte *= *(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?<= ?(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!lte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__gt *= *(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?> ?(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!gt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__gte *= *(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?>= ?(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!gte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__exact *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!exact *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!month *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!month *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__month ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__day__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__lt *= *(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?< ?(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!lt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__lte *= *(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?<= ?(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!lte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__gt *= *(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?> ?(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!gt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__gte *= *(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?>= ?(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!gte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__exact *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!exact *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!day *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__day ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__week_day__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__lt *= *(\\d{2})$", " WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?< ?(\\d{2})$", " WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!lt *= *(\\d{2})$", " NOT WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__lte *= *(\\d{2})$", " WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?<= ?(\\d{2})$", " WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!lte *= *(\\d{2})$", " NOT WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__gt *= *(\\d{2})$", " WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?> ?(\\d{2})$", " WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!gt *= *(\\d{2})$", " NOT WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__gte *= *(\\d{2})$", " WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?>= ?(\\d{2})$", " WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!gte *= *(\\d{2})$", " NOT WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__!week_day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!week_day *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?!= ?(\\d{2})$", " WEEKDAY($1) != '$2'");
               }
               if (fields[i].matches(".*__week_day ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?<> ?(\\d{2})$", " WEEKDAY($1) <> '$2'");
               }
               if (fields[i].matches(".*__hour__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__lt *= *(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?< ?(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!lt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__lte *= *(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?<= ?(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!lte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__gt *= *(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?> ?(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!gt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__gte *= *(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?>= ?(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!gte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__exact *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!exact *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!hour *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!hour *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?!= ?(\\d{2})$", " EXTRACT(hour FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__hour ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?<> ?(\\d{2})$", " EXTRACT(hour FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__minute__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__lt *= *(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?< ?(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!lt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__lte *= *(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?<= ?(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!lte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__gt *= *(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?> ?(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!gt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__gte *= *(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?>= ?(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!gte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__exact *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!exact *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!minute *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!minute *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?!= ?(\\d{2})$", " EXTRACT(minute FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__minute ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?<> ?(\\d{2})$", " EXTRACT(minute FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__second__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__lt *= *(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?< ?(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!lt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__lte *= *(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?<= ?(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!lte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__gt *= *(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?> ?(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!gt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__gte *= *(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?>= ?(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!gte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__exact *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!exact *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!second *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!second *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?!= ?(\\d{2})$", " EXTRACT(second FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__second ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?<> ?(\\d{2})$", " EXTRACT(second FROM $1) <> '$2'");
               }
               if (fields[i].indexOf("__lt") > -1) {
                  fields[i] = fields[i].replace("__lt = ", " < ");
               }
               if (fields[i].indexOf("__!lt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
               }
               if (fields[i].indexOf("__lte") > -1) {
                  fields[i] = fields[i].replace("__lte = ", " <= ");
               }
               if (fields[i].indexOf("__!lte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
               }
               if (fields[i].indexOf("__gt") > -1) {
                  fields[i] = fields[i].replace("__gt = ", " > ");
               }
               if (fields[i].indexOf("__!gt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
               }
               if (fields[i].indexOf("__gte") > -1) {
                  fields[i] = fields[i].replace("__gte = ", " >= ");
               }
               if (fields[i].indexOf("__!gte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
               }
               if (fields[i].indexOf("__exact") > -1) {
                  fields[i] = fields[i].replace("__exact = ", " = ");
               }
               if (fields[i].indexOf("__!exact") > -1) {
                  fields[i] = fields[i].replace("__!exact = ", " != ");
               }
               if (fields[i].indexOf("__isnull") > -1) {
                  String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length()).trim();
                  if (bool.equalsIgnoreCase("true")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
                  }
                  if (bool.equalsIgnoreCase("false")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
                  }
                  fields[i] = fields[i].replace(bool, "");
               }
               where += fields[i] + " AND ";
               where = where.replace(" AND OR AND", " OR");
               where = where.replace(" AND AND AND", " AND");
            }
            where = where.substring(0, where.lastIndexOf("AND"));
            where = where.trim();
            if (joins.isEmpty()) {
               sql = String.format("%s %s", sql, where);
            } else {
               sql = String.format(format, tableName, entityName, joins, where.isEmpty() ? "" : "WHERE\n\t" + where);
               // controla o grau de identação da instrução SQL.
               sql = sql.replaceAll("\t", "    ");
            }
            String _sql = likeSqlDateTime(sql);
            if (!_sql.isEmpty()) {
               sql = _sql;
            }
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
               return querySet;
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               Object obj = entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  if (connection.toString().startsWith("oracle")) {
                     id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
                  } else {
                     id.set(obj, resultSet.getObject(id.getName()));
                  }
               }
               // Iterates through the fields of the model.
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field field : _fields) {
                  // Sets private or protected fields as accessible.
                  field.setAccessible(true);
                  // Discards non annotated fields
                  if (!JediEngine.isJediField(field)) {
                     continue;
                  }
                  // Discards the serialVersionUID field.
                  if (field.getName().equals("serialVersionUID"))
                     continue;
                  // Discards the objects field.
                  if (field.getName().equalsIgnoreCase("objects"))
                     continue;
                  // Checks if the field are annotated as OneToOneField,
                  // ForeignKeyField or ManyToManyField.
                  oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class superClazz = null;
                        Class clazz = null;
                        String packageName = this.entity.getPackage().getName();
                        String model = manyToManyFieldAnnotation.model().getSimpleName();
                        model = Model.class.getSimpleName().equals(model) ? "" : model;
                        if (model.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 model = clazz.getSimpleName();
                              }
                           }
                        }
                        String references = manyToManyFieldAnnotation.references();
                        if (references == null || references.trim().isEmpty()) {
                           if (clazz != null) {
                              references = TableUtil.getTableName(clazz);
                           } else {
                              references = TableUtil.getTableName(model);
                           }
                        }
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                        manager = new Manager(associatedModelClass);
                        List<List<Map<String, Object>>> recordSet = null;
                        // Performs a SQL query.
                        recordSet = manager.raw(
                           String.format(
                              "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                              TableUtil.getColumnName(model),
                              tableName,
                              TableUtil.getTableName(references),
                              TableUtil.getColumnName(this.entity),
                              ((Model) obj).id()
                           )
                        );
                        String args = recordSet.toString();
                        args = args.replace("[", "");
                        args = args.replace("{", "");
                        args = args.replace("]", "");
                        args = args.replace("}", "");
                        args = args.replace("=", "");
                        args = args.replace(", ", ",");
                        args = args.replace(String.format("%s_id", TableUtil.getColumnName(model)), "");
                        args = String.format("id__in=[%s]", args);
                        QuerySet querySetAssociatedModels = manager._filter(args);
                        field.set(obj, querySetAssociatedModels);
                     } else {
                        field.set(obj, null);
                     }
                  } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                     if (oneToOneFieldAnnotation != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     }
                     if (fetchType.equals(FetchType.EAGER)) {
                        // If it's recovers the field's class.
                        Class associatedModelClass = Class.forName(field.getType().getName());
                        // Instanciates a Model Manager.
                        manager = new Manager(associatedModelClass);
                        String columnName = TableUtil.getColumnName(field);
                        Object id = resultSet.getObject(String.format("%s_id", columnName));
                        Model associatedModel = manager.get("id", id);
                        // Calls the get method recursivelly.
                        // References the model associated by foreign key
                        // annotation.
                        field.set(obj, associatedModel);
                     } else {
                        field.set(obj, null);
                     }
                  } else {
                     // Sets fields the aren't Model's instances.
                     if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        if (resultSet.getObject(TableUtil.getColumnName(field.getName())) == null) {
                           field.set(obj, 0);
                        } else {
                           String columnName = TableUtil.getColumnName(field.getName());
                           BigDecimal columnValue = (BigDecimal) resultSet.getObject(columnName);
                           field.set(obj, columnValue.intValue());
                        }
                     } else {
                        String columnName = TableUtil.getColumnName(field.getName());
                        Object columnValue = resultSet.getObject(columnName);
                        if (columnValue instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           columnValue = calendar.getTime();
                        }
                        field.set(obj, columnValue);
                     }
                  }
                  manager = null;
               }
               T model = (T) obj;
               if (model != null) {
                  model.setPersisted(true);
               }
               querySet.add(model);
            }
            if (querySet != null) {
               querySet.setPersisted(true);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(statement, resultSet, connection);
         }
      }
      return querySet;
   }
   
   public <T extends Model> QuerySet<T> filter(String... fields) {
      return (QuerySet<T>) this.filter(this.entity, fields);
   }
   
   public int count(String... conditions) {
      int rows = 0;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
         String sql = String.format("SELECT COUNT(id) AS \"rows\" FROM %s", tableName);
         String format = "SELECT\n\tCOUNT(%s.id) AS rows\nFROM\n\t%s AS %s\n%s";
         String join = "";
         String joins = "";
         if (conditions != null && conditions.length > 0) {
            String where = "WHERE";
            for (int i = 0; i < conditions.length; i++) {
               conditions[i] = conditions[i] == null ? "" : conditions[i].trim();
               if (!conditions[i].isEmpty()) {
                  join = FieldLookup.translateJoin(entityName + "." + conditions[i]);
                  if (!joins.contains(join)) {
                     joins += join;
                     conditions[i] = conditions[i].replace(conditions[i].replaceAll("\\w+\\.\\w+__.*", ""), "");
                  }
                  if (conditions[i].equalsIgnoreCase("AND")) {
                     conditions[i] = "AND";
                  }
                  if (conditions[i].equalsIgnoreCase("OR")) {
                     conditions[i] = "OR";
                  }
                  String[] decomposedEl = FieldLookup.decompose(conditions[i]);
                  if (decomposedEl.length > 0) {
                     Field associationField = JediEngine.getField(decomposedEl[0], entity);
                     if (JediEngine.isOneToOneField(associationField) || JediEngine.isForeignKeyField(associationField)) {
                        conditions[i] = conditions[i].replace(decomposedEl[0], decomposedEl[0] + "_id");
                     }
                  }
                  /*
                   * Changes the name of the field to reflect the name
                   * pattern of the table columns.
                   */
                  if (conditions[i].contains("=")) {
                     String fieldName = conditions[i].substring(0, conditions[i].lastIndexOf("="));
                     String fieldValue = conditions[i].substring(conditions[i].lastIndexOf("="));
                     conditions[i] = String.format("%s%s", TableUtil.getColumnName(fieldName), fieldValue);
                  }
                  // Adds a blank space between the field's name and value.
                  conditions[i] = conditions[i].replace("=", " = ");
                  // Replaces % by \%
                  conditions[i] = conditions[i].replace("%", "\\%");
                  // Adds a blank space between the values separated by comma
                  // character.
                  conditions[i] = conditions[i].replace(",", ", ");
                  // Checks if the current pair contains __startswith,
                  // __contains or __endswith.
                  if (conditions[i].indexOf("__startswith") > -1 || conditions[i].indexOf("__!startswith") > -1 ||
                        conditions[i].indexOf("__istartswith") > -1 || conditions[i].indexOf("__!istartswith") > -1 ||
                        conditions[i].indexOf("__contains") > -1 || conditions[i].indexOf("__!contains") > -1 ||
                        conditions[i].indexOf("__icontains") > -1 || conditions[i].indexOf("__!icontains") > -1 ||
                        conditions[i].indexOf("__endswith") > -1 || conditions[i].indexOf("__!endswith") > -1 ||
                        conditions[i].indexOf("__iendswith") > -1 || conditions[i].indexOf("__!iendswith") > -1) {
                     // Creates the LIKE SQL statement.
                     if (conditions[i].indexOf("__startswith") > -1) {
                        conditions[i] = conditions[i].replace("__startswith = ", " LIKE ");
                        // Replaces 'value' by 'value%'.
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__!startswith") > -1) {
                        conditions[i] = conditions[i].replace("__!startswith = ", " NOT LIKE ");
                        // Replaces 'value' by 'value%'.
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__istartswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__!istartswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__contains") > -1) {
                        conditions[i] = conditions[i].replace("__contains = ", " LIKE ");
                        // Replaces 'value' by '%value%'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__!contains") > -1) {
                        conditions[i] = conditions[i].replace("__!contains = ", " NOT LIKE ");
                        // Replaces 'value' by '%value%'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\'";
                     } else if (conditions[i].indexOf("__icontains") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__!icontains") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                        conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                        conditions[i] = conditions[i] + "%\')";
                     } else if (conditions[i].indexOf("__endswith") > -1) {
                        conditions[i] = conditions[i].replace("__endswith = ", " LIKE ");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__!endswith") > -1) {
                        conditions[i] = conditions[i].replace("__!endswith = ", " NOT LIKE ");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__iendswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else if (conditions[i].indexOf("__!iendswith") > -1) {
                        conditions[i] = conditions[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                        // Replaces 'value' by '%value'.
                        conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                     } else {
                        
                     }
                  }
                  if (conditions[i].indexOf("__in") > -1) {
                     // Creates the IN SQL statement.
                     conditions[i] = conditions[i].replace("__in = ", " IN ");
                     // Replaces the [] characters by ().
                     conditions[i] = conditions[i].replace("[", "(");
                     conditions[i] = conditions[i].replace("]", ")");
                  }
                  if (conditions[i].indexOf("__!in") > -1) {
                     // Creates a IN statement in SQL.
                     conditions[i] = conditions[i].replace("__!in = ", " NOT IN ");
                     // Replaces [] by ()
                     conditions[i] = conditions[i].replace("[", "(");
                     conditions[i] = conditions[i].replace("]", ")");
                  }
                  if (conditions[i].indexOf("__range") > -1) {
                     // Creates the BETWEEN SQL statement.
                     conditions[i] = conditions[i].replace("__range = ", " BETWEEN ");
                     // Removes the [ or ] characters.
                     conditions[i] = conditions[i].replace("[", "");
                     conditions[i] = conditions[i].replace("]", "");
                     // Replaces the comma character by AND.
                     conditions[i] = conditions[i].replace(", ", " AND ");
                  }
                  if (conditions[i].indexOf("__!range") > -1) {
                     conditions[i] = conditions[i].replace("__!range = ", " NOT BETWEEN ");
                     conditions[i] = conditions[i].replace("[", "");
                     conditions[i] = conditions[i].replace("]", "");
                     conditions[i] = conditions[i].replace(", ", " AND ");
                  }
                  if (conditions[i].indexOf("__regex") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__regex ?= ?(.*)$", " $1 REGEXP BINARY '$2'");
                  }
                  if (conditions[i].indexOf("__iregex") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__iregex ?= ?(.*)$", " $1 REGEXP '$2'");
                  }
                  if (conditions[i].matches(".*__year__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__lt *= *(\\d{4})$", " $1 < '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?< ?(\\d{4})$", " $1 < '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__!lt *= *(\\d{4})$", " NOT $1 < '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__lte *= *(\\d{4})$", " $1 <= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?<= ?(\\d{4})$", " $1 <= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__!lte *= *(\\d{4})$", " NOT $1 <= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__gt *= *(\\d{4})$", " $1 > '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?> ?(\\d{4})$", " $1 > '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__!gt *= *(\\d{4})$", " NOT $1 > '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__gte *= *(\\d{4})$", " $1 >= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?>= ?(\\d{4})$", " $1 >= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__!gte *= *(\\d{4})$", " NOT $1 >= '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__exact *= *(\\d{4})$", " $1 = '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year__!exact *= *(\\d{4})$", " NOT $1 = '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year *= *(\\d{4})$", " $1 BETWEEN '$2-01-01' AND '$2-12-31'");
                  }
                  if (conditions[i].matches(".*__!year *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!year *= *(\\d{4})$", " $1 NOT BETWEEN '$2-01-01' AND '$2-12-31'");
                  }
                  if (conditions[i].matches(".*__year ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?!= ?(\\d{4})$", " $1 != '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__year ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__year ?<> ?(\\d{4})$", " $1 <> '$2-01-01'");
                  }
                  if (conditions[i].matches(".*__month__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__lt *= *(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__month ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?< ?(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__month__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__!lt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__month__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__lte *= *(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__month ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?<= ?(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__month__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__!lte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__month__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__gt *= *(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__month ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?> ?(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__month__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__!gt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__month__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__gte *= *(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__month ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?>= ?(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__month__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__!gte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__month__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__exact *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__month__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month__!exact *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__month *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!month *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!month *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__month ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
                  }
                  if (conditions[i].matches(".*__month ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__month ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
                  }
                  if (conditions[i].matches(".*__day__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__lt *= *(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__day ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?< ?(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__day__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__!lt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__day__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__lte *= *(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__day ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?<= ?(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__day__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__!lte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__day__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__gt *= *(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__day ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?> ?(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__day__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__!gt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__day__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__gte *= *(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__day ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?>= ?(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__day__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__!gte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__day__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__exact *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__day__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day__!exact *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__day *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!day *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!day *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__day ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
                  }
                  if (conditions[i].matches(".*__day ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__day ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__lt *= *(\\d{2})$", " WEEKDAY($1) < '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?< ?(\\d{2})$", " WEEKDAY($1) < '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__!lt *= *(\\d{2})$", " NOT WEEKDAY($1) < '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__lte *= *(\\d{2})$", " WEEKDAY($1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?<= ?(\\d{2})$", " WEEKDAY($1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__!lte *= *(\\d{2})$", " NOT WEEKDAY($1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__gt *= *(\\d{2})$", " WEEKDAY($1) > '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?> ?(\\d{2})$", " WEEKDAY($1) > '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__!gt *= *(\\d{2})$", " NOT WEEKDAY($1) > '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__gte *= *(\\d{2})$", " WEEKDAY($1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?>= ?(\\d{2})$", " WEEKDAY($1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__!gte *= *(\\d{2})$", " NOT WEEKDAY($1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
                  }
                  if (conditions[i].matches(".*__week_day__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
                  }
                  if (conditions[i].matches(".*__week_day *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!week_day *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!week_day *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?!= ?(\\d{2})$", " WEEKDAY($1) != '$2'");
                  }
                  if (conditions[i].matches(".*__week_day ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__week_day ?<> ?(\\d{2})$", " WEEKDAY($1) <> '$2'");
                  }
                  if (conditions[i].matches(".*__hour__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__lt *= *(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?< ?(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__hour__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__!lt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__hour__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__lte *= *(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?<= ?(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__hour__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__!lte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__hour__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__gt *= *(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?> ?(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__hour__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__!gt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__hour__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__gte *= *(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?>= ?(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__hour__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__!gte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__hour__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__exact *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__hour__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour__!exact *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__hour *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!hour *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!hour *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?!= ?(\\d{2})$", " EXTRACT(hour FROM $1) != '$2'");
                  }
                  if (conditions[i].matches(".*__hour ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__hour ?<> ?(\\d{2})$", " EXTRACT(hour FROM $1) <> '$2'");
                  }
                  if (conditions[i].matches(".*__minute__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__lt *= *(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?< ?(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__minute__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__!lt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__minute__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__lte *= *(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?<= ?(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__minute__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__!lte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__minute__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__gt *= *(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?> ?(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__minute__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__!gt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__minute__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__gte *= *(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?>= ?(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__minute__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__!gte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__minute__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__exact *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__minute__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute__!exact *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__minute *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!minute *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!minute *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?!= ?(\\d{2})$", " EXTRACT(minute FROM $1) != '$2'");
                  }
                  if (conditions[i].matches(".*__minute ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__minute ?<> ?(\\d{2})$", " EXTRACT(minute FROM $1) <> '$2'");
                  }
                  if (conditions[i].matches(".*__second__lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__lt *= *(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__second ?< ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?< ?(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__second__!lt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__!lt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) < '$2'");
                  }
                  if (conditions[i].matches(".*__second__lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__lte *= *(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__second ?<= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?<= ?(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__second__!lte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__!lte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) <= '$2'");
                  }
                  if (conditions[i].matches(".*__second__gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__gt *= *(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__second ?> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?> ?(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__second__!gt.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__!gt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) > '$2'");
                  }
                  if (conditions[i].matches(".*__second__gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__gte *= *(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__second ?>= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?>= ?(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__second__!gte.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__!gte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) >= '$2'");
                  }
                  if (conditions[i].matches(".*__second__exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__exact *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__second__!exact.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second__!exact *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__second *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__!second *= *.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!second *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
                  }
                  if (conditions[i].matches(".*__second ?!= ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?!= ?(\\d{2})$", " EXTRACT(second FROM $1) != '$2'");
                  }
                  if (conditions[i].matches(".*__second ?<> ?.*")) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__second ?<> ?(\\d{2})$", " EXTRACT(second FROM $1) <> '$2'");
                  }
                  if (conditions[i].indexOf("__lt") > -1) {
                     conditions[i] = conditions[i].replace("__lt = ", " < ");
                  }
                  if (conditions[i].indexOf("__!lt") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
                  }
                  if (conditions[i].indexOf("__lte") > -1) {
                     conditions[i] = conditions[i].replace("__lte = ", " <= ");
                  }
                  if (conditions[i].indexOf("__!lte") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
                  }
                  if (conditions[i].indexOf("__gt") > -1) {
                     conditions[i] = conditions[i].replace("__gt = ", " > ");
                  }
                  if (conditions[i].indexOf("__!gt") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
                  }
                  if (conditions[i].indexOf("__gte") > -1) {
                     conditions[i] = conditions[i].replace("__gte = ", " >= ");
                  }
                  if (conditions[i].indexOf("__!gte") > -1) {
                     conditions[i] = conditions[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
                  }
                  if (conditions[i].indexOf("__exact") > -1) {
                     conditions[i] = conditions[i].replace("__exact = ", " = ");
                  }
                  if (conditions[i].indexOf("__!exact") > -1) {
                     conditions[i] = conditions[i].replace("__!exact = ", " != ");
                  }
                  if (conditions[i].indexOf("__isnull") > -1) {
                     String bool = conditions[i].substring(conditions[i].indexOf("=") + 1, conditions[i].length()).trim();
                     if (bool.equalsIgnoreCase("true")) {
                        conditions[i] = conditions[i].replace("__isnull = ", " IS NULL ");
                     }
                     if (bool.equalsIgnoreCase("false")) {
                        conditions[i] = conditions[i].replace("__isnull = ", " IS NOT NULL ");
                     }
                     conditions[i] = conditions[i].replace(bool, "");
                  }
                  where += " " + conditions[i] + " AND";
                  where = where.replace(" AND OR AND", " OR");
                  where = where.replace(" AND AND AND", " AND");
               }
            }
            if (where.indexOf(" AND") > -1) {
               where = where.substring(0, where.lastIndexOf("AND"));
               where = where.trim();
               if (joins.isEmpty()) {
                  sql = String.format("%s %s", sql, where);
               } else {
                  sql = String.format(format, entityName, tableName, entityName, joins);
                  sql += where.isEmpty() ? "" : where.replace("WHERE", "WHERE\n\t");
                  sql = sql.replaceAll("\t", "    ");
               }
            }
         }
         String _sql = likeSqlDateTime(sql);
         if (!_sql.isEmpty()) {
            sql = _sql;
         }
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         while (resultSet.next()) {
            rows = resultSet.getInt("rows");
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(statement, resultSet, connection);
      }
      return rows;
   }
   
   /**
    * Conta todos os registros da entidade
    * persistidos no banco de dados.
    */
   public int count() {
      return count("");
   }
   
   public <T extends Model> QuerySet<T> exclude(String... fields) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      if (fields != null) {
         try {
            String sql = String.format("SELECT * FROM %s WHERE", tableName);
            String where = "";
            String format = "SELECT\n\t*\nFROM\n\t%s AS %s\n%s%s";
            String joins = "";
            String join = "";
            // Iterates through the pairs field=value.
            for (int i = 0; i < fields.length; i++) {
               fields[i] = fields[i] == null ? "" : fields[i].trim();
               if (fields[i].isEmpty()) {
                  continue;
               }
               join = FieldLookup.translateJoin(entityName + "." + fields[i]);
               if (!joins.contains(join)) {
                  joins += join;
                  fields[i] = fields[i].replace(fields[i].replaceAll("\\w+\\.\\w+__.*", ""), "");
               }
               if (fields[i].equalsIgnoreCase("AND")) {
                  fields[i] = "AND";
               }
               if (fields[i].equalsIgnoreCase("OR")) {
                  fields[i] = "OR";
               }
               String[] decomposedEl = FieldLookup.decompose(fields[i]);
               if (decomposedEl.length > 0) {
                  Field associationField = JediEngine.getField(decomposedEl[0], entity);
                  if (JediEngine.isOneToOneField(associationField) || JediEngine.isForeignKeyField(associationField)) {
                     fields[i] = fields[i].replace(decomposedEl[0], decomposedEl[0] + "_id");
                  }
               }
               // Creates the column name.
               if (fields[i].contains("=")) {
                  fields[i] = String.format(
                        "%s%s",
                        TableUtil.getColumnName(fields[i].substring(0, fields[i].lastIndexOf("="))),
                        fields[i].substring(fields[i].lastIndexOf("=")));
               }
               // Adds a blank space between the field name and value.
               fields[i] = fields[i].replace("=", " = ");
               // Replaces % by \%
               fields[i] = fields[i].replace("%", "\\%");
               // Adds a blank space between the values separated by comma
               // character.
               fields[i] = fields[i].replace(",", ", ");
               // Checks if the current pair contains __startswith, __contains
               // ou __endswith.
               if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__!startswith") > -1 ||
                     fields[i].indexOf("__istartswith") > -1 || fields[i].indexOf("__!istartswith") > -1 ||
                     fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__!contains") > -1 || fields[i].indexOf("__icontains") > -1 ||
                     fields[i].indexOf("__!icontains") > -1 || fields[i].indexOf("__endswith") > -1 || fields[i].indexOf("__!endswith") > -1 ||
                     fields[i].indexOf("__iendswith") > -1 || fields[i].indexOf("__!iendswith") > -1) {
                  // Creates a LIKE SQL statement.
                  if (fields[i].indexOf("__startswith") > -1) {
                     fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!startswith") > -1) {
                     fields[i] = fields[i].replace("__!startswith = ", " NOT LIKE ");
                     // Replaces 'value' by 'value%'.
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!istartswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__contains") > -1) {
                     fields[i] = fields[i].replace("__contains = ", " LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__!contains") > -1) {
                     fields[i] = fields[i].replace("__!contains = ", " NOT LIKE ");
                     // Replaces 'value' by '%value%'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\'";
                  } else if (fields[i].indexOf("__icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__!icontains") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                     fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                     fields[i] = fields[i] + "%\')";
                  } else if (fields[i].indexOf("__endswith") > -1) {
                     fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!endswith") > -1) {
                     fields[i] = fields[i].replace("__!endswith = ", " NOT LIKE ");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else if (fields[i].indexOf("__!iendswith") > -1) {
                     fields[i] = fields[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                     // Replaces 'value' by '%value'.
                     fields[i] = fields[i].replaceFirst("\'", "\'%");
                  } else {
                     
                  }
               }
               if (fields[i].indexOf("__in") > -1) {
                  // Creates a IN SQL statement.
                  fields[i] = fields[i].replace("__in = ", " IN ");
                  // Replaces [] characters by () characters.
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__!in") > -1) {
                  // Creates a IN SQL statement.
                  fields[i] = fields[i].replace("__!in = ", " NOT IN ");
                  // Replaces [] characters by () characters.
                  fields[i] = fields[i].replace("[", "(");
                  fields[i] = fields[i].replace("]", ")");
               }
               if (fields[i].indexOf("__range") > -1) {
                  // Creates a BETWEEN SQL statement.
                  fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                  // Removes the [ character.
                  fields[i] = fields[i].replace("[", "");
                  // Removes the ] character.
                  fields[i] = fields[i].replace("]", "");
                  // Substituindo o caracter , por AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__!range") > -1) {
                  // Creates a BETWEEN SQL statement.
                  fields[i] = fields[i].replace("__!range = ", " NOT BETWEEN ");
                  // Removes the [ character.
                  fields[i] = fields[i].replace("[", "");
                  // Removes the ] character.
                  fields[i] = fields[i].replace("]", "");
                  // Substituindo o caracter , por AND.
                  fields[i] = fields[i].replace(", ", " AND ");
               }
               if (fields[i].indexOf("__regex") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__regex ?= ?(.*)$", " $1 REGEXP BINARY '$2'");
               }
               if (fields[i].indexOf("__iregex") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__iregex ?= ?(.*)$", " $1 REGEXP '$2'");
               }
               if (fields[i].matches(".*__year__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__lt *= *(\\d{4})$", " $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?< ?(\\d{4})$", " $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!lt *= *(\\d{4})$", " NOT $1 < '$2-01-01'");
               }
               if (fields[i].matches(".*__year__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__lte *= *(\\d{4})$", " $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?<= ?(\\d{4})$", " $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!lte *= *(\\d{4})$", " NOT $1 <= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__gt *= *(\\d{4})$", " $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?> ?(\\d{4})$", " $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!gt *= *(\\d{4})$", " NOT $1 > '$2-01-01'");
               }
               if (fields[i].matches(".*__year__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__gte *= *(\\d{4})$", " $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?>= ?(\\d{4})$", " $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!gte *= *(\\d{4})$", " NOT $1 >= '$2-01-01'");
               }
               if (fields[i].matches(".*__year__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__exact *= *(\\d{4})$", " $1 = '$2-01-01'");
               }
               if (fields[i].matches(".*__year__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year__!exact *= *(\\d{4})$", " NOT $1 = '$2-01-01'");
               }
               if (fields[i].matches(".*__year *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year *= *(\\d{4})$", " $1 BETWEEN '$2-01-01' AND '$2-12-31'");
               }
               if (fields[i].matches(".*__!year *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!year *= *(\\d{4})$", " $1 NOT BETWEEN '$2-01-01' AND '$2-12-31'");
               }
               if (fields[i].matches(".*__year ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?!= ?(\\d{4})$", " $1 != '$2-01-01'");
               }
               if (fields[i].matches(".*__year ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__year ?<> ?(\\d{4})$", " $1 <> '$2-01-01'");
               }
               if (fields[i].matches(".*__month__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__lt *= *(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?< ?(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!lt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__month__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__lte *= *(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?<= ?(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!lte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__month__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__gt *= *(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?> ?(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!gt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__month__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__gte *= *(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?>= ?(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!gte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__month__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__exact *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month__!exact *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!month *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!month *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__month ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__month ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__month ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__day__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__lt *= *(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?< ?(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!lt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__day__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__lte *= *(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?<= ?(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!lte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__day__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__gt *= *(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?> ?(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!gt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__day__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__gte *= *(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?>= ?(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!gte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__day__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__exact *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day__!exact *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!day *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__day ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__day ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__day ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__week_day__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__lt *= *(\\d{2})$", " WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?< ?(\\d{2})$", " WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!lt *= *(\\d{2})$", " NOT WEEKDAY($1) < '$2'");
               }
               if (fields[i].matches(".*__week_day__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__lte *= *(\\d{2})$", " WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?<= ?(\\d{2})$", " WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!lte *= *(\\d{2})$", " NOT WEEKDAY($1) <= '$2'");
               }
               if (fields[i].matches(".*__week_day__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__gt *= *(\\d{2})$", " WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?> ?(\\d{2})$", " WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!gt *= *(\\d{2})$", " NOT WEEKDAY($1) > '$2'");
               }
               if (fields[i].matches(".*__week_day__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__gte *= *(\\d{2})$", " WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?>= ?(\\d{2})$", " WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__!gte *= *(\\d{2})$", " NOT WEEKDAY($1) >= '$2'");
               }
               if (fields[i].matches(".*__week_day__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__!week_day *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!week_day *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
               }
               if (fields[i].matches(".*__week_day ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?!= ?(\\d{2})$", " WEEKDAY($1) != '$2'");
               }
               if (fields[i].matches(".*__week_day ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__week_day ?<> ?(\\d{2})$", " WEEKDAY($1) <> '$2'");
               }
               if (fields[i].matches(".*__hour__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__lt *= *(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?< ?(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!lt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__hour__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__lte *= *(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?<= ?(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!lte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__hour__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__gt *= *(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?> ?(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!gt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__hour__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__gte *= *(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?>= ?(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!gte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__hour__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__exact *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour__!exact *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!hour *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!hour *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__hour ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?!= ?(\\d{2})$", " EXTRACT(hour FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__hour ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__hour ?<> ?(\\d{2})$", " EXTRACT(hour FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__minute__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__lt *= *(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?< ?(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!lt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__minute__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__lte *= *(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?<= ?(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!lte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__minute__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__gt *= *(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?> ?(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!gt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__minute__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__gte *= *(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?>= ?(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!gte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__minute__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__exact *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute__!exact *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!minute *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!minute *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__minute ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?!= ?(\\d{2})$", " EXTRACT(minute FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__minute ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__minute ?<> ?(\\d{2})$", " EXTRACT(minute FROM $1) <> '$2'");
               }
               if (fields[i].matches(".*__second__lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__lt *= *(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second ?< ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?< ?(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second__!lt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!lt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) < '$2'");
               }
               if (fields[i].matches(".*__second__lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__lte *= *(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second ?<= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?<= ?(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second__!lte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!lte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) <= '$2'");
               }
               if (fields[i].matches(".*__second__gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__gt *= *(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second ?> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?> ?(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second__!gt.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!gt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) > '$2'");
               }
               if (fields[i].matches(".*__second__gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__gte *= *(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second ?>= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?>= ?(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second__!gte.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!gte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) >= '$2'");
               }
               if (fields[i].matches(".*__second__exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__exact *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second__!exact.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second__!exact *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__!second *= *.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__!second *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
               }
               if (fields[i].matches(".*__second ?!= ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?!= ?(\\d{2})$", " EXTRACT(second FROM $1) != '$2'");
               }
               if (fields[i].matches(".*__second ?<> ?.*")) {
                  fields[i] = fields[i].replaceAll("^(.*)__second ?<> ?(\\d{2})$", " EXTRACT(second FROM $1) <> '$2'");
               }
               if (fields[i].indexOf("__lt") > -1) {
                  fields[i] = fields[i].replace("__lt = ", " < ");
               }
               if (fields[i].indexOf("__!lt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
               }
               if (fields[i].indexOf("__lte") > -1) {
                  fields[i] = fields[i].replace("__lte = ", " <= ");
               }
               if (fields[i].indexOf("__!lte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
               }
               if (fields[i].indexOf("__gt") > -1) {
                  fields[i] = fields[i].replace("__gt = ", " > ");
               }
               if (fields[i].indexOf("__!gt") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
               }
               if (fields[i].indexOf("__gte") > -1) {
                  fields[i] = fields[i].replace("__gte = ", " >= ");
               }
               if (fields[i].indexOf("__!gte") > -1) {
                  fields[i] = fields[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
               }
               if (fields[i].indexOf("__exact") > -1) {
                  fields[i] = fields[i].replace("__exact = ", " = ");
               }
               if (fields[i].indexOf("__!exact") > -1) {
                  fields[i] = fields[i].replace("__!exact = ", " != ");
               }
               if (fields[i].indexOf("__isnull") > -1) {
                  String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length()).trim();
                  if (bool.equalsIgnoreCase("true")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
                  }
                  if (bool.equalsIgnoreCase("false")) {
                     fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
                  }
                  fields[i] = fields[i].replace(bool, "");
               }
               where += fields[i] + " AND ";
               where = where.replace(" AND OR AND", " OR");
               where = where.replace(" AND AND AND", " AND");
            }
            where = where.substring(0, where.lastIndexOf("AND"));
            if (joins.isEmpty()) {
               sql = String.format("%s NOT (%s)", sql, where.trim());
            } else {
               sql = String.format(format, tableName, entityName, joins, where.isEmpty() ? "" : "WHERE\n\tNOT (" + where.trim() + ")");
               sql = sql.replaceAll("\t", "    ");
            }
            String _sql = likeSqlDateTime(sql);
            if (!_sql.isEmpty()) {
               sql = _sql;
            }
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            connect();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
               return querySet;
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               Object obj = entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  if (connection.toString().startsWith("oracle")) {
                     id.set(
                           obj,
                           ((java.math.BigDecimal) resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)))
                                 .intValue());
                  } else {
                     id.set(obj, resultSet.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1)));
                  }
               }
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field field : _fields) {
                  field.setAccessible(true);
                  if (!JediEngine.isJediField(field)) {
                     continue;
                  }
                  if (field.getName().equals("serialVersionUID")) {
                     continue;
                  }
                  if (field.getName().equals("objects")) {
                     continue;
                  }
                  oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class superClazz = null;
                        Class clazz = null;
                        String model = manyToManyFieldAnnotation.model().getSimpleName();
                        model = Model.class.getSimpleName().equals(model) ? "" : model;
                        if (model.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 model = clazz.getSimpleName();
                              }
                           }
                        }
                        String references = manyToManyFieldAnnotation.references();
                        if (references == null || references.trim().isEmpty()) {
                           if (clazz != null) {
                              references = TableUtil.getTableName(clazz);
                           } else {
                              references = TableUtil.getTableName(model);
                           }
                        }
                        String packageName = this.entity.getPackage().getName();
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                        manager = new Manager(associatedModelClass);
                        QuerySet querySetAssociatedModels = manager.raw(
                              String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    references,
                                    model,
                                    tableName,
                                    references,
                                    TableUtil.getColumnName(obj.getClass()),
                                    ((Model) obj).getId()),
                              associatedModelClass);
                        field.set(obj, querySetAssociatedModels);
                     } else {
                        field.set(obj, null);
                     }
                  } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                     if (oneToOneFieldAnnotation != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     }
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(field.getType().getName());
                        manager = new Manager(associatedModelClass);
                        Model associatedModel = manager
                              .get(String.format("id"), resultSet.getObject(String.format("%s_id", TableUtil.getColumnName(field))));
                        field.set(obj, associatedModel);
                     } else {
                        field.set(obj, null);
                     }
                  } else {
                     if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                           connection.toString().startsWith("oracle")) {
                        if (resultSet.getObject(TableUtil.getColumnName(field)) == null) {
                           field.set(obj, 0);
                        } else {
                           String columnName = TableUtil.getColumnName(field);
                           BigDecimal bigDecimal = (BigDecimal) resultSet.getObject(columnName);
                           field.set(obj, bigDecimal.intValue());
                        }
                     } else {
                        String columnName = TableUtil.getColumnName(field);
                        Object columnValue = resultSet.getObject(columnName);
                        if (columnValue instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           columnValue = calendar.getTime();
                        }
                        if (columnValue instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) columnValue;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           columnValue = calendar.getTime();
                        }
                        field.set(obj, columnValue);
                     }
                  }
               }
               T model = (T) obj;
               if (model != null) {
                  model.setPersisted(true);
               }
               querySet.add(model);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            close(statement, resultSet, connection);
         }
      }
      return querySet;
   }
   
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder,
         String... filters) {
      Integer _pageStart = pageStart == null ? 0 : pageStart.get();
      Integer _pageSize = pageSize == null ? 0 : pageSize.get();
      boolean reverseOrder = SortOrder.isDescending(pageOrder);
      String orderBy = pageOrder == null ? QueryPageOrder.get(QueryPage.orderBy("id")) : QueryPageOrder.get(pageOrder);
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity((Class<T>) this.entity);
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      String sql = String.format("SELECT * FROM %s", tableName);
      String where = "";
      String joins = "";
      String join = "";
      if (filters != null && filters.length > 0) {
         sql = String.format("SELECT * FROM %s WHERE", tableName);
         String fieldName = "";
         String fieldValue = "";
         // Iterates through the pairs field=value passed.
         for (int i = 0; i < filters.length; i++) {
            filters[i] = filters[i] == null ? "" : filters[i].trim();
            if (filters[i].isEmpty()) {
               continue;
            }
            join = FieldLookup.translateJoin(entityName + "." + filters[i]);
            if (!joins.contains(join)) {
               joins += join;
               filters[i] = filters[i].replace(filters[i].replaceAll("\\w+\\.\\w+__.*", ""), "");
            }
            if (filters[i].equalsIgnoreCase("AND")) {
               filters[i] = "AND";
            }
            if (filters[i].equalsIgnoreCase("OR")) {
               filters[i] = "OR";
            }
            String[] decomposedEl = FieldLookup.decompose(filters[i]);
            if (decomposedEl.length > 0) {
               Field associationField = JediEngine.getField(decomposedEl[0], entity);
               if (JediEngine.isOneToOneField(associationField) || JediEngine.isForeignKeyField(associationField)) {
                  filters[i] = filters[i].replace(decomposedEl[0], decomposedEl[0] + "_id");
               }
            }
            // Changes the name of the field to the corresponding pattern
            // name on the database.
            if (filters[i].contains("=")) {
               fieldName = filters[i].substring(0, filters[i].lastIndexOf("="));
               fieldName = TableUtil.getColumnName(fieldName);
               fieldValue = filters[i].substring(filters[i].lastIndexOf("="));
               filters[i] = String.format("%s%s", fieldName, fieldValue);
            }
            // Adds a blank space between the field name and value.
            filters[i] = filters[i].replace("=", " = ");
            // Replaces % by \%
            filters[i] = filters[i].replace("%", "\\%");
            // Adds a blank space between the values separated by commas.
            filters[i] = filters[i].replace(",", ", ");
            // Checks if the current pair contains __startswith, __contains
            // or __endswith.
            if (filters[i].indexOf("__startswith") > -1 || filters[i].indexOf("__!startswith") > -1 ||
                  filters[i].indexOf("__istartswith") > -1 || filters[i].indexOf("__!istartswith") > -1 ||
                  filters[i].indexOf("__contains") > -1 || filters[i].indexOf("__!contains") > -1 || filters[i].indexOf("__icontains") > -1 ||
                  filters[i].indexOf("__!icontains") > -1 || filters[i].indexOf("__endswith") > -1 || filters[i].indexOf("__!endswith") > -1 ||
                  filters[i].indexOf("__iendswith") > -1 || filters[i].indexOf("__!iendswith") > -1) {
               // Creates the LIKE SQL statement.
               if (filters[i].indexOf("__startswith") > -1) {
                  filters[i] = filters[i].replace("__startswith = ", " LIKE ");
                  // Replaces 'value' by 'value%'.
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\'";
               } else if (filters[i].indexOf("__!startswith") > -1) {
                  filters[i] = filters[i].replace("__!startswith = ", " NOT LIKE ");
                  // Replaces 'value' by 'value%'.
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\'";
               } else if (filters[i].indexOf("__istartswith") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__istartswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\')";
               } else if (filters[i].indexOf("__!istartswith") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__!istartswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\')";
               } else if (filters[i].indexOf("__contains") > -1) {
                  filters[i] = filters[i].replace("__contains = ", " LIKE ");
                  // Replaces 'value' by '%value%'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\'";
               } else if (filters[i].indexOf("__!contains") > -1) {
                  filters[i] = filters[i].replace("__!contains = ", " NOT LIKE ");
                  // Replaces 'value' by '%value%'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\'";
               } else if (filters[i].indexOf("__icontains") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__icontains *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\')";
               } else if (filters[i].indexOf("__!icontains") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__!icontains *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
                  filters[i] = filters[i].substring(0, filters[i].lastIndexOf("\'"));
                  filters[i] = filters[i] + "%\')";
               } else if (filters[i].indexOf("__endswith") > -1) {
                  filters[i] = filters[i].replace("__endswith = ", " LIKE ");
                  // Replaces 'value' by '%value'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
               } else if (filters[i].indexOf("__!endswith") > -1) {
                  filters[i] = filters[i].replace("__!endswith = ", " NOT LIKE ");
                  // Replaces 'value' by '%value'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
               } else if (filters[i].indexOf("__iendswith") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__iendswith *= *(.*)$", "UPPER($1) LIKE UPPER($2)");
                  // Replaces 'value' by '%value'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
               } else if (filters[i].indexOf("__!iendswith") > -1) {
                  filters[i] = filters[i].replaceAll("^(.*)__!iendswith *= *(.*)$", "UPPER($1) NOT LIKE UPPER($2)");
                  // Replaces 'value' by '%value'.
                  filters[i] = filters[i].replaceFirst("\'", "\'%");
               } else {
                  
               }
            }
            if (filters[i].indexOf("__in") > -1) {
               // Creates a IN statement in SQL.
               filters[i] = filters[i].replace("__in = ", " IN ");
               // Replaces [] by ()
               filters[i] = filters[i].replace("[", "(");
               filters[i] = filters[i].replace("]", ")");
            }
            if (filters[i].indexOf("__!in") > -1) {
               // Creates a IN statement in SQL.
               filters[i] = filters[i].replace("__!in = ", " NOT IN ");
               // Replaces [] by ()
               filters[i] = filters[i].replace("[", "(");
               filters[i] = filters[i].replace("]", ")");
            }
            if (filters[i].indexOf("__range") > -1) {
               // Creates a BETWEEN statement in SQL.
               filters[i] = filters[i].replace("__range = ", " BETWEEN ");
               // Removes [ or ] characters.
               filters[i] = filters[i].replace("[", "");
               filters[i] = filters[i].replace("]", "");
               // Replaces , (comma character) by AND.
               filters[i] = filters[i].replace(", ", " AND ");
            }
            if (filters[i].indexOf("__!range") > -1) {
               // Creates a BETWEEN statement in SQL.
               filters[i] = filters[i].replace("__!range = ", " NOT BETWEEN ");
               // Removes [ or ] characters.
               filters[i] = filters[i].replace("[", "");
               filters[i] = filters[i].replace("]", "");
               // Replaces , (comma character) by AND.
               filters[i] = filters[i].replace(", ", " AND ");
            }
            if (filters[i].indexOf("__regex") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__regex ?= ?(.*)$", " $1 REGEXP BINARY '$2'");
            }
            if (filters[i].indexOf("__iregex") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__iregex ?= ?(.*)$", " $1 REGEXP '$2'");
            }
            if (filters[i].matches(".*__year__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__lt *= *(\\d{4})$", " $1 < '$2-01-01'");
            }
            if (filters[i].matches(".*__year ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?< ?(\\d{4})$", " $1 < '$2-01-01'");
            }
            if (filters[i].matches(".*__year__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__!lt *= *(\\d{4})$", " NOT $1 < '$2-01-01'");
            }
            if (filters[i].matches(".*__year__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__lte *= *(\\d{4})$", " $1 <= '$2-01-01'");
            }
            if (filters[i].matches(".*__year ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?<= ?(\\d{4})$", " $1 <= '$2-01-01'");
            }
            if (filters[i].matches(".*__year__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__!lte *= *(\\d{4})$", " NOT $1 <= '$2-01-01'");
            }
            if (filters[i].matches(".*__year__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__gt *= *(\\d{4})$", " $1 > '$2-01-01'");
            }
            if (filters[i].matches(".*__year ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?> ?(\\d{4})$", " $1 > '$2-01-01'");
            }
            if (filters[i].matches(".*__year__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__!gt *= *(\\d{4})$", " NOT $1 > '$2-01-01'");
            }
            if (filters[i].matches(".*__year__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__gte *= *(\\d{4})$", " $1 >= '$2-01-01'");
            }
            if (filters[i].matches(".*__year ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?>= ?(\\d{4})$", " $1 >= '$2-01-01'");
            }
            if (filters[i].matches(".*__year__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__!gte *= *(\\d{4})$", " NOT $1 >= '$2-01-01'");
            }
            if (filters[i].matches(".*__year__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__exact *= *(\\d{4})$", " $1 = '$2-01-01'");
            }
            if (filters[i].matches(".*__year__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year__!exact *= *(\\d{4})$", " NOT $1 = '$2-01-01'");
            }
            if (filters[i].matches(".*__year *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year *= *(\\d{4})$", " $1 BETWEEN '$2-01-01' AND '$2-12-31'");
            }
            if (filters[i].matches(".*__!year *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!year *= *(\\d{4})$", " $1 NOT BETWEEN '$2-01-01' AND '$2-12-31'");
            }
            if (filters[i].matches(".*__year ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?!= ?(\\d{4})$", " $1 != '$2-01-01'");
            }
            if (filters[i].matches(".*__year ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__year ?<> ?(\\d{4})$", " $1 <> '$2-01-01'");
            }
            if (filters[i].matches(".*__month__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__lt *= *(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__month ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?< ?(\\d{2})$", " EXTRACT(month FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__month__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__!lt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__month__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__lte *= *(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__month ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?<= ?(\\d{2})$", " EXTRACT(month FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__month__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__!lte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__month__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__gt *= *(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__month ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?> ?(\\d{2})$", " EXTRACT(month FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__month__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__!gt *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__month__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__gte *= *(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__month ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?>= ?(\\d{2})$", " EXTRACT(month FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__month__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__!gte *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__month__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__exact *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__month__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month__!exact *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__month *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month *= *(\\d{2})$", " EXTRACT(month FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__!month *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!month *= *(\\d{2})$", " NOT EXTRACT(month FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__month ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
            }
            if (filters[i].matches(".*__month ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__month ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
            }
            if (filters[i].matches(".*__day__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__lt *= *(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__day ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?< ?(\\d{2})$", " EXTRACT(day FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__day__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__!lt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__day__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__lte *= *(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__day ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?<= ?(\\d{2})$", " EXTRACT(day FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__day__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__!lte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__day__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__gt *= *(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__day ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?> ?(\\d{2})$", " EXTRACT(day FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__day__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__!gt *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__day__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__gte *= *(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__day ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?>= ?(\\d{2})$", " EXTRACT(day FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__day__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__!gte *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__day__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__exact *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__day__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day__!exact *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__day *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day *= *(\\d{2})$", " EXTRACT(day FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__!day *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!day *= *(\\d{2})$", " NOT EXTRACT(day FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__day ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?!= ?(\\d{2})$", " EXTRACT(month FROM $1) != '$2'");
            }
            if (filters[i].matches(".*__day ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__day ?<> ?(\\d{2})$", " EXTRACT(month FROM $1) <> '$2'");
            }
            if (filters[i].matches(".*__week_day__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__lt *= *(\\d{2})$", " WEEKDAY($1) < '$2'");
            }
            if (filters[i].matches(".*__week_day ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?< ?(\\d{2})$", " WEEKDAY($1) < '$2'");
            }
            if (filters[i].matches(".*__week_day__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__!lt *= *(\\d{2})$", " NOT WEEKDAY($1) < '$2'");
            }
            if (filters[i].matches(".*__week_day__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__lte *= *(\\d{2})$", " WEEKDAY($1) <= '$2'");
            }
            if (filters[i].matches(".*__week_day ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?<= ?(\\d{2})$", " WEEKDAY($1) <= '$2'");
            }
            if (filters[i].matches(".*__week_day__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__!lte *= *(\\d{2})$", " NOT WEEKDAY($1) <= '$2'");
            }
            if (filters[i].matches(".*__week_day__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__gt *= *(\\d{2})$", " WEEKDAY($1) > '$2'");
            }
            if (filters[i].matches(".*__week_day ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?> ?(\\d{2})$", " WEEKDAY($1) > '$2'");
            }
            if (filters[i].matches(".*__week_day__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__!gt *= *(\\d{2})$", " NOT WEEKDAY($1) > '$2'");
            }
            if (filters[i].matches(".*__week_day__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__gte *= *(\\d{2})$", " WEEKDAY($1) >= '$2'");
            }
            if (filters[i].matches(".*__week_day ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?>= ?(\\d{2})$", " WEEKDAY($1) >= '$2'");
            }
            if (filters[i].matches(".*__week_day__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__!gte *= *(\\d{2})$", " NOT WEEKDAY($1) >= '$2'");
            }
            if (filters[i].matches(".*__week_day__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
            }
            if (filters[i].matches(".*__week_day__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day__exact *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
            }
            if (filters[i].matches(".*__week_day *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day *= *(\\d{2})$", " WEEKDAY($1) = '$2'");
            }
            if (filters[i].matches(".*__!week_day *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!week_day *= *(\\d{2})$", " NOT WEEKDAY($1) = '$2'");
            }
            if (filters[i].matches(".*__week_day ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?!= ?(\\d{2})$", " WEEKDAY($1) != '$2'");
            }
            if (filters[i].matches(".*__week_day ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__week_day ?<> ?(\\d{2})$", " WEEKDAY($1) <> '$2'");
            }
            if (filters[i].matches(".*__hour__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__lt *= *(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__hour ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?< ?(\\d{2})$", " EXTRACT(hour FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__hour__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__!lt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__hour__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__lte *= *(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__hour ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?<= ?(\\d{2})$", " EXTRACT(hour FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__hour__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__!lte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__hour__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__gt *= *(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__hour ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?> ?(\\d{2})$", " EXTRACT(hour FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__hour__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__!gt *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__hour__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__gte *= *(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__hour ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?>= ?(\\d{2})$", " EXTRACT(hour FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__hour__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__!gte *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__hour__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__exact *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__hour__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour__!exact *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__hour *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour *= *(\\d{2})$", " EXTRACT(hour FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__!hour *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!hour *= *(\\d{2})$", " NOT EXTRACT(hour FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__hour ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?!= ?(\\d{2})$", " EXTRACT(hour FROM $1) != '$2'");
            }
            if (filters[i].matches(".*__hour ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__hour ?<> ?(\\d{2})$", " EXTRACT(hour FROM $1) <> '$2'");
            }
            if (filters[i].matches(".*__minute__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__lt *= *(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__minute ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?< ?(\\d{2})$", " EXTRACT(minute FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__minute__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__!lt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__minute__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__lte *= *(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__minute ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?<= ?(\\d{2})$", " EXTRACT(minute FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__minute__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__!lte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__minute__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__gt *= *(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__minute ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?> ?(\\d{2})$", " EXTRACT(minute FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__minute__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__!gt *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__minute__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__gte *= *(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__minute ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?>= ?(\\d{2})$", " EXTRACT(minute FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__minute__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__!gte *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__minute__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__exact *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__minute__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute__!exact *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__minute *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute *= *(\\d{2})$", " EXTRACT(minute FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__!minute *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!minute *= *(\\d{2})$", " NOT EXTRACT(minute FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__minute ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?!= ?(\\d{2})$", " EXTRACT(minute FROM $1) != '$2'");
            }
            if (filters[i].matches(".*__minute ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__minute ?<> ?(\\d{2})$", " EXTRACT(minute FROM $1) <> '$2'");
            }
            if (filters[i].matches(".*__second__lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__lt *= *(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__second ?< ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?< ?(\\d{2})$", " EXTRACT(second FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__second__!lt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__!lt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) < '$2'");
            }
            if (filters[i].matches(".*__second__lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__lte *= *(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__second ?<= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?<= ?(\\d{2})$", " EXTRACT(second FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__second__!lte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__!lte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) <= '$2'");
            }
            if (filters[i].matches(".*__second__gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__gt *= *(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__second ?> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?> ?(\\d{2})$", " EXTRACT(second FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__second__!gt.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__!gt *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) > '$2'");
            }
            if (filters[i].matches(".*__second__gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__gte *= *(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__second ?>= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?>= ?(\\d{2})$", " EXTRACT(second FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__second__!gte.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__!gte *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) >= '$2'");
            }
            if (filters[i].matches(".*__second__exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__exact *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__second__!exact.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second__!exact *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__second *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second *= *(\\d{2})$", " EXTRACT(second FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__!second *= *.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__!second *= *(\\d{2})$", " NOT EXTRACT(second FROM $1) = '$2'");
            }
            if (filters[i].matches(".*__second ?!= ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?!= ?(\\d{2})$", " EXTRACT(second FROM $1) != '$2'");
            }
            if (filters[i].matches(".*__second ?<> ?.*")) {
               filters[i] = filters[i].replaceAll("^(.*)__second ?<> ?(\\d{2})$", " EXTRACT(second FROM $1) <> '$2'");
            }
            if (filters[i].indexOf("__lt") > -1) {
               filters[i] = filters[i].replace("__lt = ", " < ");
            }
            if (filters[i].indexOf("__!lt") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__!lt *= *(.*)$", "NOT $1 < $2");
            }
            if (filters[i].indexOf("__lte") > -1) {
               filters[i] = filters[i].replace("__lte = ", " <= ");
            }
            if (filters[i].indexOf("__!lte") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__!lte *= *(.*)$", "NOT $1 <= $2");
            }
            if (filters[i].indexOf("__gt") > -1) {
               filters[i] = filters[i].replace("__gt = ", " > ");
            }
            if (filters[i].indexOf("__!gt") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__!gt *= *(.*)$", "NOT $1 > $2");
            }
            if (filters[i].indexOf("__gte") > -1) {
               filters[i] = filters[i].replace("__gte = ", " >= ");
            }
            if (filters[i].indexOf("__!gte") > -1) {
               filters[i] = filters[i].replaceAll("^(.*)__!gte *= *(.*)$", "NOT $1 >= $2");
            }
            if (filters[i].indexOf("__exact") > -1) {
               filters[i] = filters[i].replace("__exact = ", " = ");
            }
            if (filters[i].indexOf("__!exact") > -1) {
               filters[i] = filters[i].replace("__!exact = ", " != ");
            }
            if (filters[i].indexOf("__isnull") > -1) {
               String bool = filters[i].substring(filters[i].indexOf("=") + 1, filters[i].length()).trim();
               if (bool.equalsIgnoreCase("true")) {
                  filters[i] = filters[i].replace("__isnull = ", " IS NULL ");
               }
               if (bool.equalsIgnoreCase("false")) {
                  filters[i] = filters[i].replace("__isnull = ", " IS NOT NULL ");
               }
               filters[i] = filters[i].replace(bool, "");
            }
            where += filters[i] + " AND ";
            where = where.replace(" AND OR AND", " OR");
            where = where.replace(" AND AND AND", " AND");
         }
      }
      try {
         if (where.lastIndexOf("AND") > -1) {
            where = where.substring(0, where.lastIndexOf("AND"));
         }
         where = where.trim();
         if (joins.isEmpty()) {
            sql = String.format("%s %s %s LIMIT %d OFFSET %d", sql, where, orderBy, _pageSize, _pageStart);
         } else {
            sql = String.format("SELECT\n\t*\nFROM\n\t%s AS %s", tableName, entityName);
            sql += String.format("\n%s", joins);
            sql += String.format("WHERE\n\t%s", where);
            orderBy = reverseOrder == false
                  ? String.format("\nORDER BY\n\t%s.id ASC", entityName) : String.format("\nORDER BY\n\t%s.id DESC", entityName);
            sql += orderBy;
            sql += String.format("\nLIMIT %d", _pageSize);
            sql += String.format("\nOFFSET %d", _pageStart);
            sql = sql.replaceAll("\t", "    ");
         }
         String _sql = likeSqlDateTime(sql);
         if (!_sql.isEmpty()) {
            sql = _sql;
         }
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         resultSet = statement.executeQuery();
         if (!resultSet.next()) {
            return querySet;
         }
         resultSet.beforeFirst();
         while (resultSet.next()) {
            Object obj = entity.newInstance();
            if (resultSet.getObject("id") != null) {
               Field id = jedi.db.models.Model.class.getDeclaredField("id");
               id.setAccessible(true);
               if (connection.toString().startsWith("oracle")) {
                  id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
               } else {
                  id.set(obj, resultSet.getObject(id.getName()));
               }
            }
            List<Field> _fields = JediEngine.getAllFields(this.entity);
            for (Field field : _fields) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.getName().equals("serialVersionUID"))
                  continue;
               if (field.getName().equalsIgnoreCase("objects"))
                  continue;
               oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
               foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
               manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
               FetchType fetchType = JediEngine.FETCH_TYPE;
               Manager manager = null;
               if (manyToManyFieldAnnotation != null) {
                  fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                  if (fetchType.equals(FetchType.EAGER)) {
                     Class superClazz = null;
                     Class clazz = null;
                     String packageName = this.entity.getPackage().getName();
                     String model = manyToManyFieldAnnotation.model().getSimpleName();
                     model = Model.class.getSimpleName().equals(model) ? "" : model;
                     if (model.isEmpty()) {
                        ParameterizedType genericType = null;
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           genericType = (ParameterizedType) field.getGenericType();
                           superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                           if (superClazz == Model.class) {
                              clazz = (Class) genericType.getActualTypeArguments()[0];
                              model = clazz.getSimpleName();
                           }
                        }
                     }
                     String references = manyToManyFieldAnnotation.references();
                     if (references == null || references.trim().isEmpty()) {
                        if (clazz != null) {
                           references = TableUtil.getTableName(clazz);
                        } else {
                           references = TableUtil.getTableName(model);
                        }
                     }
                     Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, model));
                     manager = new Manager(associatedModelClass);
                     List<List<Map<String, Object>>> recordSet = null;
                     recordSet = manager.raw(
                           String.format(
                                 "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                                 TableUtil.getColumnName(model),
                                 tableName,
                                 TableUtil.getTableName(references),
                                 TableUtil.getColumnName(this.entity),
                                 ((Model) obj).id()));
                     String args = recordSet.toString();
                     args = args.replace("[", "");
                     args = args.replace("{", "");
                     args = args.replace("]", "");
                     args = args.replace("}", "");
                     args = args.replace("=", "");
                     args = args.replace(", ", ",");
                     args = args.replace(String.format("%s_id", TableUtil.getColumnName(model)), "");
                     args = String.format("id__in=[%s]", args);
                     QuerySet querySetAssociatedModels = manager._filter(args);
                     field.set(obj, querySetAssociatedModels);
                  } else {
                     field.set(obj, null);
                  }
               } else if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
                  if (oneToOneFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                  } else {
                     fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                  }
                  if (fetchType.equals(FetchType.EAGER)) {
                     Class associatedModelClass = Class.forName(field.getType().getName());
                     manager = new Manager(associatedModelClass);
                     String columnName = TableUtil.getColumnName(field);
                     Object id = resultSet.getObject(String.format("%s_id", columnName));
                     Model associatedModel = manager.get("id", id);
                     field.set(obj, associatedModel);
                  } else {
                     field.set(obj, null);
                  }
               } else {
                  if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) &&
                        connection.toString().startsWith("oracle")) {
                     if (resultSet.getObject(TableUtil.getColumnName(field.getName())) == null) {
                        field.set(obj, 0);
                     } else {
                        String columnName = TableUtil.getColumnName(field.getName());
                        BigDecimal columnValue = (BigDecimal) resultSet.getObject(columnName);
                        field.set(obj, columnValue.intValue());
                     }
                  } else {
                     String columnName = TableUtil.getColumnName(field.getName());
                     Object columnValue = resultSet.getObject(columnName);
                     if (columnValue instanceof java.sql.Date) {
                        java.sql.Date date = (java.sql.Date) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(date.getTime());
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof java.sql.Time) {
                        java.sql.Time time = (java.sql.Time) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time.getTime());
                        calendar.set(Calendar.YEAR, 0);
                        calendar.set(Calendar.MONTH, 0);
                        calendar.set(Calendar.DAY_OF_MONTH, 0);
                        columnValue = calendar.getTime();
                     }
                     if (columnValue instanceof Timestamp) {
                        Timestamp timestamp = (Timestamp) columnValue;
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp.getTime());
                        columnValue = calendar.getTime();
                     }
                     field.set(obj, columnValue);
                  }
               }
               manager = null;
            }
            T model = (T) obj;
            if (model != null) {
               model.setPersisted(true);
            }
            querySet.add(model);
         }
         if (querySet != null) {
            querySet.setPersisted(true);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         close(resultSet, statement, connection);
      }
      return querySet;
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, String... filters) {
      return page(pageStart, pageSize, QueryPage.orderBy("id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder) {
      return page(pageStart, pageSize, pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize) {
      return page(pageStart, pageSize, QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageOrder pageOrder) {
      return page(pageStart, pageSize(10), pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart, String... filters) {
      return page(pageStart, pageSize(10), QueryPage.orderBy("id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageStart pageStart) {
      return page(pageStart, pageSize(10), QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageSize pageSize) {
      return page(pageStart(1), pageSize, QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageOrder pageOrder) {
      return page(pageStart(1), pageSize(10), pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(String... filters) {
      return page(pageStart(1), pageSize(10), QueryPage.orderBy("id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page() {
      return page(pageStart(1), pageSize(10), QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder,
         String... filters) {
      QuerySet<T> qs = null;
      pageNumber = pageNumber == null ? new QueryPageNumber() : pageNumber;
      pageSize = pageSize == null ? new QueryPageSize() : pageSize;
      pageOrder = pageOrder == null ? QueryPage.orderBy("id") : pageOrder;
      QueryPageStart pageStart = new QueryPageStart();
      // Calcula o deslocamento a partir da fórmula: deslocamento = (nº página -
      // 1) x tamanho da página.
      if (pageNumber.value() > 0) {
         pageStart.value((pageNumber.value() - 1) * pageSize.value());
         qs = page(pageStart, pageSize, pageOrder, filters);
      } else {
         qs = page(pageStart(0), pageSize(0), pageOrder, filters);
      }
      return qs;
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, String... filters) {
      return page(pageNumber, pageSize, QueryPage.orderBy("id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder) {
      return page(pageNumber, pageSize, pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize) {
      return page(pageNumber, pageSize, QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageOrder pageOrder) {
      return page(pageNumber, pageSize(10), pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, String... filters) {
      return page(pageNumber, pageSize(10), QueryPage.orderBy("id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber) {
      return page(pageNumber, pageSize(10), QueryPage.orderBy("id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder,
         String... filters) {
      String field = pageOrder != null ? pageOrder.field() : "";
      if (!field.isEmpty() && !field.startsWith("-")) {
         pageOrder.field("-" + field);
      }
      return page(pageStart, pageSize, pageOrder, filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, String... filters) {
      return reversePage(pageStart, pageSize, QueryPage.orderBy("-id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder) {
      return reversePage(pageStart, pageSize, pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize) {
      return reversePage(pageStart, pageSize, QueryPage.orderBy("-id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageOrder pageOrder) {
      return reversePage(pageStart, pageSize(10), pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, String... filters) {
      return reversePage(pageStart, pageSize(10), QueryPage.orderBy("-id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart) {
      return reversePage(pageStart, pageSize(10), QueryPage.orderBy("-id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageSize pageSize) {
      return reversePage(pageStart(1), pageSize, QueryPage.orderBy("-id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageOrder pageOrder) {
      return reversePage(pageStart(1), pageSize(10), pageOrder, (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(String... filters) {
      return reversePage(pageStart(1), pageSize(10), QueryPage.orderBy("-id"), filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage() {
      return reversePage(pageStart(1), pageSize(10), QueryPage.orderBy("-id"), (String[]) null);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder,
         String... filters) {
      String field = pageOrder != null ? pageOrder.field() : "";
      if (!field.isEmpty() && !field.startsWith("-")) {
         pageOrder.field("-" + field);
      }
      return page(pageNumber, pageSize, pageOrder, filters);
   }
   
   @Override
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, String... filters) {
      return reversePage(pageNumber, pageSize, QueryPage.orderBy("-id"), filters);
   }
   
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder) {
      return reversePage(pageNumber, pageSize, pageOrder, (String[]) null);
   }
   
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize) {
      return reversePage(pageNumber, pageSize, QueryPage.orderBy("-id"), (String[]) null);
   }
   
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageOrder pageOrder) {
      return reversePage(pageNumber, pageSize(10), pageOrder, (String[]) null);
   }
   
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, String... filters) {
      return reversePage(pageNumber, pageSize(10), QueryPage.orderBy("-id"), filters);
   }
   
   public <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber) {
      return reversePage(pageNumber, pageSize(10), QueryPage.orderBy("-id"), (String[]) null);
   }
   
   public String likeDateTime(String sql) {
      sql = sql == null ? "" : sql;
      if (!sql.isEmpty()) {
         // TODO - substituir '//%', '%//' ou '%//%' por ''
         // TODO - OBS: data vazia busca por todos os registros.
         Pattern pattern = Pattern.compile(Regex.LIKE_DATETIME.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
         Matcher matcher = pattern.matcher(sql);
         String regexp = "";
         // System.out.println("SQL (IN): " + sql);
         while (matcher.find()) {
            regexp = matcher.group();
            regexp = regexp.replaceAll("(\\w+)_(\\w+)", "$1@$2");
            // regexp = regexp.replaceAll("[/_]", "");
            regexp = regexp.replaceAll("[//_]", "");
            regexp = regexp.replaceAll("@", "_");
            regexp = regexp.replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%d%m%Y %T') REGEXP$2");
            regexp = regexp.replaceAll(" , ", ", ");
            regexp = regexp.replaceAll("%'", "^'");
            regexp = regexp.replaceAll("'%", "'\\$");
            regexp = regexp.replaceAll("'(\\d*)(\\^)'", "'$2$1'");
            regexp = regexp.replaceAll("'(\\$)(\\d*)'", "'$2$1'");
            regexp = regexp.replace("$d", "%d");
            if (regexp.contains("'$") && regexp.contains("^'")) {
               regexp = regexp.replace("$", "");
               regexp = regexp.replace("^", "");
            }
            // System.out.println("MySQL RegExp: " + regexp);
            sql = sql.replace(matcher.group(), regexp);
         }
         // System.out.println("SQL (OUT): " + sql);
      }
      return sql;
   }
   
   private String likeDate(String sql) {
      Pattern pattern = Pattern.compile(Regex.LIKE_DATE.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(sql);
      String regexp = "";
      // System.out.println("SQL (IN): " + sql);
      while (matcher.find()) {
         // System.out.println("Matcher Group: " + matcher.group());
         regexp = matcher.group().replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%d/%m/%Y') REGEXP$2");
         regexp = regexp.replaceAll(" , ", ", ");
         regexp = regexp.replaceAll("'%", "'.*");
         regexp = regexp.replaceAll("%'", ".*'");
         regexp = regexp.replace("'.*d", "'%d");
         // System.out.println("MySQL RegExp: " + regexp);
         sql = sql.replace(matcher.group(), regexp);
      }
      // System.out.println("SQL (OUT): " + sql);
      return sql;
   }
   
   private String likeTime(String sql) {
      Pattern pattern = Pattern.compile(Regex.LIKE_TIME.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(sql);
      String regexp = "";
      // System.out.println("SQL (IN): " + sql);
      while (matcher.find()) {
         // System.out.println("Matcher Group: " + matcher.group());
         regexp = matcher.group().replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%T') REGEXP$2");
         regexp = regexp.replaceAll(" , ", ", ");
         regexp = regexp.replaceAll("'%", "'.*");
         regexp = regexp.replaceAll("%'", ".*'");
         regexp = regexp.replace("'.*T", "'%T");
         // System.out.println("MySQL RegExp: " + regexp);
         sql = sql.replace(matcher.group(), regexp);
      }
      // System.out.println("SQL (OUT): " + sql);
      return sql;
   }
   
   private String likeSqlDateTime(String sql) {
      sql = sql == null ? "" : sql;
      if (!sql.isEmpty()) {
         Pattern pattern = Pattern.compile(Regex.LIKE_SQL_DATETIME.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
         Matcher matcher = pattern.matcher(sql);
         String regexp = "";
         while (matcher.find()) {
            regexp = matcher.group();
            regexp = regexp.replaceAll("(\\w+)_(\\w+)", "$1@$2");
            regexp = regexp.replaceAll("_", "");
            regexp = regexp.replaceAll("@", "_");
            regexp = regexp.replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%Y-%m-%d %T') REGEXP$2");
            regexp = regexp.replaceAll(" , ", ", ");
            regexp = regexp.replaceAll("%'", "^'");
            regexp = regexp.replaceAll("'%", "'\\$");
            regexp = regexp.replaceAll("'(\\d*)(\\^)'", "'$2$1'");
            regexp = regexp.replaceAll("'(\\$)(\\d*)'", "'$2$1'");
            regexp = regexp.replace("$d", "%d");
            if (regexp.contains("'$") && regexp.contains("^'")) {
               regexp = regexp.replace("$", "");
               regexp = regexp.replace("^", "");
            }
            sql = sql.replace(matcher.group(), regexp);
         }
      }
      return sql;
   }
   
   private String likeSqlDate(String sql) {
      sql = sql == null ? "" : sql;
      if (!sql.isEmpty()) {
         Pattern pattern = Pattern.compile(Regex.LIKE_SQL_DATE.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
         Matcher matcher = pattern.matcher(sql);
         String regexp = "";
         while (matcher.find()) {
            regexp = matcher.group();
            regexp = regexp.replaceAll("(\\w+)_(\\w+)", "$1@$2");
            regexp = regexp.replaceAll("_", "");
            regexp = regexp.replaceAll("@", "_");
            regexp = regexp.replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%Y-%m-%d') REGEXP$2");
            regexp = regexp.replaceAll(" , ", ", ");
            regexp = regexp.replaceAll("%'", "^'");
            regexp = regexp.replaceAll("'%", "'\\$");
            regexp = regexp.replaceAll("'(\\d*)(\\^)'", "'$2$1'");
            regexp = regexp.replaceAll("'(\\$)(\\d*)'", "'$2$1'");
            regexp = regexp.replace("$d", "%d");
            if (regexp.contains("'$") && regexp.contains("^'")) {
               regexp = regexp.replace("$", "");
               regexp = regexp.replace("^", "");
            }
            sql = sql.replace(matcher.group(), regexp);
         }
      }
      return sql;
   }
   
   private String likeSqlTime(String sql) {
      sql = sql == null ? "" : sql;
      if (!sql.isEmpty()) {
         Pattern pattern = Pattern.compile(Regex.LIKE_SQL_TIME.getValue(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
         Matcher matcher = pattern.matcher(sql);
         String regexp = "";
         while (matcher.find()) {
            regexp = matcher.group();
            regexp = regexp.replaceAll("(\\w+)_(\\w+)", "$1@$2");
            regexp = regexp.replaceAll("_", "");
            regexp = regexp.replaceAll("@", "_");
            regexp = regexp.replaceAll("(.*)LIKE(.*)", "DATE_FORMAT($1, '%T') REGEXP$2");
            regexp = regexp.replaceAll(" , ", ", ");
            regexp = regexp.replaceAll("%'", "^'");
            regexp = regexp.replaceAll("'%", "'\\$");
            regexp = regexp.replaceAll("'(\\d*)(\\^)'", "'$2$1'");
            regexp = regexp.replaceAll("'(\\$)(\\d*)'", "'$2$1'");
            regexp = regexp.replace("$d", "%d");
            if (regexp.contains("'$") && regexp.contains("^'")) {
               regexp = regexp.replace("$", "");
               regexp = regexp.replace("^", "");
            }
            sql = sql.replace(matcher.group(), regexp);
         }
      }
      return sql;
   }
   
   public <T extends Model> QuerySet<T> query(String sql, Class<T> clazz) {
      return raw(sql, clazz);
   }
   
   public List<Row> query(String sql) {
      List<Row> rows = null;
      List<List<Map<String, Object>>> records = raw(sql);
      if (records != null) {
         rows = new ArrayList<>();
         Row row = null;
         Column column = null;
         for (List<Map<String, Object>> record : records) {
            for (Map<String, Object> map : record) {
               column = Column.of(map);
               row = Row.of(column);
               rows.add(row);
            }
         }
      }
      return rows;
   }
   
   public <T extends Model> T getRandom() {
      T object = null;
      int count = this.count();
      if (count > 0) {
         Random random = new Random();
         int id = random.nextInt(count) + 1;
         object = this.get("id", id);
      }
      return object;
   }
   
   public <T extends Model> T random() {
      return getRandom();
   }
   
   private <T extends Model> List<T> getRandom(int size) {
      List<T> objects = null;
      if (size > 0) {
         int count = this.count();
         if (count >= size) {
            Random random = new Random();
            int id = 0;
            T object = null;
            objects = new ArrayList<>();
            for (int i = 0; i < size; i++) {
               id = random.nextInt(count) + 1;
               object = this.get("id", id);
               objects.add(object);
            }
         } else {
            exceptionMessage = "ATENÇÃO: A quantidade de registros gravados na tabela é menor que o parâmetro size!";
            throw new IllegalStateException(exceptionMessage);
         }
      }
      return objects;
   }
   
   public <T extends Model> List<T> random(int size) {
      return getRandom(size);
   }
   
   public <T extends Model> List<T> getDistinctRandom(int size) {
      List<T> objects = null;
      if (size > 0) {
         int count = this.count();
         if (count >= size) {
            Random random = new Random();
            StringBuilder ids = new StringBuilder();
            int randomInt = 0;
            for (int i = 0; i < size; i++) {
               randomInt = random.nextInt(count) + 1;
               ids.append(randomInt + ", ");
            }
            ids.deleteCharAt(ids.length() - 2);
            String criteria = String.format("id__in=[%s]", ids.toString());
            objects = this.filter(criteria);
         } else {
            exceptionMessage = "ATENÇÃO: A quantidade de registros gravados na tabela é menor que o parâmetro size!";
            throw new IllegalStateException(exceptionMessage);
         }
      }
      return objects;
   }
   
   public <T extends Model> List<T> distinctRandom(int size) {
      return getDistinctRandom(size);
   }
   
   public <T extends Model> T id(int id) {
      T model = null;
      if (id > 0) {
         model = this.get("id", id);
      } else {
         exceptionMessage = "ATENÇÃO: O id deve ser um número inteiro maior que zero!";
         throw new IllegalStateException(exceptionMessage);
      }
      return model;
   }
   
   private <T extends Model> QuerySet<T> find(String field, Object value, Class<T> modelClass) {
      QuerySet<T> models = (QuerySet<T>) EMPTY_QUERYSET;
      T model = null;
      String columnName = "";
      Object o = null;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      OneToOneField oneToOneFieldAnnotation = null;
      ForeignKeyField foreignKeyFieldAnnotation = null;
      ManyToManyField manyToManyFieldAnnotation = null;
      field = field == null ? "" : field.trim();
      if (!field.isEmpty()) {
         try {
            field = TableUtil.getColumnName(field);
            String sql = "SELECT * FROM";
            if (value != null) {
               sql = String.format("%s %s WHERE %s = '%s'", sql, tableName, field, value.toString());
            } else {
               if (field.equals("id")) {
                  return null;
               }
               sql = String.format("%s %s WHERE %s IS NULL", sql, tableName, field);
            }
            if (Integer.class.isInstance(value) || Float.class.isInstance(value) || Double.class.isInstance(value)) {
               sql = sql.replaceAll("\'", "");
            }
            connect();
            statement = connection.prepareStatement(sql);
            if (JediEngine.DEBUG) {
               System.out.println(sql + ";\n");
            }
            resultSet = statement.executeQuery();
            int rowCount = resultSet.last() ? resultSet.getRow() : 0;
            if (rowCount > 0) {
               models = new QuerySet<T>();
            }
            resultSet.beforeFirst();
            while (resultSet.next()) {
               model = (T) entity.newInstance();
               if (resultSet.getObject("id") != null) {
                  Field id = jedi.db.models.Model.class.getDeclaredField("id");
                  id.setAccessible(true);
                  o = resultSet.getObject(id.getName());
                  if (connection.toString().startsWith("oracle")) {
                     id.set(model, ((java.math.BigDecimal) o).intValue());
                  } else {
                     id.set(model, o);
                  }
               }
               List<Field> _fields = JediEngine.getAllFields(this.entity);
               for (Field f : _fields) {
                  f.setAccessible(true);
                  if (!JediEngine.isJediField(f)) {
                     continue;
                  }
                  if (f.getName().equals("serialVersionUID"))
                     continue;
                  if (f.getName().equalsIgnoreCase("objects"))
                     continue;
                  oneToOneFieldAnnotation = f.getAnnotation(OneToOneField.class);
                  foreignKeyFieldAnnotation = f.getAnnotation(ForeignKeyField.class);
                  manyToManyFieldAnnotation = f.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Manager manager = null;
                  String referencedModel = null;
                  String referencedTable = null;
                  if (manyToManyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? manyToManyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        if (!manyToManyFieldAnnotation.through().getSimpleName().equals(Model.class.getSimpleName())) {
                           continue;
                        }
                        Class superClazz = null;
                        Class clazz = null;
                        referencedModel = manyToManyFieldAnnotation.model().getSimpleName();
                        referencedModel = Model.class.getSimpleName().equals(referencedModel) ? "" : referencedModel;
                        if (referencedModel.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(f.getGenericType().getClass())) {
                              genericType = (ParameterizedType) f.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 clazz = (Class) genericType.getActualTypeArguments()[0];
                                 referencedModel = clazz.getSimpleName();
                              }
                           }
                        }
                        referencedTable = manyToManyFieldAnnotation.references();
                        if (referencedTable == null || referencedTable.trim().isEmpty()) {
                           if (clazz != null) {
                              referencedTable = TableUtil.getTableName(clazz);
                           } else {
                              referencedTable = TableUtil.getTableName(referencedModel);
                           }
                        }
                        String packageName = this.entity.getPackage().getName();
                        Class associatedModelClass = Class.forName(String.format("%s.%s", packageName, referencedModel));
                        manager = new Manager(associatedModelClass);
                        QuerySet associatedModelsQuerySet = manager.raw(
                           String.format(
                              "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                              referencedTable,
                              referencedModel.toLowerCase(),
                              tableName,
                              referencedTable,
                              TableUtil.getColumnName(model.getClass()),
                              model.getId()
                           ),
                           associatedModelClass
                        );
                        f.set(model, associatedModelsQuerySet);
                     } else {
                        f.set(model, null);
                     }
                  } else if (foreignKeyFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? foreignKeyFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(f.getType().getName());
                        manager = new Manager(associatedModelClass);
                        Model associatedModel = manager.get("id", resultSet.getObject(String.format("%s_id", TableUtil.getColumnName(f))));
                        f.set(model, associatedModel);
                     } else {
                        f.set(model, null);
                     }
                  } else if (oneToOneFieldAnnotation != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? oneToOneFieldAnnotation.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        Class associatedModelClass = Class.forName(f.getType().getName());
                        manager = new Manager(associatedModelClass);
                        columnName = TableUtil.getColumnName(f.getType().getSimpleName());
                        o = resultSet.getObject(String.format("%s_id", columnName));
                        Model associatedModel = manager.get("id", o);
                        f.set(model, associatedModel);
                     } else {
                        f.set(model, null);
                     }
                  } else {
                     if ((f.getType().getSimpleName().equals("int") || 
                          f.getType().getSimpleName().equals("Integer")) && 
                          connection.toString().startsWith("oracle")) {
                        columnName = TableUtil.getColumnName(f.getName());
                        o = resultSet.getObject(columnName);
                        if (o == null) {
                           f.set(model, 0);
                        } else {
                           columnName = TableUtil.getColumnName(f.getName());
                           o = resultSet.getObject(columnName);
                           f.set(model, ((BigDecimal) o).intValue());
                        }
                     } else {
                        columnName = TableUtil.getColumnName(f.getName());
                        o = resultSet.getObject(columnName);
                        if (o instanceof java.sql.Date) {
                           java.sql.Date date = (java.sql.Date) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(date.getTime());
                           calendar.set(Calendar.HOUR_OF_DAY, 0);
                           calendar.set(Calendar.MINUTE, 0);
                           calendar.set(Calendar.SECOND, 0);
                           o = calendar.getTime();
                        }
                        if (o instanceof java.sql.Time) {
                           java.sql.Time time = (java.sql.Time) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(time.getTime());
                           calendar.set(Calendar.YEAR, 0);
                           calendar.set(Calendar.MONTH, 0);
                           calendar.set(Calendar.DAY_OF_MONTH, 0);
                           o = calendar.getTime();
                        }
                        if (o instanceof Timestamp) {
                           Timestamp timestamp = (Timestamp) o;
                           Calendar calendar = Calendar.getInstance();
                           calendar.setTimeInMillis(timestamp.getTime());
                           o = calendar.getTime();
                        }
                        f.set(model, o);
                     }
                  }
                  manager = null;
               }
               models.add(model);
            }
            if (model != null) {
               model.setPersisted(true);
            }
         } catch (SQLException e) {
            e.printStackTrace();
         } catch (InstantiationException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
         } catch (SecurityException e) {
            e.printStackTrace();
         } catch (ClassNotFoundException e) {
            e.printStackTrace();
         } finally {
            close(resultSet, statement, connection);
         }
      }
      return models;
   }
   
   public <T extends Model> QuerySet<T> find(String field, Object value) {
      return (QuerySet<T>) find(field, value, this.entity);
   }
   
   // QuerySet API Reference
   // https://docs.djangoproject.com/en/1.10/ref/models/querysets/
   // TODO - implementar o filtro search
   // TODO - implementar o filtro regex
   // TODO - implementar o filtro iregex
   
}
