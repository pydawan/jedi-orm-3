package jedi.db.models;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jedi.db.exceptions.IndexException;
import jedi.db.exceptions.SyntaxException;
import jedi.db.exceptions.ValueException;
import jedi.types.Block;
import jedi.types.Function;

/**
 * Representa uma lista de objetos retornados do banco de dados ou
 * armazenados na memória. QuerySet possui uma API praticamente igual para
 * listas de objetos persistentens ou transientes.
 * 
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 * @param <T>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class QuerySet<T extends Model> extends ArrayList<T> {
   
   private static final long serialVersionUID = -4117280644858641809L;
   private Class<T> entity = null;
   private int offset = 0;
   private transient boolean persited;
   
   public QuerySet() {}
   
   public QuerySet(int size) {
      super(size);
   }
   
   public QuerySet(Class<T> entity) {
      this.entity = entity;
   }
   
   public QuerySet(Collection<T> collection) {
      super(collection);
   }
   
   public QuerySet(T... collection) {
      super(Arrays.asList(collection));
   }
   
   // Getters
   public Class<T> getEntity() {
      return entity;
   }
   
   public Class<T> entity() {
      return entity;
   }
   
   public boolean isPersited() {
      return this.persited;
   }
   
   // Setters
   public void setEntity(Class<T> entity) {
      this.entity = entity;
   }
   
   public void entity(Class<T> entity) {
      this.entity = entity;
   }
   
   public QuerySet<T> setPersisted(boolean isPersisted) {
      this.persited = isPersisted;
      for (T o : this) {
         o.setPersisted(isPersisted);
      }
      return this;
   }
   
   // orderBy
   public QuerySet<T> orderBy(String field) {
      QuerySet<T> orderedList = new QuerySet<T>();
      orderedList.setEntity(this.entity);
      if (field != null && !field.equals("") && !this.isEmpty()) {
         Comparator comparator = null;
         try {
            // As variáveis abaixo tem modificador final para serem
            // acessadas nas classes internas.
            final String fld = field.replace("-", "");
            final String fld2 = field;
            Field f = null;
            if (field.equals("id") || field.equals("-id")) {
               f = this.entity.getSuperclass().getDeclaredField("id");
            } else {
               f = this.entity.getDeclaredField(fld);
            }
            f.setAccessible(true);
            if (f != null) {
               if (field.equals("id")) {
                  comparator = new Comparator<Model>() {
                     
                     public int compare(Model m1, Model m2) {
                        if (m1.getId() < m2.getId()) {
                           return -1;
                        }
                        if (m1.getId() > m2.getId()) {
                           return 1;
                        }
                        return 0;
                     }
                  };
               } else if (field.equals("-id")) {
                  comparator = new Comparator<Model>() {
                     
                     public int compare(Model m1, Model m2) {
                        if (m1.getId() < m2.getId()) {
                           return 1;
                        }
                        if (m1.getId() > m2.getId()) {
                           return -1;
                        }
                        return 0;
                     }
                  };
               }
               if (f.getType().getName().equals("java.lang.String")) {
                  comparator = new Comparator<Model>() {
                     
                     public int compare(Model m1, Model m2) {
                        int result = 0;
                        try {
                           Field f1 = m1.getClass().getDeclaredField(fld);
                           f1.setAccessible(true);
                           Field f2 = m2.getClass().getDeclaredField(fld);
                           f2.setAccessible(true);
                           if (fld2.startsWith("-")) {
                              result = ((String) f2.get(m2)).compareTo((String) f1.get(m1));
                           } else {
                              result = ((String) f1.get(m1)).compareTo((String) f2.get(m2));
                           }
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                        return result;
                     }
                  };
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
         Collections.sort(this, comparator);
         orderedList = this;
      }
      return orderedList;
   }
   
   public QuerySet<T> limit(int... params) {
      QuerySet<T> objs = new QuerySet<T>();
      objs.setEntity(entity);
      if (!this.isEmpty() && params != null) {
         int start = 0;
         int end = 0;
         // Se for um argumento:
         // params[0] - limit
         if (params.length == 1) {
            if (this.offset > 0) {
               start = this.offset;
               // Reconfigurando a memória de deslocamento.
               this.offset = 0;
            } else {
               start = 0;
            }
            end = start + params[0];
         }
         // Se forem dois argumentos:
         // params[0] - offset
         // params[1] - limit
         if (params.length == 2) {
            start = params[0];
            end = params[0] + params[1];
         }
         if (end > this.size()) {
            end = this.size();
         }
         for (int i = start; i < end; i++) {
            objs.add(this.get(i));
         }
      }
      return objs;
   }
   
   public QuerySet<T> offset(int offset) {
      QuerySet<T> records = new QuerySet<T>();
      records.setEntity(this.entity);
      this.offset = offset;
      // Verificando se a lista é vazia.
      if (!this.isEmpty()) {
         for (int i = offset; i < this.size(); i++) {
            records.add(this.get(i));
         }
      }
      return records;
   }
   
   public QuerySet<T> save() {
      if (!this.isEmpty()) {
         boolean autoCloseConnection;
         for (Object o : this) {
            Model model = (Model) o;
            autoCloseConnection = model.autoCloseConnection();
            model.autoCloseConnection(false);
            model.save();
            model.autoCloseConnection(autoCloseConnection);
         }
         // Informando que a lista foi persistida.
         this.setPersisted(true);
      }
      return this;
   }
   
   public QuerySet<T> delete() {
      if (!this.isEmpty()) {
         Model model;
         for (Object o : this) {
            model = (Model) o;
            // Desabilitando o fechamento automático da conexão após
            // cada operação no banco de dados.
            model.autoCloseConnection(false);
            model.delete();
         }
         // Informando que a lista não se encontra persistida no banco de
         // dados.
         this.setPersisted(false);
         this.removeAll(this);
      }
      return this;
   }
   
   public int count() {
      return this.size();
   }
   
   public QuerySet<T> all() {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      for (int i = 0; i < this.size(); i++) {
         querySet.add(this.get(i));
      }
      return querySet;
   }
   
   private QuerySet<T> in(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      try {
         if (query != null && !query.trim().isEmpty()) {
            query = query.replace("__in", "");
            // query = query.replace("', ", "',");
            query = query.replaceAll("',\\s+", "',");
            query = query.replaceAll("[\\[\\]]", "");
            String[] queryComponents = query.split("=");
            if (queryComponents != null && queryComponents.length > 0) {
               String fieldName = queryComponents[0].trim();
               String[] fieldValues = queryComponents[1].split(",");
               Field field = null;
               if (fieldName.equalsIgnoreCase("id")) {
                  field = this.entity.getSuperclass().getDeclaredField(fieldName);
               } else {
                  field = this.entity.getDeclaredField(fieldName);
               }
               field.setAccessible(true);
               for (T model : this) {
                  for (String fieldValue : fieldValues) {
                     if (field.get(model) != null && field.get(model).toString().equals(fieldValue.replaceAll("'(.*)'", "$1"))) {
                        querySet.add(model);
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return querySet;
   }
   
   private QuerySet<T> range(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      try {
         if (query != null && !query.trim().isEmpty()) {
            query = query.replace("__range", "");
            query = query.replace(", ", ",");
            query = query.replaceAll("['\\[\\]]", "");
            String[] queryComponents = query.split("=");
            if (queryComponents != null && queryComponents.length > 0) {
               String fieldName = queryComponents[0];
               String[] fieldValues = queryComponents[1].split(",");
               Field field = null;
               if (fieldName.trim().equalsIgnoreCase("id")) {
                  field = this.entity.getSuperclass().getDeclaredField(fieldName);
               } else {
                  field = this.entity.getDeclaredField(fieldName);
               }
               field.setAccessible(true);
               for (T model : this) {
                  for (int fieldValue = Integer.parseInt(fieldValues[0]); fieldValue <= Integer.parseInt(fieldValues[1]); fieldValue++) {
                     if (field.get(model).equals(fieldValue)) {
                        querySet.add(model);
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return querySet;
   }
   
   private QuerySet<T> filterNumericField(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.trim().isEmpty()) {
         query = query.trim().toLowerCase();
         // (<|<=|==|!=|>=|>)
         String[] queryComponents = query.split("\\s+");
         String fieldName = queryComponents[0].trim();
         String operator = queryComponents[1].trim();
         String fieldValue = queryComponents[2].trim();
         // System.out.printf("%s%s%s", field_name, operator, field_value);
         Field field = null;
         try {
            if (fieldName.equals("id")) {
               field = this.entity.getSuperclass().getDeclaredField(fieldName);
            } else {
               field = this.entity.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            for (T model : this) {
               if (operator.equals("<")) {
                  if (field.getDouble(model) < Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else if (operator.equals("<=")) {
                  if (field.getDouble(model) <= Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else if (operator.equals("=")) {
                  if (field.getDouble(model) == Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else if (operator.equals("!=")) {
                  if (field.getDouble(model) != Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else if (operator.equals(">")) {
                  if (field.getDouble(model) > Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else if (operator.equals(">=")) {
                  if (field.getDouble(model) >= Double.parseDouble(fieldValue)) {
                     querySet.add(model);
                  }
               } else {
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return querySet;
   }
   
   private QuerySet<T> exact(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.trim().isEmpty()) {
         query = query.replace("__exact", "");
         String[] queryComponents = query.split("=");
         String fieldName = queryComponents[0];
         String fieldValue = queryComponents[1];
         if (fieldValue.equalsIgnoreCase("null")) {
            querySet.add(this.isNull(String.format("%s__isnull=true", fieldName)));
         } else {
            querySet.add(this.in(String.format("%s__in=[%s]", fieldName, fieldValue)));
         }
      }
      return querySet;
   }
   
   private QuerySet<T> isNull(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.trim().isEmpty()) {
         query = query.trim().toLowerCase();
         query = query.replace("__isnull", "");
         String[] queryComponents = query.split("=");
         String fieldName = queryComponents[0];
         boolean isNull = Boolean.parseBoolean(queryComponents[1]);
         Field field = null;
         try {
            if (fieldName.equalsIgnoreCase("id")) {
               field = this.entity.getSuperclass().getDeclaredField(fieldName);
            } else {
               field = this.entity.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            for (T model : this) {
               if (isNull) {
                  if (field.get(model) == null) {
                     querySet.add(model);
                  }
               } else {
                  if (field.get(model) != null) {
                     querySet.add(model);
                  }
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return querySet;
   }
   
   public QuerySet<T> startsWith(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.isEmpty()) {
         query = query.replace("__startswith", "");
         String[] queryComponents = query.split("=");
         String fieldName = queryComponents[0];
         String fieldValue = queryComponents[1];
         Field field = null;
         try {
            if (fieldName.equals("id")) {
               field = this.entity.getSuperclass().getDeclaredField(fieldName);
            } else {
               field = this.entity.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            String pattern = String.format("^%s.*$", fieldValue.replace("'", ""));
            
            for (T model : this) {
               if (field.get(model) != null && field.get(model).toString().matches(pattern)) {
                  querySet.add(model);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return querySet;
   }
   
   public QuerySet<T> endsWith(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.isEmpty()) {
         query = query.replace("__endswith", "");
         String[] queryComponents = query.split("=");
         String fieldName = queryComponents[0];
         String fieldValue = queryComponents[1];
         Field field = null;
         try {
            if (fieldName.equals("id")) {
               field = this.entity.getSuperclass().getDeclaredField(fieldName);
            } else {
               field = this.entity.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            String pattern = String.format("^.*%s$", fieldValue.replace("'", ""));
            
            for (T model : this) {
               if (field.get(model) != null && field.get(model).toString().matches(pattern)) {
                  querySet.add(model);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return querySet;
   }
   
   public QuerySet<T> contains(String query) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (query != null && !query.isEmpty()) {
         query = query.replace("__contains", "");
         String[] queryComponents = query.split("=");
         String fieldName = queryComponents[0];
         String fieldValue = queryComponents[1];
         Field field = null;
         try {
            if (fieldName.equals("id")) {
               field = this.entity.getSuperclass().getDeclaredField(fieldName);
            } else {
               field = this.entity.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            String pattern = String.format("^.*%s.*$", fieldValue.replace("'", ""));
            
            for (T model : this) {
               if (field.get(model) != null && field.get(model).toString().matches(pattern)) {
                  querySet.add(model);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return querySet;
   }
   
   public QuerySet<T> filter(String... queries) {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (queries != null && !queries.toString().trim().isEmpty()) {
         for (String query : queries) {
            query = query.trim();
            // Retirando o excesso de espaços ao redor do operador =
            query = query.replaceAll("\\s*=\\s*", "=");
            // Tratamento para consulta field__in=['valor1', 'valor2']
            if (query.matches("^\\w+__in=\\[(\\d+|\\d+((,|, )\\d+)+)\\]$")) {
               query = query.replaceAll("(\\d+)", "'$1'");
               querySet.add(this.in(query));
            } else if (query.matches("^\\w+__in=\\[('[^']+'|'[^']+'((,|, )'[^']+')+)\\]$")) {
               querySet.add(this.in(query));
            } else if (query.matches("^\\w+__range=\\[\\d+(,|, )\\d+\\]$")) {
               querySet.add(this.range(query));
            } else if (query.matches("^\\s*\\w+\\s+(<|<=|==|!=|>=|>)\\s+\\d+\\s*$")) {
               querySet.add(this.filterNumericField(query));
            } else if (query.matches("^(\\w+)__isnull\\s*=\\s*(true|false)\\s*$")) {
               querySet.add(this.isNull(query));
            } else if (query.matches("^(\\w+)__exact\\s*=\\s*('[^']+'|\\d+|null)\\s*$")) {
               querySet.add(this.exact(query));
            } else if (query.matches("^(\\w+)__(lt|lte|gt|gte)\\s*=\\s*(\\d+)$")) {
               query = query.replace("__lt=", " < ");
               query = query.replace("__lte=", " <= ");
               query = query.replace("__gt=", " > ");
               query = query.replace("__gte=", " >= ");
               querySet = this.filterNumericField(query);
            } else if (query.matches("^(\\w+)__startswith\\s*=\\s*('[^']+'|\\d+)$")) {
               querySet = this.startsWith(query);
            } else if (query.matches("^(\\w+)__endswith\\s*=\\s*('[^']+'|\\d+)$")) {
               querySet = this.endsWith(query);
            } else if (query.matches("^(\\w+)__contains\\s*=\\s*('[^']+'|\\d+)$")) {
               querySet = this.contains(query);
            } else {
            
            }
         }
         querySet.entity(this.entity);
      }
      return querySet;
   }
   
   // Funciona como o filter negado.
   public QuerySet<T> exclude(String... queries) {
      QuerySet<T> querySet = this.all();
      querySet.entity(this.entity);
      querySet = querySet.remove(querySet.filter(queries));
      return querySet;
   }
   
   // O código desse método entrou em conflito com o método save no
   // Manager.java
   // uma vez que ao inserir elementos na QuerySet eles tem seu id definido e o
   // método save só insere
   // models com o id igual a 0.
   // Esse conflito foi solucionado através do atributo is_persisted.
   public boolean add(T model) {
      if (model != null && model.id() == 0) {
         model.id(this.size() + 1);
      }
      return super.add(model);
   }
   
   public QuerySet<T> add(QuerySet<T> querySet) {
      if (querySet != null && !querySet.isEmpty()) {
         querySet.entity(this.entity());
         this.addAll(querySet);
      }
      return this;
   }
   
   public QuerySet<T> add(QuerySet<T>... querySets) {
      if (querySets != null && querySets.length > 0) {
         for (QuerySet<T> querySet : querySets) {
            querySet.entity(this.entity());
            this.add(querySet);
         }
      }
      return this;
   }
   
   public QuerySet<T> add(T... models) {
      // Verificando se o array de modelos passada existe e não está vazia.
      if (models != null && models.length > 0) {
         // Percorrendo cada modelo do array.
         for (T model : models) {
            this.add(model);
         }
      }
      return this;
   }
   
   public QuerySet<T> add(List<T> models) {
      if (models != null && models.size() > 0) {
         for (T model : models) {
            this.add(model);
         }
      }
      return this;
   }
   
   public QuerySet<T> remove(QuerySet<T> querySet) {
      if (querySet != null && !querySet.isEmpty()) {
         this.removeAll(querySet);
      }
      return this;
   }
   
   public QuerySet<T> remove(QuerySet<T>... querySets) {
      if (querySets != null && querySets.length > 0) {
         for (QuerySet<T> querySet : querySets) {
            this.removeAll(querySet);
         }
      }
      return this;
   }
   
   public QuerySet<T> remove(String... queries) {
      QuerySet<T> querySet = this.filter(queries);
      if (querySet != null && !querySet.isEmpty()) {
         this.removeAll(querySet);
      }
      return this;
   }
   
   public QuerySet<T> remove(String query) {
      QuerySet<T> querySet = this.filter(query);
      if (querySet != null && !querySet.isEmpty()) {
         this.removeAll(querySet);
      }
      return this;
   }
   
   public QuerySet<T> remove(T model) {
      if (!this.isEmpty() && model != null) {
         // Fazendo cast para Object para evitar StackOverFlowError.
         // Esse erro ocorre porque é feita uma chamada recursiva a esse
         // método e ao fazer o cast o Java
         // chamada o método desejado.
         this.remove((Object) model);
      }
      return this;
   }
   
   public QuerySet<T> remove(T... models) {
      if (models != null && models.length > 0) {
         for (T model : models) {
            this.remove(model);
         }
      }
      return this;
   }
   
   public QuerySet<T> distinct() {
      QuerySet<T> querySet = new QuerySet<T>();
      querySet.setEntity(this.entity);
      if (!this.isEmpty()) {
         // Eliminando elementos repetidos da coleção através de HashSet.
         querySet = new QuerySet(new HashSet<T>(this));
      }
      return querySet;
   }
   
   public T earliest() {
      T model = null;
      if (!this.isEmpty()) {
         model = this.get(0);
      }
      return model;
   }
   
   public T latest() {
      T model = null;
      if (!this.isEmpty()) {
         model = this.get(this.size() - 1);
      }
      return model;
   }
   
   public QuerySet<T> get(String field, Object value) {
      QuerySet<T> querySet = null;
      if (!this.isEmpty()) {
         if (value instanceof String) {
            querySet = this.filter(String.format("%s__in=['%s']", field, value));
         } else {
            querySet = this.filter(String.format("%s__in=[%s]", field, value));
         }
      }
      return querySet;
   }
   
   public T get(String id, int value) {
      T model = null;
      QuerySet<T> querySet = null;
      if (!this.isEmpty()) {
         querySet = this.get("id", new Integer(value));
         model = querySet != null && !querySet.isEmpty() ? querySet.get(0) : null;
      }
      return model;
   }
   
   public boolean exists() {
      if (!this.isEmpty()) {
         return true;
      }
      return false;
   }
   
   public QuerySet<T> reverse() {
      if (!this.isEmpty()) {
         Collections.reverse(this);
         return this;
      }
      return null;
   }
   
   public String repr() {
      String string = "[";
      if (!this.isEmpty()) {
         string += "\n";
      }
      for (Model model : this) {
         string += String.format("%s,\n", model.repr(1));
      }
      if (!this.isEmpty()) {
         string = string.substring(0, string.length() - 2);
         string += "\n";
      }
      string += "]\n";
      return string;
   }
   
   public String toJSON() {
      String json = "[";
      if (!this.isEmpty()) {
         json += "\n";
      }
      for (Model model : this) {
         json += String.format("%s,\n", model.toJSON(1));
      }
      if (!this.isEmpty()) {
         json = json.substring(0, json.length() - 2);
         json += "\n";
      }
      json += "]\n";
      return json;
   }
   
   public String toXML() {
      StringBuilder xml = new StringBuilder();
      String xmlElementOpenTag = "";
      String xmlElementCloseTag = "";
      Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
      if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
         xmlElementOpenTag = String.format("<%s>", tableAnnotation.name().trim().toLowerCase());
         xmlElementCloseTag = String.format("<%s>", tableAnnotation.name().trim().toLowerCase());
      } else {
         xmlElementOpenTag = String.format("<%ss>", this.entity.getSimpleName().toLowerCase());
         xmlElementCloseTag = String.format("</%ss>", this.entity.getSimpleName().toLowerCase());
      }
      if (!this.isEmpty()) {
         xml.append(xmlElementOpenTag);
         xml.append("\n");
         for (Model model : this) {
            xml.append(model.toXML(1));
            xml.append("\n");
         }
         xml.append(xmlElementCloseTag);
      } else {
         xmlElementOpenTag = xmlElementOpenTag.replace(">", " />");
         xml.append(xmlElementOpenTag);
         xml.append("\n");
      }
      return xml.toString();
   }
   
   public String toExtenseXML() {
      StringBuilder xml = new StringBuilder();
      String xmlElementOpenTag = "";
      String xmlElementCloseTag = "";
      Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
      if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
         xmlElementOpenTag = String.format("<%s>", tableAnnotation.name().trim().toLowerCase());
         xmlElementCloseTag = String.format("<%s>", tableAnnotation.name().trim().toLowerCase());
      } else {
         xmlElementOpenTag = String.format("<%ss>", this.entity.getSimpleName().toLowerCase());
         xmlElementCloseTag = String.format("</%ss>", this.entity.getSimpleName().toLowerCase());
      }
      xml.append(xmlElementOpenTag);
      if (!this.isEmpty()) {
         xml.append("\n");
         for (Model model : this) {
            xml.append(model.toExtenseXML(1));
            xml.append("\n");
         }
      }
      xml.append(xmlElementCloseTag);
      return xml.toString();
   }
   
   public String toCSV() {
      String csv = "";
      for (Model model : this) {
         csv += String.format("%s\n", model.toCSV());
      }
      return csv + "\n";
   }
   
   public QuerySet<T> append(T object) {
      if (object != null) {
         this.add(object);
      }
      return this;
   }
   
   public <E extends Model> QuerySet<E> as(Class<E> c) {
      return (QuerySet<E>) this;
   }
   
   public void each(Block<T> block) {
      int index = 0;
      if (block != null) {
         for (T object : this) {
            block.index = index++;
            block.value = object;
            block.run();
         }
      }
   }
   
   public void each(Function<T> function) {
      int index = 0;
      if (function != null) {
         for (T object : this) {
            function.index = index++;
            function.value = object;
            function.run();
         }
      }
   }
   
// public QuerySet<T> set(String field, Object value) {
// for (T model : this) {
// model.set(field, value);
// }
// return this;
// }

// public List<List<String>> get(String fieldNames) {
// List<List<String>> fieldsValues = null;
// if (fieldNames != null && !fieldNames.trim().isEmpty()) {
// fieldsValues = new ArrayList<List<String>>();
// String[] fields = null;
// // Apenas um atributo.
// if (fieldNames.matches("^(\\w+)$")) {
// fields = new String[]{fieldNames};
// } else if (fieldNames.matches("^[\\w+,\\s+\\w+]+$")) {
// // Mais de um atributo.
// // Criando array de fields utilizando vírgula seguida ou não de
// // espaço como separador.
// fields = fieldNames.split(",\\s+");
// } else {
//
// }
// for (T model : this) {
// List<String> fieldValue = new ArrayList<String>();
// for (String field : fields) {
// if (model.get(field) != null) {
// fieldValue.add((model.get(field)).toString());
// } else {
// fieldValue.add((String) model.get(field));
// }
// }
// fieldsValues.add(fieldValue);
// }
// }
// return fieldsValues;
// }
   
   /**
    * Retorna o primeiro objeto correspondente a consulta ou null.
    * 
    * @return Model
    */
   public T first() {
      T obj = null;
      // Verificando se a lista não é vazia.
      if (!this.isEmpty()) {
         // Ordenando a lista em ordem crescente pela chave primária.
         this.orderBy("id");
         // Referenciando o primeiro item da lista.
         obj = this.get(0);
      }
      return obj;
   }
   
   /**
    * Retorna o último objeto correspondente a consulta ou null.
    * 
    * @return Model
    */
   public T last() {
      T obj = null;
      if (!this.isEmpty()) {
         this.orderBy("-id");
         obj = this.get(0);
      }
      return obj;
   }
   
   public QuerySet<T> create(String... list) {
      Manager objects = new Manager(this.entity);
      this.add((T) objects.create(list));
      return this;
   }
   
   public QuerySet<T> slice(int index) {
      QuerySet<T> qs = new QuerySet<T>();
      int size = this.size();
      try {
         if (index < -size || index >= size) {
            throw new IndexException();
         } else {
            if (index < 0) {
               qs.add(this.get(size + index));
            } else {
               qs.add(this.get(index));
            }
         }
      } catch (IndexException e) {
         e.printStackTrace();
      }
      return qs;
   }
   
   public QuerySet<T> slice(Integer start, Integer end, Integer step) {
      QuerySet<T> qs = new QuerySet<T>();
      int size = this.size();
      step = step == null ? 1 : step;
      if (step < 0) {
         if (start == null) {
            start = -1;
         }
         if (end == null || end < -(size + 1)) {
            end = -(size + 1);
         } else if (end >= 0) {
            end = end - size;
         }
         for (int i = start; i > end; i += step) {
            qs.add(this.get(size + i));
         }
      } else if (step == 0) {
         try {
            throw new ValueException("slice step cannot be zero");
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {
         if (start == null || start < -(size + 1)) {
            start = 0;
         } else if (start < 0) {
            start = size + start;
         }
         if (end == null || end > size) {
            end = size;
         } else if (end < 0) {
            end = size + end;
         }
         for (int i = start; i < end; i += step) {
            qs.add(this.get(i));
         }
      }
      return qs;
   }
   
   public QuerySet<T> slice(Integer start, Integer end) {
      return slice(start, end, null);
   }
   
   public QuerySet<T> slice(String slice) {
      QuerySet<T> qs = new QuerySet<T>();
      slice = slice != null ? slice.trim() : "";
      Pattern pattern1 = Pattern.compile("-?\\d+");
      Matcher matcher1 = pattern1.matcher(slice);
      if (matcher1.matches()) {
         int i = Integer.parseInt(matcher1.group());
         qs = slice(i);
      } else {
         Pattern pattern2 = Pattern.compile("(-?\\d+)?:(-?\\d+)?:?(-?\\d+)?");
         Matcher matcher2 = pattern2.matcher(slice);
         if (matcher2.matches()) {
            String _start = matcher2.group(1);
            String _end = matcher2.group(2);
            String _step = matcher2.group(3);
            Integer start = _start != null ? Integer.parseInt(_start) : null;
            Integer end = _end != null ? Integer.parseInt(_end) : null;
            Integer step = _step != null ? Integer.parseInt(_step) : null;
            qs = slice(start, end, step);
         } else {
            try {
               throw new SyntaxException(String.format("[%s] invalid syntax", slice));
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
      return qs;
   }
   
   public String json() {
      return this.toJSON() + "\n";
   }
   
   public String xml() {
      return this.toXML() + "\n";
   }
   
   public void print() {
      this.forEach(System.out::println);
      System.out.println();
   }
   
   public void print(String format) {
      this.forEach(i -> i.print(format));
      System.out.println();
   }
   
   public boolean isEmpty() {
      return super.isEmpty();
   }
   
   public boolean isNotEmpty() {
      return !super.isEmpty();
   }
   
   public boolean empty() {
      return isEmpty();
   }
   
   public boolean notEmpty() {
      return isNotEmpty();
   }
   
   // TODO - Implementar o método query() que irá retornar a instrução SQL correspondente a QuerySet.
}
