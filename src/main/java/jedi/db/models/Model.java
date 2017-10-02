package jedi.db.models;

import static jedi.db.engine.JediEngine.AUTO_CLOSE;
import static jedi.db.engine.JediEngine.AUTO_COMMIT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT;
import static jedi.db.engine.JediEngine.EXCEPTION_HANDLING;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jedi.db.connection.DataSource;
import jedi.db.engine.JediEngine;
import jedi.db.exceptions.DatabaseException;
import jedi.db.util.TableUtil;

/**
 * Classe que representa um modelo.
 *
 * @author Thiago Alexandre Martins Monteiro
 * @version v1.0.0
 * @version v1.0.1 05/09/2017
 * @since v1.0.0
 */
@SuppressWarnings({ "rawtypes", "unused", "unchecked", "deprecation" })
public class Model implements IModel {

   private static final long serialVersionUID = 1L;
   
   public static final List<?> NULL_LIST = null;
   public static final QuerySet<? extends Model> NULL_QUERYSET = null;
   
   public static final List<?> EMPTY_LIST = new ArrayList<>(0);
   public static final QuerySet<? extends Model> EMPTY_QUERYSET = new QuerySet<>(0);
   
   private transient Connection connection;
   private transient boolean autoCloseConnection = AUTO_CLOSE.isValue();
   
   protected int id;
   protected transient boolean persisted;
   protected transient String tableName;
   
   public Model() {
      this.setTableName(TableUtil.tableName(this.getClass()));
   }
   
   public boolean getAutoCloseConnection() {
      return autoCloseConnection;
   }
   
   public boolean autoCloseConnection() {
      return autoCloseConnection;
   }
   
   public Connection getConnection() {
      return connection;
   }
   
   public Connection connection() {
      return connection;
   }
   
   public int getId() {
      return id;
   }
   
   public int id() {
      return id;
   }
   
   public boolean isPersisted() {
      boolean isPersisted = persisted == true || id != 0 ? true : false;
      return isPersisted;
   }
   
   public boolean getPersisted() {
      return isPersisted();
   }
   
   public boolean persisted() {
      return getPersisted();
   }
   
   public void setPersisted(boolean isPersisted) {
      this.persisted = isPersisted;
   }
   
   public Model persisted(boolean persisted) {
      this.persisted = persisted;
      return this;
   }
   
   public String getTableName() {
      return tableName;
   }
   
   public String tableName() {
      return tableName;
   }
   
   // Setters
   public void setConnection(Connection connection) {
      this.connection = connection;
   }
   
   public Model connection(Connection connection) {
      this.connection = connection;
      return this;
   }
   
   public void setTableName(String tableName) {
      this.tableName = TableUtil.getTableName(tableName);
   }
   
   public Model tableName(String tableName) {
      setTableName(tableName);
      return this;
   }
   
   public void setId(int id) {
      this.id = id;
   }
   
   public Model id(int id) {
      this.id = id;
      return this;
   }
   
   public void setAutoCloseConnection(boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
   }
   
   public Model autoCloseConnection(boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
      return this;
   }
   
