package jedi.db.models;

import java.util.List;

import jedi.types.Block;
import jedi.types.Function;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 * @param <T>
 */
@SuppressWarnings("unchecked")
public interface IQuerySet<T extends Model> {
   
   public QuerySet<T> orderBy(String field) ;
   
   public QuerySet<T> limit(int... params);
   
   public QuerySet<T> offset(int offset);
   
   public QuerySet<T> save();
   
   public QuerySet<T> delete();
   
   public int count();
   
   public QuerySet<T> all();
   
   public QuerySet<T> startsWith(String query);
   
   public QuerySet<T> endsWith(String query);
   
   public QuerySet<T> contains(String query);
   
   public QuerySet<T> filter(String... queries);
   
   public QuerySet<T> exclude(String... queries);
   
   public boolean add(T model);
   
   public QuerySet<T> add(QuerySet<T> querySet);
   
   public QuerySet<T> add(QuerySet<T>... querySets);
   
   public QuerySet<T> add(T... models);
   
   public QuerySet<T> add(List<T> models);
   
   public QuerySet<T> remove(QuerySet<T> querySet);
   
   public QuerySet<T> remove(QuerySet<T>... querySets);
   
   public QuerySet<T> remove(String... queries);
   
   public QuerySet<T> remove(String query);
   
   public QuerySet<T> remove(T model);
   
   public QuerySet<T> remove(T... models);
   
   public QuerySet<T> distinct();
   
   public T earliest();
   
   public T latest();
   
   public QuerySet<T> get(String field, Object value);
   
   public T get(String id, int value);
   
   public boolean exists();
   
   public QuerySet<T> reverse();
   
   public String repr();
   
   public String toJSON();
   
   public String toXML();
   
   public String toExtenseXML();
   
   public String toCSV();
   
   public QuerySet<T> append(T object);
   
   public <E extends Model> QuerySet<E> as(Class<E> c);
   
   public void each(Block<T> block);
   
   public void each(Function<T> function);
   
   public QuerySet<T> set(String field, Object value);
   
   public List<List<String>> get(String fieldNames);
   
   public T first();
   
   public T last();
   
   public QuerySet<T> create(String... list);
   
   public QuerySet<T> slice(String slice);
   
   public QuerySet<T> asc(String field);
   
   public QuerySet<T> asc();
   
   public QuerySet<T> desc(String field);
   
   public QuerySet<T> desc();
   
}
