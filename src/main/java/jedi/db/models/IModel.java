package jedi.db.models;

import java.io.Serializable;
import java.sql.Connection;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
public interface IModel extends Comparable<Model>, Serializable {
   
   public boolean getAutoCloseConnection();
   
   public boolean autoCloseConnection();
   
   public int getId();
   
   public int id();
   
   public boolean isPersisted();
   
   public boolean getPersisted();
   
   public String getTableName();
   
   public String tableName();
   
   public void setConnection(Connection connection);
   
   public Model connection(Connection connection);
   
   public void setTableName(String tableName);
   
   public Model tableName(String tableName);
   
   public void setId(int id);
   
   public Model id(int id);
   
   public void setAutoCloseConnection(boolean autoCloseConnection);
   
   public Model autoCloseConnection(boolean autoCloseConnection);
   
   public void setPersisted(boolean isPersisted);
   
   public void save();
   
   public <T extends Model> T save(Class<T> modelClass);
   
   public void delete();
   
   public int compareTo(Model model);
   
   public String toString();
   
   public String repr(int i);
   
   public String repr();
   
   public boolean equals(Object o);
   
   public String toJSON(int i);
   
   public String toJSON();
   
   public String toXML(int i);
   
   public String toXML();
   
   public String toExtenseXML(int i);
   
   public String toExtenseXML();
   
   public String toCSV();
   
   public <T extends Model> T as(Class<T> c);
   
   public void onPreSave();
   
   public void onPostSave();
   
}