   /**
    * Método que insere o modelo invocador na tabela apropriada
    * no banco de dados.
    * 
    * @author - Thiago Alexandre Martins Monteiro
    * @param -
    *           nenhum
    * @return - nenhum
    * @throws -
    *            java.lang.Exception
    */
   public void insert() throws DatabaseException {
      Statement statement = null;
      try {
         String sql = "INSERT INTO";
         String columns = "";
         String values = "";
         String defaultValue = "";
         String references = null;
         String manyToManySQLFormatter = "INSERT INTO %s_%s (%s_id, %s_id) VALUES (%d,";
         List<String> manyToManySQLs = new ArrayList<String>();
         Manager associatedModelManager = null;
         Annotation annotation = null;
         Class annotationClass = null;
         for (Field field : JediEngine.getAllFields(this.getClass())) {
            field.setAccessible(true);
            if (!JediEngine.isJediField(field)) {
               continue;
            }
            if (field.getName().equals("serialVersionUID")) continue;
            if (field.getName().equals("objects")) continue;
            // Busca a anotação.
            for (Class class_ : JediEngine.JEDI_FIELD_ANNOTATION_CLASSES) {
               annotation = field.getAnnotation(class_);
               if (annotation != null) {
                  annotationClass = annotation.annotationType();
                  defaultValue = JediEngine.getDefaultValue(annotation);
                  break;
               }
            }
            CascadeType cascadeType = JediEngine.CASCADE_TYPE;
            // Treats the columns.
            if (field.getType().getSuperclass() != null && field.getType().getSuperclass().getSimpleName().equals("Model")) {
               if (annotationClass == OneToOneField.class || annotationClass == ForeignKeyField.class) {
                  columns += String.format("%s_id, ", TableUtil.getColumnName(field));
               }
            } else if (field.getType().getName().equals("java.util.List") || field.getType().getName().equals("jedi.db.models.QuerySet")) {
               // Doesn't creates the field here.
            } else {
               columns += String.format("%s, ", TableUtil.getColumnName(field.getName()));
            }
            // Treats the values.
            if (field.getType().getSimpleName().equalsIgnoreCase("boolean")) {
               values += String.format("%s, ", field.get(this).equals(Boolean.FALSE) ? 0 : 1);
            } else if (field.getType().toString().endsWith("String")) { // Campo texto.
               if (field.get(this) != null) { // Atributo não nulo.
                  // Substituindo ' por \' para evitar erro de sintaxe no SQL.
                  values += String.format("'%s', ", ((String) field.get(this)).replaceAll("'", "\\\\'"));
               } else { // Atributo nulo.
                  // Valor padrão não informado.
                  if (defaultValue.equals("\\0")) {
                     // Remove a coluna da instrução SQL.
                     columns = columns.replace(String.format("%s, ", TableUtil.getColumnName(field.getName())), "");
                  } else if (defaultValue.equalsIgnoreCase("null")) {
                     values += String.format("%s, ", defaultValue.toUpperCase());
                  } else {
                     values += String.format("'%s', ", defaultValue.replaceAll("'", "\\\\'"));
                  }
               }
            } else if (field.getType().toString().endsWith("Date") || field.getType().toString().endsWith("PyDate") ||
               field.getType().toString().endsWith("DateTime")) {
               Date date = (Date) field.get(this);
               if (date != null) {
                  Calendar calendar = Calendar.getInstance();
                  calendar.setTime(date);
//                  calendar.set(Calendar.MILLISECOND, 0);
                  if (annotationClass == DateField.class) {
                     values += String.format("'%d-%02d-%02d', ", date.getYear() + 1900, date.getMonth() + 1, date.getDate());
                  } else if (annotationClass == TimeField.class) {
                     values += String.format("'%02d:%02d:%02d', ", date.getHours(), date.getMinutes(), date.getSeconds());
                  } else if (annotationClass == DateTimeField.class) {
                     values += String.format(
                        "'%d-%02d-%02d %02d:%02d:%02d.%d', ",
                        date.getYear() + 1900,
                        date.getMonth() + 1,
                        date.getDate(),
                        date.getHours(),
                        date.getMinutes(),
                        date.getSeconds(),
                        calendar.get(Calendar.MILLISECOND));
                  } else {
                  
                  }
               } else {
                  if (annotationClass == DateField.class) {
                     if (defaultValue.isEmpty()) {
                        // Atribui a data atual ao inserir ou atualizar.
                        if (((DateField) annotation).auto_now_add()) {
                           SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                           values += String.format("'%s', ", sdf.format(new Date()));
                        } else {
                           // Remove coluna da instrução SQL.
                           columns = columns.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                        }
                     } else {
                        values += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                     }
                  } else if (annotationClass == TimeField.class) {
                     if (defaultValue.isEmpty()) {
                        if (((TimeField) annotation).auto_now_add()) {
                           SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                           values += String.format("'%s', ", sdf.format(new Date()));
                        } else {
                           columns = columns.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                        }
                     } else {
                        values += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                     }
                  } else if (annotationClass == DateTimeField.class) {
                     if (defaultValue.isEmpty()) {
                        if (((DateTimeField) annotation).auto_now_add()) {
//                           SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                           SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                           values += String.format("'%s', ", sdf.format(new Date()));
                        } else {
                           columns = columns.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                        }
                     } else {
                        values += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                     }
                  } else {
                     values += String.format("'', ", field.get(this));
                  }
               }
            } else {
               if (annotationClass == OneToOneField.class || annotationClass == ForeignKeyField.class) {
                  Model model = (Model) field.get(this);
                  if (model != null) {
                     if (cascadeType.equals(CascadeType.NONE)) {
                        if (annotationClass == OneToOneField.class) {
                           cascadeType = ((OneToOneField) annotation).cascade_type();
                        } else {
                           cascadeType = ((ForeignKeyField) annotation).cascade_type();
                        }
                     }
                     if (cascadeType.equals(CascadeType.INSERT) || cascadeType.equals(CascadeType.SAVE) ||
                        cascadeType.equals(CascadeType.ALL)) {
                        if (model != null) {
                           model.save();
                        }
                     }
                     values += String.format("%s, ", model.getId());
                  } else {
                     values += String.format("%s, ", "NULL");
                  }
               } else
                  if ((field.getType().getName().equals("java.util.List") || field.getType().getName().equals("jedi.db.models.QuerySet")) &&
                     annotationClass == ManyToManyField.class) {
                  String model = ((ManyToManyField) annotation).model().getSimpleName();
                  model = Model.class.getSimpleName().equals(model) ? "" : model;
                  ParameterizedType genericType = null;
                  Class superClazz = null;
                  Class clazz = null;
                  if (model.isEmpty()) {
                     if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                        genericType = (ParameterizedType) field.getGenericType();
                        superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                        if (superClazz == Model.class) {
                           clazz = (Class) genericType.getActualTypeArguments()[0];
                           model = clazz.getSimpleName();
                        }
                     }
                  }
                  references = ((ManyToManyField) annotation).references();
                  if (references == null || references.trim().isEmpty()) {
                     if (clazz != null) {
                        references = TableUtil.getTableName(clazz);
                     } else {
                        references = TableUtil.getTableName(model);
                     }
                  }
                  String packageName = this.getClass().getPackage().getName();
                  associatedModelManager = new Manager(Class.forName(String.format("%s.%s", packageName, model)));
                  cascadeType = cascadeType.equals(CascadeType.NONE) ? ((ManyToManyField) annotation).cascade_type() : cascadeType;
                  if (cascadeType.equals(CascadeType.INSERT) || cascadeType.equals(CascadeType.SAVE) ||
                     cascadeType.equals(CascadeType.ALL)) {
                     Model mo = null;
                     if (field.getType().getName().equals("java.util.List")) {
                        if ((List) field.get(this) != null) {
                           for (Object obj : (List) field.get(this)) {
                              if (obj == null) continue;
                              mo = (Model) obj;
                              if (!mo.persisted) {
                                 mo.insert();
                              }
                              manyToManySQLs.add(
                                 String.format(
                                    manyToManySQLFormatter,
                                    tableName,
                                    references,
                                    TableUtil.getColumnName(model),
                                    TableUtil.getColumnName(this.getClass()),
                                    mo.id()));
                           }
                        }
                     }
                     if (field.getType().getName().equals("jedi.db.models.QuerySet")) {
                        if ((QuerySet) field.get(this) != null) {
                           for (Object obj : (QuerySet) field.get(this)) {
                              if (obj == null) continue;
                              mo = (Model) obj;
                              if (!mo.persisted) {
                                 mo.insert();
                              }
                              manyToManySQLs.add(
                                 String.format(
                                    manyToManySQLFormatter,
                                    tableName,
                                    references,
                                    TableUtil.getColumnName(model),
                                    TableUtil.getColumnName(this.getClass()),
                                    mo.id()
                                 )
                              );
                           }
                        }
                     }
                  }
               } else {
                  values += String.format("%s, ", field.get(this));
               }
            }
         }
         columns = columns.substring(0, columns.lastIndexOf(','));
         values = values.substring(0, values.lastIndexOf(','));
         sql = String.format("%s %s (%s) VALUES (%s);", sql, tableName, columns, values);
         if (JediEngine.DEBUG) {
            System.out.println(sql + "\n");
         }
         connect();
         statement = connection.createStatement();
         statement.executeUpdate(sql);
         commit();
         Manager manager = new Manager(this.getClass());
         this.id = manager.getLastInsertedID();
         for (String associatedModelSQL : manyToManySQLs) {
            associatedModelSQL = String.format("%s %d);", associatedModelSQL, this.id());
            associatedModelManager.raw(associatedModelSQL);
         }
      } catch (Exception e) {
         rollback();
         if (EXCEPTION_HANDLING.equals(ExceptionHandling.PRINT)) {
            e.printStackTrace();
         } else {
            throw new DatabaseException(e);
         }
      } finally {
         close(statement, connection);
      }
   }
   
   /**
    * Método que atualiza o modelo invocador na tabela apropriada no banco de
    * dados.
    * 
    * @author - Thiago Alexandre Martins Monteiro
    * @param -
    *           nenhum
    * @return - nenhum
    * @throws -
    *            java.lang.Exception
    */
   public void update(String... args) throws DatabaseException {
      PreparedStatement statement = null;
      try {
         if (args == null) {
            return;
         }
         String sql = "UPDATE";
         String fieldsAndValues = "";
         String defaultValue = "";
         String referencedModel = null;
         String referencedTable = null;
         List<String> manyToManySQLs = new ArrayList<String>();
         sql = String.format("%s %s SET", sql, this.getTableName());
         OneToOneField oneToOneFieldAnnotation = null;
         ForeignKeyField foreignKeyFieldAnnotation = null;
         ManyToManyField manyToManyFieldAnnotation = null;
         DateField dateFieldAnnotation = null;
         TimeField timeFieldAnnotation = null;
         DateTimeField dateTimeFieldAnnotation = null;
         if (args.length == 0) {
            for (Field field : this.getClass().getDeclaredFields()) {
               field.setAccessible(true);
               if (!JediEngine.isJediField(field)) {
                  continue;
               }
               if (field.getName().equals("serialVersionUID")) continue;
               if (field.getName().equals("objects")) continue;
               oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
               foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
               manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
               dateFieldAnnotation = field.getAnnotation(DateField.class);
               timeFieldAnnotation = field.getAnnotation(TimeField.class);
               dateTimeFieldAnnotation = field.getAnnotation(DateTimeField.class);
               CascadeType cascadeType = JediEngine.CASCADE_TYPE;
               String fieldName = field.getName();
               String columnName = TableUtil.getColumnName(fieldName);
               if (field.getType().getName().equals("jedi.db.models.QuerySet") || field.getType().getName().equals("java.util.List")) {
                  if (manyToManyFieldAnnotation != null) {
                     Class superClazz = null;
                     Class clazz = null;
                     referencedModel = manyToManyFieldAnnotation.model().getSimpleName();
                     referencedModel = Model.class.getSimpleName().equals(referencedModel) ? "" : referencedModel;
                     if (referencedModel.isEmpty()) {
                        ParameterizedType genericType = null;
                        if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                           genericType = (ParameterizedType) field.getGenericType();
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
                           referencedTable = TableUtil.getTableName(referencedTable);
                        }
                     }
                     boolean persistedModel = false;
                     cascadeType = cascadeType.equals(CascadeType.NONE) ? manyToManyFieldAnnotation.cascade_type() : cascadeType;
                     if (cascadeType.equals(CascadeType.UPDATE) || cascadeType.equals(CascadeType.SAVE) ||
                        cascadeType.equals(CascadeType.ALL)) {
                        if ((List<Model>) field.get(this) != null) {
                           for (Model model : (List<Model>) field.get(this)) {
                              if (model == null) continue;
                              persistedModel = model.isPersisted();
                              model.save();
                              Object id = model.id;
                              String intermediateModel = manyToManyFieldAnnotation.through().getSimpleName();
                              intermediateModel = Model.class.getSimpleName().equals(intermediateModel) ? "" : intermediateModel;
                              if (intermediateModel.isEmpty()) {
                                 // Checks if the model was persisted.
                                 if (!persistedModel) {
                                    manyToManySQLs.add(
                                       String.format(
                                          "INSERT INTO %s_%s (%s_id, %s_id) VALUES (%s, %s)",
                                          tableName,
                                          TableUtil.getColumnName(referencedTable),
                                          TableUtil.getColumnName(this.getClass()),
                                          TableUtil.getColumnName(referencedModel),
                                          this.id,
                                          id));
                                 } else {
                                    manyToManySQLs.add(
                                       String.format(
                                          "UPDATE %s_%s SET %s_id = %s WHERE %s_id = %s AND %s_id = %s",
                                          tableName,
                                          TableUtil.getColumnName(referencedTable),
                                          TableUtil.getColumnName(referencedModel),
                                          id,
                                          TableUtil.getColumnName(this.getClass()),
                                          this.id,
                                          TableUtil.getColumnName(referencedModel),
                                          id));
                                 }
                              }
                           }
                        }
                     }
                  }
               } else if (field.getType().getSuperclass() != null && field.getType().getSuperclass().getSimpleName().equals("Model")) {
                  Model model = (Model) field.get(this);
                  if (oneToOneFieldAnnotation != null) {
                     cascadeType = cascadeType.equals(CascadeType.NONE) ? oneToOneFieldAnnotation.cascade_type() : cascadeType;
                  } else if (foreignKeyFieldAnnotation != null) {
                     cascadeType = cascadeType.equals(CascadeType.NONE) ? foreignKeyFieldAnnotation.cascade_type() : cascadeType;
                  } else {
                  
                  }
                  if (cascadeType.equals(CascadeType.UPDATE) || cascadeType.equals(CascadeType.SAVE) ||
                     cascadeType.equals(CascadeType.ALL)) {
                     if (model != null) {
                        model.save(); // Saves the model if it not saved yet.
                     }
                  }
                  if (foreignKeyFieldAnnotation != null || oneToOneFieldAnnotation != null) {
                     fieldsAndValues += String.format("%s_id = ", columnName);
                  }
               } else {
                  fieldsAndValues += String.format("%s = ", columnName);
               }
               if (field.getType().toString().endsWith("String")) {
                  if (field.get(this) != null) {
                     fieldsAndValues += String.format("'%s', ", ((String) field.get(this)).replaceAll("'", "\\\\'"));
                  } else {
                     fieldsAndValues += "'', ";
                  }
               } else if (field.getType().toString().endsWith("Date") || field.getType().toString().endsWith("PyDate") ||
                  field.getType().toString().endsWith("DateTime")) {
                  Date date = (Date) field.get(this);
                  if (date != null) {
                     Calendar calendar = Calendar.getInstance();
                     calendar.setTime(date);
                     calendar.set(Calendar.MILLISECOND, 0);
                     if (dateFieldAnnotation != null) {
                        fieldsAndValues += String.format("'%d-%02d-%02d', ", date.getYear() + 1900, date.getMonth() + 1, date.getDate());
                     } else if (timeFieldAnnotation != null) {
                        fieldsAndValues += String.format("'%02d:%02d:%02d', ", date.getHours(), date.getMinutes(), date.getSeconds());
                     } else if (dateTimeFieldAnnotation != null) {
                        fieldsAndValues += String.format(
                           "'%d-%02d-%02d %02d:%02d:%02d.%d', ",
                           date.getYear() + 1900,
                           date.getMonth() + 1,
                           date.getDate(),
                           date.getHours(),
                           date.getMinutes(),
                           date.getSeconds(),
                           calendar.get(Calendar.MILLISECOND));
                     } else {
                     
                     }
                  } else {
                     if (dateFieldAnnotation != null) {
                        defaultValue = JediEngine.getDefaultValue(dateFieldAnnotation);
                        if (defaultValue.isEmpty()) {
                           if (dateFieldAnnotation.auto_now()) {
                              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                              fieldsAndValues += String.format("'%s', ", sdf.format(new Date()));
                           } else {
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s =", TableUtil.getColumnName(field)), "");
                           }
                        } else {
                           fieldsAndValues += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                        }
                     } else if (timeFieldAnnotation != null) {
                        defaultValue = JediEngine.getDefaultValue(timeFieldAnnotation);
                        if (defaultValue.isEmpty()) {
                           if (timeFieldAnnotation.auto_now()) {
                              SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                              fieldsAndValues += String.format("'%s', ", sdf.format(new Date()));
                           } else {
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s =", TableUtil.getColumnName(field)), "");
                           }
                        } else {
                           fieldsAndValues += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                        }
                     } else if (dateTimeFieldAnnotation != null) {
                        defaultValue = JediEngine.getDefaultValue(dateTimeFieldAnnotation);
                        if (defaultValue.isEmpty()) {
                           if (dateTimeFieldAnnotation.auto_now()) {
//                              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                              fieldsAndValues += String.format("'%s', ", sdf.format(new Date()));
                           } else {
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s, ", TableUtil.getColumnName(field)), "");
                              fieldsAndValues = fieldsAndValues.replace(String.format("%s =", TableUtil.getColumnName(field)), "");
                           }
                        } else {
                           fieldsAndValues += String.format(defaultValue.equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", defaultValue);
                        }
                     } else {
                        fieldsAndValues += String.format("'', ", field.get(this));
                     }
                  }
               } else {
                  if (oneToOneFieldAnnotation != null || foreignKeyFieldAnnotation != null) {
//                     Object id = ((Model) field.get(this)).id;
                     Model model = (Model) field.get(this);
                     Object id = model == null ? null : model.id();
                     fieldsAndValues += String.format("%s, ", id);
                  } else if (manyToManyFieldAnnotation != null) {
                  
                  } else {
                     fieldsAndValues += String.format("%s, ", field.get(this));
                  }
               }
            }
            fieldsAndValues = fieldsAndValues.substring(0, fieldsAndValues.lastIndexOf(','));
         } else {
            if (args.length > 0) {
               Field field = null;
               String fieldName = "";
               String fieldValue = "";
               String columnName = "";
               String columnValue = "";
               for (int i = 0; i < args.length; i++) {
                  args[i] = args[i] == null ? "" : args[i].trim();
                  if (args[i].isEmpty()) {
                     continue;
                  }
                  fieldName = args[i].split("=")[0];
                  columnName = TableUtil.getColumnName(fieldName);
                  if (fieldName.endsWith("_id")) {
                     fieldName = fieldName.replace("_id", "");
                  }
                  fieldValue = args[i].split("=")[1];
                  columnValue = fieldValue;
                  if (fieldValue.startsWith("'") && fieldValue.endsWith("'")) {
                     fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
                  }
                  field = this.getClass().getDeclaredField(fieldName);
                  field.setAccessible(true);
                  if (field.getType() == String.class) {
                     field.set(this, fieldValue);
                  } else if (field.getType() == Integer.class) {
                     field.set(this, Integer.parseInt(fieldValue));
                  } else if (field.getType() == Float.class) {
                     field.set(this, Float.parseFloat(fieldValue));
                  } else if (field.getType() == Double.class) {
                     field.set(this, Double.parseDouble(fieldValue));
                  } else if (field.getType() == Date.class) {
                     field.set(this, Date.parse(fieldValue));
                  } else if (field.getType() == Boolean.class) {
                     field.set(this, Boolean.parseBoolean(fieldValue));
                  } else if (field.getAnnotation(ForeignKeyField.class) != null) {
                     if (field.get(this) != null) {
                        ((Model) field.get(this)).setId(Integer.parseInt(fieldValue));
                     }
                  } else {
                  }
                  fieldsAndValues += String.format("%s = %s, ", columnName, columnValue);
               }
               fieldsAndValues = fieldsAndValues.substring(0, fieldsAndValues.lastIndexOf(","));
            }
         }
         sql = String.format("%s %s WHERE id = %s", sql, fieldsAndValues, jedi.db.models.Model.class.getDeclaredField("id").get(this));
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         statement.execute();
         for (String manyToManySQL : manyToManySQLs) {
            if (JediEngine.DEBUG) {
               System.out.println(manyToManySQL + "\n");
            }
            statement = connection.prepareStatement(manyToManySQL);
            statement.execute();
         }
         commit();
      } catch (Exception e) {
         rollback();
         if (EXCEPTION_HANDLING.equals(ExceptionHandling.PRINT)) {
            e.printStackTrace();
         } else {
            throw new DatabaseException(e);
         }
      } finally {
         close(statement, connection);
      }
   }
   
   public void save() throws DatabaseException {
      if (!persisted()) {
         this.insert();
      } else {
         this.update();
      }
      persisted = true;
   }
   
   public <T extends Model> T save(Class<T> modelClass) throws DatabaseException {
      this.save();
      return this.as(modelClass);
   }
   
   public void delete() throws DatabaseException {
      // TODO - Verificar a viabilidade de CascadeType.DELETE.
      PreparedStatement statement = null;
      try {
         String sql = "DELETE FROM";
         sql = String.format("%s %s WHERE", sql, tableName);
         sql = String.format("%s id = %s", sql, jedi.db.models.Model.class.getDeclaredField("id").get(this));
         if (JediEngine.DEBUG) {
            System.out.println(sql + ";\n");
         }
         connect();
         statement = connection.prepareStatement(sql);
         statement.execute();
         commit();
         this.setPersisted(false);
      } catch (Exception e) {
         rollback();
         if (EXCEPTION_HANDLING.equals(ExceptionHandling.PRINT)) {
            e.printStackTrace();
         } else {
            throw new DatabaseException(e);
         }
      } finally {
         close(statement, connection);
      }
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (this.getClass() != obj.getClass()) return false;
      Model other = (Model) obj;
      if (this.id != other.id) return false;
      return true;
   }
   
   public int compareTo(Model model) {
      if (this.id < model.id) {
         return -1;
      }
      if (this.id > model.id) {
         return 1;
      }
      return 0;
   }
   
   public String repr(int i) {
      // i - identation level
      String s = "";
      String identationToClass = "";
      String identationToFields = "    ";
      String identationToListItems = "        ";
      for (int j = 0; j < i; j++) {
         identationToClass += "    ";
         identationToFields += "    ";
         identationToListItems += "    ";
      }
      try {
         s = String.format(
            "%s%s {\n%sid: %s,",
            identationToClass,
            this.getClass().getSimpleName(),
            identationToFields,
            jedi.db.models.Model.class.getDeclaredField("id").get(this));
         List<Field> fields = JediEngine.getAllFields(this.getClass());
         for (Field f : fields) {
            f.setAccessible(true);
            if (f.getName().equals("serialVersionUID")) continue;
            if (f.getName().equalsIgnoreCase("objects")) continue;
            if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model")) {
               if (f.get(this) != null) {
                  s += String.format("\n%s%s: %s,", identationToFields, f.getName(), ((Model) f.get(this)).repr(i + 1).trim());
               }
            } else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet")) {
               String strItems = "";
               for (Object item : (List) f.get(this)) {
                  strItems += String.format("\n%s,", ((Model) item).repr((i + 2)));
               }
               if (strItems.lastIndexOf(",") >= 0) {
                  strItems = strItems.substring(0, strItems.lastIndexOf(","));
               }
               s += String.format("\n%s%s: [%s\n%s],", identationToFields, f.getName(), strItems, identationToFields);
            } else {
               if (f.get(this) instanceof String) {
                  s += String.format("\n%s%s: \"%s\",", identationToFields, f.getName(), f.get(this));
               } else {
                  s += String.format("\n%s%s: %s,", identationToFields, f.getName(), f.get(this));
               }
            }
         }
         if (s.lastIndexOf(",") >= 0) {
            s = s.substring(0, s.lastIndexOf(","));
         }
         s += String.format("\n%s}", identationToClass);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return s;
   }
   
   public String repr() {
      return repr(0);
   }
   
   public String unicode() {
      Class<? extends Model> c = this.getClass();
      String u = c.getSimpleName();
      u = String.format("%s object", u);
      return u;
   }
   
   public String toString() {
      Class<? extends Model> c = this.getClass();
      String clazz = c.getSimpleName();
      return String.format("<%s: %s>", clazz, this.unicode());
   }
   
   /**
    * @param i int
    * @return String
    */
   public String toJSON(int i) {
      // i - identation level
      String json = "";
      String identationToClass = "";
      String identationToFields = "    ";
      String identationToListItems = "        ";
      for (int j = 0; j < i; j++) {
         identationToClass += "    ";
         identationToFields += "    ";
         identationToListItems += "    ";
      }
      try {
         json = String.format(
            "%s{\n%s\"id\": %s,",
            identationToClass,
            identationToFields,
            jedi.db.models.Model.class.getDeclaredField("id").get(this));
         List<Field> fields = JediEngine.getAllFields(this.getClass());
         for (Field f : fields) {
            f.setAccessible(true);
            if (f.getName().equals("serialVersionUID")) continue;
            if (f.getName().equalsIgnoreCase("objects")) continue;
            if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model")) {
               if (f.get(this) != null) {
                  json += String.format("\n%s\"%s\": %s,", identationToFields, f.getName(), ((Model) f.get(this)).toJSON(i + 1).trim());
               } else {
                  json += String.format("\n%s\"%s\": null,", identationToFields, f.getName());
               }
            } else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet")) {
               String strItems = "";
               for (Object item : (List) f.get(this)) {
                  strItems += String.format("\n%s,", ((Model) item).toJSON((i + 2)));
               }
               if (strItems.lastIndexOf(",") >= 0) {
                  strItems = strItems.substring(0, strItems.lastIndexOf(","));
               }
               json += String.format("\n%s\"%s\": [%s\n%s],", identationToFields, f.getName(), strItems, identationToFields);
            } else {
               if (f.get(this) instanceof String) {
                  json += String.format("\n%s\"%s\": \"%s\",", identationToFields, f.getName(), f.get(this));
               } else {
                  json += String.format("\n%s\"%s\": %s,", identationToFields, f.getName(), f.get(this));
               }
            }
         }
         if (json.lastIndexOf(",") >= 0) {
            json = json.substring(0, json.lastIndexOf(","));
         }
         json += String.format("\n%s}", identationToClass);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return json;
   }
   
   /**
    * @return String
    */
   public String toJSON() {
      return toJSON(0);
   }
   
   public String toXML(int i) {
      // i - identation level
      String xmlElement = this.getClass().getSimpleName().toLowerCase();
      StringBuilder xml = new StringBuilder();
      StringBuilder xmlElementAttributes = new StringBuilder();
      StringBuilder xmlChildElements = new StringBuilder();
      xmlElementAttributes.append("");
      String xmlElementString = "";
      String identationToElement = "";
      String identationToChildElements = "    ";
      for (int j = 0; j < i; j++) {
         identationToElement += "    ";
         identationToChildElements += "    ";
      }
      try {
         xmlElementAttributes.append(String.format("id=\"%d\"", jedi.db.models.Model.class.getDeclaredField("id").getInt(this)));
         List<Field> fields = JediEngine.getAllFields(this.getClass());
         for (Field f : fields) {
            f.setAccessible(true);
            if (f.getName().equals("serialVersionUID")) continue;
            if (f.getName().equalsIgnoreCase("objects")) continue;
            if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model")) {
               Model model = (Model) f.get(this);
               if (model != null) {
                  xmlChildElements.append(String.format("\n%s\n", model.toXML(i + 1)));
               } else {
                  xmlChildElements.append(
                     String.format(
                        "\n%s<%s>null</%s>\n",
                        identationToChildElements,
                        f.getType().getSimpleName().toLowerCase(),
                        f.getType().getSimpleName().toLowerCase()));
               }
            } else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet")) {
               String xmlChildOpenTag = "";
               String xmlChildCloseTag = "";
               Table tableAnnotation = null;
               if (!((List) f.get(this)).isEmpty()) {
                  tableAnnotation = ((List) f.get(this)).get(0).getClass().getAnnotation(Table.class);
                  if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
                     xmlChildOpenTag = String.format("\n%s<%s>", identationToChildElements, tableAnnotation.name().trim().toLowerCase());
                     xmlChildCloseTag =
                        String.format("\n%s</%s>\n", identationToChildElements, tableAnnotation.name().trim().toLowerCase());
                  } else {
                     xmlChildOpenTag = String.format(
                        "\n%s<%ss>",
                        identationToChildElements,
                        ((List) f.get(this)).get(0).getClass().getSimpleName().toLowerCase());
                     xmlChildCloseTag = String.format(
                        "\n%s</%ss>",
                        identationToChildElements,
                        ((List) f.get(this)).get(0).getClass().getSimpleName().toLowerCase());
                  }
                  xmlChildElements.append(xmlChildOpenTag);
                  for (Object item : (List) f.get(this)) {
                     xmlChildElements.append(String.format("\n%s", ((Model) item).toXML(i + 2)));
                  }
                  xmlChildElements.append(xmlChildCloseTag);
               }
            } else {
               xmlElementAttributes.append(String.format(" %s=\"%s\"", f.getName(), f.get(this)));
            }
         }
         if (xmlChildElements.toString().isEmpty()) {
            xml.append(String.format("%s<%s %s />", identationToElement, xmlElement, xmlElementAttributes.toString()));
         } else {
            xml.append(
               String.format(
                  "%s<%s %s>%s%s</%s>",
                  identationToElement,
                  xmlElement,
                  xmlElementAttributes.toString(),
                  xmlChildElements,
                  identationToElement,
                  xmlElement));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return xml.toString();
   }
   
   public String toXML() {
      return toXML(0);
   }
   
   public String toExtenseXML(int i) {
      // i - identation level
      String xmlElement = this.getClass().getSimpleName().toLowerCase();
      StringBuilder xml = new StringBuilder();
      StringBuilder xmlElementAttributes = new StringBuilder();
      StringBuilder xmlChildElements = new StringBuilder();
      xmlElementAttributes.append("");
      String xmlElementString = "";
      String identationToElement = "";
      String identationToAttributes = "    ";
      String identationToChildElements = "    ";
      for (int j = 0; j < i; j++) {
         identationToElement += "    ";
         identationToAttributes += "    ";
         identationToChildElements += "    ";
      }
      try {
         xmlElementAttributes.append(
            String.format("\n%s<id>%d</id>\n", identationToAttributes, jedi.db.models.Model.class.getDeclaredField("id").get(this)));
         List<Field> fields = JediEngine.getAllFields(this.getClass());
         for (Field f : fields) {
            f.setAccessible(true);
            if (f.getName().equals("serialVersionUID")) continue;
            if (f.getName().equalsIgnoreCase("objects")) continue;
            if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model")) {
               Model model = (Model) f.get(this);
               if (model != null) {
                  xmlChildElements.append(String.format("%s\n", ((Model) f.get(this)).toExtenseXML(i + 1)));
               } else {
                  xmlChildElements.append(
                     String.format(
                        "%s<%s>null</%s>\n",
                        identationToChildElements,
                        f.getType().getSimpleName().toLowerCase(),
                        f.getType().getSimpleName().toLowerCase()));
               }
            } else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet")) {
               String xmlChildOpenTag = "";
               String xmlChildCloseTag = "";
               Table tableAnnotation = null;
               if (!((List) f.get(this)).isEmpty()) {
                  tableAnnotation = ((List) f.get(this)).get(0).getClass().getAnnotation(Table.class);
                  if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
                     xmlChildOpenTag = String.format("%s<%s>", identationToChildElements, tableAnnotation.name().trim().toLowerCase());
                     xmlChildCloseTag =
                        String.format("\n%s</%s>\n", identationToChildElements, tableAnnotation.name().trim().toLowerCase());
                  } else {
                     xmlChildOpenTag = String
                        .format("%s<%ss>", identationToChildElements, ((List) f.get(this)).get(0).getClass().getSimpleName().toLowerCase());
                     xmlChildCloseTag = String.format(
                        "\n%s</%ss>\n",
                        identationToChildElements,
                        ((List) f.get(this)).get(0).getClass().getSimpleName().toLowerCase());
                  }
                  xmlChildElements.append(xmlChildOpenTag);
                  for (Object item : (List) f.get(this)) {
                     xmlChildElements.append(String.format("\n%s", ((Model) item).toExtenseXML(i + 2)));
                  }
                  xmlChildElements.append(xmlChildCloseTag);
               }
            } else {
               xmlElementAttributes.append(String.format("%s<%s>%s</%s>\n", identationToAttributes, f.getName(), f.get(this), f.getName()));
            }
         }
         if (xmlChildElements.toString().isEmpty()) {
            xml.append(
               String.format(
                  "%s<%s>%s%s</%s>",
                  identationToElement,
                  xmlElement,
                  xmlElementAttributes.toString(),
                  identationToElement,
                  xmlElement));
         } else {
            xml.append(
               String.format(
                  "%s<%s>%s%s%s</%s>",
                  identationToElement,
                  xmlElement,
                  xmlElementAttributes.toString(),
                  xmlChildElements,
                  identationToElement,
                  xmlElement));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return xml.toString();
   }
   
   public String toExtenseXML() {
      return toExtenseXML(0);
   }
   
   /**
    * Retorna a represetação do modelo em formato CSV (Comma Separated Value).
    * 
    * @return String
    */
   public String toCSV() {
      String csv = this.getId() > 0 ? String.format("%d,", this.getId()) : "";
      List<Field> fields = JediEngine.getAllFields(this.getClass());
      for (Field field : fields) {
         field.setAccessible(true);
         if (field.getName().equals("serialVersionUID")) {
            continue;
         }
         if (field.getName().equals("objects")) {
            continue;
         }
         try {
            if (field.get(this) != null) {
               if (Collection.class.isAssignableFrom(field.getType())) {
                  List<Model> models = (List<Model>) field.get(this);
                  for (Model model : models) {
                     csv += String.format("\"%s\",", model.toCSV());
                  }
               } else if (Model.class.isAssignableFrom(field.getType())) {
                  csv += String.format("\"%s\",", ((Model) field.get(this)).toCSV());
               } else {
                  String s = field.get(this).toString();
                  if (s.startsWith("\"") && s.endsWith("\"")) {
                     if (s.contains(",")) {
                        csv += String.format("\"%s\",", field.get(this));
                     } else {
                        csv += String.format("\"\"%s\"\",", field.get(this));
                     }
                  } else {
                     csv += String.format("%s,", field.get(this));
                  }
               }
            }
         } catch (IllegalArgumentException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         }
      }
      csv = csv.endsWith(",") ? csv.substring(0, csv.length() - 1) : csv;
      return csv;
   }
   
   public <T extends Model> T as(Class<T> c) {
      return (T) this;
   }
   
   public String json() {
      return this.toJSON();
   }
   
   public String xml() {
      return this.toExtenseXML();
   }
   
   public void print() {
      System.out.println(this);
   }
   
   public void print(String format) {
      format = format == null ? "" : format.trim();
      switch (format) {
         case "json":
            System.out.println(this.json());
            break;
         case "xml":
            System.out.println(this.xml());
            break;
         default:
            this.print();
      }
   }
   
   /**
    * Retorna se o Manager tem ou não uma conexão válida com o banco de dados.
    * 
    * @return boolean conectado ou não.
    */
   private boolean connected() {
      boolean connected = false;
      if (connection != null) { // conexão existe?
         try {
            connected = connection.isValid(DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT); // a conexão é válida?
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return connected;
   }
   
   private boolean disconnected() {
      return !connected();
   }
   
   /**
    * Conecta no banco de dados.
    * 
    * @return Connection conexão com o banco de dados.
    */
   private Connection connect() {
      if (JediEngine.Pool.isNotActive()) {
         if (disconnected()) {
            connection = DataSource.getConnection();
         }
      } else {
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
   
   public void set(String field, Object value) {
      field = field == null ? "" : field;
      if (!field.isEmpty()) {
         try {
            Field _field = this.getClass().getDeclaredField(field);
            _field.setAccessible(true);
            _field.set(this, value);
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
         } catch (SecurityException e) {
            e.printStackTrace();
         } catch (IllegalArgumentException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         }
      }
   }
   
   public Object get(String field) {
      Object o = null;
      field = field == null ? "" : field;
      if (!field.isEmpty()) {
         try {
            Field _field = this.getClass().getDeclaredField(field);
            _field.setAccessible(true);
            o = _field.get(this);
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
         } catch (SecurityException e) {
            e.printStackTrace();
         } catch (IllegalArgumentException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         }
      }
      return o;
   }
   
/* 
 * JPA Lifecycle Events
 * @PrePersistjavax.persistence.PrePersistJPA annotationIs used to specify callback methods for the corresponding lifecycle event.
 * See JavaDoc Reference Page... - before a new entity is persisted (added to the EntityManager).
 * @PostPersist - after storing a new entity in the database (during commit or flush).
 * @PostLoad - after an entity has been retrieved from the database.
 * @PreUpdate - when an entity is identified as modified by the EntityManager.
 * @PostUpdate - after updating an entity in the database (during commit or flush).
 * @PreRemove - when an entity is marked for removal in the EntityManager.
 * @PostRemove - after deleting an entity from the database (during commit or flush).
*/
   
}
