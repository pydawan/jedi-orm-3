package jedi.db.models;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import jedi.db.exceptions.MultipleObjectsReturnedException;
import jedi.db.exceptions.ObjectDoesNotExistException;

/**
 * @author thiago-amm
 * @version v1.0.0 28/07/2017
 * @since v1.0.0
 */
public interface IManager {
   
   void setConnection(Connection connection);
   
   void setTableName(String tableName);
   
   void setAutoCloseConnection(boolean autoCloseConnection);
   
   IManager bulkCreate(Model... models);
   
   IManager save(Model... models);
   
   IManager save(List<Model> models);
   
   void using(String database);
   
   boolean getAutoCloseConnection();
   
   int _count(String... conditions);
   
   int _count();
   
   Object execute(String sql);
   
   void execute(File script);
   
   String getTableName();
   
   Connection getConnection();
   
   <T extends Model> T create(String... list);
   
   <T extends Model> T get(String field, Object value) throws ObjectDoesNotExistException, MultipleObjectsReturnedException;
   
   <T extends Model> T get(String... fieldLookups) throws ObjectDoesNotExistException, MultipleObjectsReturnedException;
   
   <T extends Model> T latest(String field);
   
   <T extends Model> T latest();
   
   <T extends Model> T earliest(String field);
   
   <T extends Model> T earliest();
   
   <T extends Model> QuerySet<T> all();
   
   <T extends Model> QuerySet<T> _filter(String... fields);
   
   <T extends Model> QuerySet<T> filter(String... fieldsLookups);
   
   <T extends Model> QuerySet<T> _exclude(String... fields);
   
   <T extends Model> QuerySet<T> where(String criteria, Object... values);
   
   <T extends Model> QuerySet<T> distinct(String... fields);
   
   <T extends Model> QuerySet<T> distinct();
   
   <T extends Model> QuerySet<T> orderBy(String... fields);
   
   <T extends Model> T getOrCreate(String... args);
   
   <T extends Model> QuerySet<T> defer(String... fields);
   
   <T extends Model> QuerySet<T> only(String... fields);
   
   <T extends Model> QuerySet<T> run(String sql);
   
   <T extends Model> QuerySet<T> paginate(Integer limit, Integer offset, String where);
   
   <T extends Model> QuerySet<T> paginate(Integer limit, Integer offset);
   
   <T extends Model> QuerySet<T> paginate(Integer limit);
   
   <T extends Model> QuerySet<T> paginate();
   
   <T extends Model> QuerySet<T> _page(Integer first, Integer pageSize, String... conditions);
   
   <T extends Model> QuerySet<T> _page(Integer first, Integer pageSize);
   
   <T extends Model> QuerySet<T> _reversePage(Integer first, Integer pageSize, String... conditions);
   
   <T extends Model> QuerySet<T> _reversePage(Integer first, Integer pageSize);
   
   <S extends Model, T extends Model> QuerySet<S> getSet(Class<T> associatedModelClass, int id);
   
   <T1 extends Model, T2 extends Model> Manager join(Class<T1> clazz1, Class<T2> clazz2, JoinType type);
   
   <T1 extends Model, T2 extends Model> Manager join(Class<T1> clazz1, Class<T2> clazz2);
   
   <T extends Model> Manager join(Class<T> clazz, JoinType type);
   
   <T extends Model> Manager join(Class<T> clazz);
   
   <T extends Model> QuerySet<T> raw(String sql, Class<T> clazz);
   
   List<List<Map<String, Object>>> raw(String sql);
   
   List<List<Map<String, Object>>> fetch();
   
   Manager select(String... fields);
   
   <T extends Model> Manager innerJoin(Class<T> clazz);
   
   <T extends Model> Manager leftJoin(Class<T> clazz);
   
   <T extends Model> Manager rightJoin(Class<T> clazz);
   
   <T extends Model> QuerySet<T> none();
   
   <T extends Model> T first();
   
   <T extends Model> T last();
   
   <T extends Model> IManager delete(Integer... id);
   
   <T extends Model> IManager delete(Model... models);
   
   <T extends Model> IManager delete();
   
   <T extends Model> QuerySet<T> query(String sql, Class<T> clazz);
   
   List<Row> query(String sql);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageStart pageStart);
   
   <T extends Model> QuerySet<T> page(QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> page(QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> page(String... filters);
   
   <T extends Model> QuerySet<T> page();
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber, String... filters);
   
   <T extends Model> QuerySet<T> page(QueryPageNumber pageNumber);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageStart pageStart);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> reversePage(String... filters);
   
   <T extends Model> QuerySet<T> reversePage();
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageSize pageSize);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, QueryPageOrder pageOrder);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber, String... filters);
   
   <T extends Model> QuerySet<T> reversePage(QueryPageNumber pageNumber);
   
}
