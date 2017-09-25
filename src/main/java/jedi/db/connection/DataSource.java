package jedi.db.connection;

import static jedi.db.engine.JediEngine.AUTO_COMMIT;
import static jedi.db.engine.JediEngine.DATABASE_ACQUIRE_INCREMENT;
import static jedi.db.engine.JediEngine.DATABASE_ACQUIRE_RETRY_ATTEMPTS;
import static jedi.db.engine.JediEngine.DATABASE_ACQUIRE_RETRY_DELAY;
import static jedi.db.engine.JediEngine.DATABASE_AUTO_RECONNECT;
import static jedi.db.engine.JediEngine.DATABASE_AUTO_RECONNECT_FOR_POOLS;
import static jedi.db.engine.JediEngine.DATABASE_CHARSET;
import static jedi.db.engine.JediEngine.DATABASE_ENGINE;
import static jedi.db.engine.JediEngine.DATABASE_HOST;
import static jedi.db.engine.JediEngine.DATABASE_IDLE_TEST_PERIOD;
import static jedi.db.engine.JediEngine.DATABASE_INITIAL_POOL_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_LOGGING;
import static jedi.db.engine.JediEngine.DATABASE_MAX_POOL_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_MAX_STATEMENTS;
import static jedi.db.engine.JediEngine.DATABASE_MIN_POOL_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_NAME;
import static jedi.db.engine.JediEngine.DATABASE_PASSWORD;
import static jedi.db.engine.JediEngine.DATABASE_POOL_ENGINE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HAKIRI_MIN_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_ALLOW_POOL_SUSPENSION;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_AUTO_RECONNECT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CACHE_PREPARED_STATEMENTS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CACHE_RESULTSET_METADATA;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CACHE_SERVER_CONFIGURATION;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_CONNECTION_TIMEOUT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_ELIDE_SET_AUTO_COMMITS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_IDLE_TIMEOUT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_ISOLATE_INTERNAL_QUERIES;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_LEAK_DETECTION;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_MAINTAIN_TIME_STATS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_MAX_LIFETIME;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_MAX_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_MINIMUM_IDLE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SIZE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SQL_LIMIT;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_READ_ONLY;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_REGISTER_MBEANS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_REWRITE_BATCHED_STAMENTS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_USE_LOCAL_SESSION_STATE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_USE_LOCAL_TRANSACTION_STATE;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_USE_SERVER_PREPARED_STATEMENTS;
import static jedi.db.engine.JediEngine.DATABASE_POOL_HIKARI_VALIDATION_TIMEOUT;
import static jedi.db.engine.JediEngine.DATABASE_PORT;
import static jedi.db.engine.JediEngine.DATABASE_UNICODE;
import static jedi.db.engine.JediEngine.DATABASE_USER;
import static jedi.db.engine.JediEngine.DATABASE_USE_SSL;
import static jedi.db.engine.JediEngine.DATABASE_VERIFY_SERVER_CERTIFICATE;
import static jedi.db.engine.JediEngine.JEDI_PROPERTIES_LOADED;
import static jedi.db.models.PoolEngine.C3P0;
import static jedi.db.models.PoolEngine.HIKARI;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;

import jedi.db.engine.JediEngine;

/**
 * @author thiago-amm
 * @version v1.0.0 19/03/2017
 * @version v1.0.1 25/09/2017
 * @since v1.0.0
 */
public class DataSource {
   
   //   TODO - Criar perfis de conexão para os ambientes dev, test, stage, prod.
   //   TODO - O DataSource deve devolver a conexão de acordo com o perfil.
   
   private static ComboPooledDataSource c3p0;
   private static HikariDataSource hikari;
   private static String url = "";
   
   static {
      if (JEDI_PROPERTIES_LOADED) {
         String prefix = "jdbc";
         String engine = DATABASE_ENGINE;
         String host = DATABASE_HOST;
         String port = DATABASE_PORT;
         String user = DATABASE_USER;
         String password = DATABASE_PASSWORD;
         String database = DATABASE_NAME;
         Boolean autoReconnect = DATABASE_AUTO_RECONNECT;
         Boolean autoReconnectForPools = DATABASE_AUTO_RECONNECT_FOR_POOLS;
         Boolean useSSL = DATABASE_USE_SSL;
         Boolean verifyServerCertificate = DATABASE_VERIFY_SERVER_CERTIFICATE;
         String driver = "";
         // Egine
         if (!engine.isEmpty()) {
            // Host
            if (host.isEmpty()) {
               if (!engine.equals("h2") && !engine.equals("sqlite")) {
                  host = "localhost";
               }
            }
            // Port
            if (port.isEmpty()) {
               if (engine.equals("mysql")) {
                  port = "3306";
               } else if (engine.equals("postgresql")) {
                  port = "5432";
               } else if (engine.equals("oracle")) {
                  port = "1521";
               }
            }
            // User
            if (user.isEmpty()) {
               if (engine.equals("mysql")) {
                  user = "root";
               } else if (engine.equals("postgresql")) {
                  user = "postgres";
               } else if (engine.equals("oracle")) {
                  user = "hr";
               } else if (engine.equals("h2")) {
                  user = "sa";
               }
            }
            // Database
            if (database.isEmpty()) {
               if (engine.equals("mysql")) {
                  database = "mysql";
               } else if (engine.equals("postgresql")) {
                  database = "postgres";
               } else if (engine.equals("oracle")) {
                  database = "xe";
               } else if (engine.equals("h2")) {
                  database = "test";
               }
            }
            // JDBC Drive and URL
            if (engine.equals("mysql")) {
               if (DATABASE_LOGGING) {
                  driver = "net.sf.log4jdbc.DriverSpy";
                  prefix = "jdbc:log4jdbc";
               } else {
                  driver = "com.mysql.jdbc.Driver";
               }
               url = String.format(
                  "%s:%s://%s:%s/%s?user=%s&password=%s",
                  prefix,
                  engine,
                  host,
                  port,
                  database,
                  user,
                  password);
            } else if (engine.equals("postgresql")) {
               driver = "org.postgresql.Driver";
            } else if (engine.equals("oracle")) {
               driver = "oracle.jdbc.driver.OracleDriver";
            } else if (engine.equals("h2")) {
               driver = "org.h2.Driver";
            }
            
            if (verifyServerCertificate != null) {
               url += String.format("&verifyServerCertificate=%s", verifyServerCertificate);
            }
            
            if (useSSL != null) {
               url += String.format("&useSSL=%s", useSSL);
            }
            
            if (autoReconnect != null) {
               url += String.format("&autoReconnect=%s", autoReconnect);
            }
            
            if (autoReconnectForPools != null) {
               url += String.format("&autoReconnectForPools=%s", autoReconnectForPools);
            }
            
            if (DATABASE_POOL_ENGINE.equals(C3P0)) {
               // Desabilita o log do c3p0
               Properties p = new Properties(System.getProperties());
               p.put("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
               p.put("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
               System.setProperties(p);
               // Creates the connection pool
               c3p0 = new ComboPooledDataSource();
               c3p0.setInitialPoolSize(DATABASE_INITIAL_POOL_SIZE);
               c3p0.setAcquireIncrement(DATABASE_ACQUIRE_INCREMENT);
               c3p0.setAcquireRetryAttempts(DATABASE_ACQUIRE_RETRY_ATTEMPTS);
               c3p0.setAcquireRetryDelay(DATABASE_ACQUIRE_RETRY_DELAY);
               c3p0.setMinPoolSize(DATABASE_MIN_POOL_SIZE);
               c3p0.setMaxPoolSize(DATABASE_MAX_POOL_SIZE);
               c3p0.setMaxStatements(DATABASE_MAX_STATEMENTS);
               
//               c3p0.setMaxIdleTime(DATABASE_MAX_IDLE_TIME);
               c3p0.setMaxIdleTime(DATABASE_POOL_HIKARI_IDLE_TIMEOUT);
               
               c3p0.setIdleConnectionTestPeriod(DATABASE_IDLE_TEST_PERIOD);
               c3p0.setCheckoutTimeout(DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT);
               c3p0.setAutoCommitOnClose(AUTO_COMMIT.isValue());
               c3p0.setUser(user);
               c3p0.setPassword(password);
               c3p0.setJdbcUrl(url);
               try {
                  c3p0.setDriverClass(driver);
               } catch (PropertyVetoException e) {
                  e.printStackTrace();
               }
            } else if (DATABASE_POOL_ENGINE.equals(HIKARI)) {
               hikari = new HikariDataSource();
               hikari.setDriverClassName(driver);
               hikari.setJdbcUrl(url.substring(0, url.indexOf("?")));
               hikari.setUsername(user);
               hikari.setPassword(password);
               
               hikari.addDataSourceProperty("cachePrepStmts", DATABASE_POOL_HIKARI_CACHE_PREPARED_STATEMENTS);
               hikari.addDataSourceProperty("useServerPrepStmts", DATABASE_POOL_HIKARI_USE_SERVER_PREPARED_STATEMENTS);
               hikari.addDataSourceProperty("prepStmtCacheSize", DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SIZE);
               hikari.addDataSourceProperty("prepStmtCacheSqlLimit", DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SQL_LIMIT);
               hikari.setAutoCommit(AUTO_COMMIT.value()); // default: true
               hikari.addDataSourceProperty("useUnicode", DATABASE_UNICODE);
               hikari.addDataSourceProperty("characterEncoding", DATABASE_CHARSET);
               hikari.setMinimumIdle(DATABASE_POOL_HIKARI_MINIMUM_IDLE); // default: 600000 (10 minutes)
               hikari.setConnectionTimeout(DATABASE_POOL_HIKARI_CONNECTION_TIMEOUT); // default: 30000 (30 seconds)
               hikari.setMaximumPoolSize(DATABASE_POOL_HIKARI_MAX_SIZE); // default: 10
               hikari.addDataSourceProperty("minimumPoolSize", DATABASE_POOL_HAKIRI_MIN_SIZE); // default: same as maximumPoolSize
               hikari.setMaxLifetime(DATABASE_POOL_HIKARI_MAX_LIFETIME); // default: 1800000 (30 minutes)
               hikari.addDataSourceProperty("maxLifetime", DATABASE_POOL_HIKARI_MAX_LIFETIME); // default: 1800000 (30 minutes)
               // Lowest acceptable value for enabling leak detection is 2000 (2 seconds).
               hikari.setLeakDetectionThreshold(DATABASE_POOL_HIKARI_LEAK_DETECTION); // default: 0 (disabled)
               hikari.setValidationTimeout(DATABASE_POOL_HIKARI_VALIDATION_TIMEOUT); // default: 5000
               hikari.addDataSourceProperty("autoReconnect", DATABASE_POOL_HIKARI_AUTO_RECONNECT);
               hikari.addDataSourceProperty("useLocalSessionState", DATABASE_POOL_HIKARI_USE_LOCAL_SESSION_STATE); // default :false
               hikari.addDataSourceProperty("useLocalTransactionState", DATABASE_POOL_HIKARI_USE_LOCAL_TRANSACTION_STATE); // default: false
               hikari.addDataSourceProperty("rewriteBatchedStatements", DATABASE_POOL_HIKARI_REWRITE_BATCHED_STAMENTS); // default: false
               hikari.addDataSourceProperty("cacheResultSetMetadata", DATABASE_POOL_HIKARI_CACHE_RESULTSET_METADATA); // default: false
               hikari.addDataSourceProperty("cacheServerConfiguration", DATABASE_POOL_HIKARI_CACHE_SERVER_CONFIGURATION); // default: false
               hikari.addDataSourceProperty("elideSetAutoCommits", DATABASE_POOL_HIKARI_ELIDE_SET_AUTO_COMMITS); // default: false
               hikari.addDataSourceProperty("maintainTimeStats", DATABASE_POOL_HIKARI_MAINTAIN_TIME_STATS); // default: false
               hikari.addDataSourceProperty("isolateInternalQueries", DATABASE_POOL_HIKARI_ISOLATE_INTERNAL_QUERIES); // default: false
               hikari.addDataSourceProperty("allowPoolSuspension", DATABASE_POOL_HIKARI_ALLOW_POOL_SUSPENSION); // default: false
               hikari.addDataSourceProperty("readOnly", DATABASE_POOL_HIKARI_READ_ONLY); // default: false
               hikari.addDataSourceProperty("registerMbeans", DATABASE_POOL_HIKARI_REGISTER_MBEANS); // default: false
//               hikari.setConnectionInitSql(DATABASE_POOL_CONNECTION_TEST_QUERY); // default: none
//               hikari.addDataSourceProperty("jdbc4ConnectionTest", DATABASE_POOL_CONNECTION_TEST);
//               hikari.setConnectionTestQuery(DATABASE_POOL_CONNECTION_TEST_QUERY);
            } else {
               
            }
         }
      }
   }
   
//   public static synchronized Connection getConnection() {
   public static Connection getConnection() {
      Connection connection = null;
      try {
         Class.forName("com.mysql.jdbc.Driver");
         if (JediEngine.Pool.equals(C3P0)) {
            connection = c3p0.getConnection();
         } else if (JediEngine.Pool.equals(HIKARI)) {
            connection = hikari.getConnection();
         } else {
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(JediEngine.AUTO_COMMIT.value());
         }
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return connection;
   }
   
   public static Connection connection() {
      return getConnection();
   }
   
   /**
    * @return Connection Returns a connection to a database.
    */
   public static Connection connect() {
      return getConnection();
   }
   
   /**
    * @param args
    * @return
    */
   public static Connection connect(String... args) {
      return getConnection(args);
   }
   
   /**
    * @param args
    * @return
    */
   public static Connection getConnection(String... args) {
      Connection connection = null;
      if (args != null && args.length > 0) {
         try {
            String databaseEngine = "";
            String databaseHost = "";
            String databasePort = "";
            String databaseUser = "";
            String databasePassword = "";
            String databaseName = "";
            String databaseOptionsAutocommit = "";
            for (int i = 0; i < args.length; i++) {
               args[i] = args[i].toLowerCase();
               args[i] = args[i].replace(" = ", "=");
               // Engine
               if (args[i].equals("engine=mysql")) {
                  Class.forName("com.mysql.jdbc.Driver");
                  databaseEngine = "mysql";
               } else if (args[i].equals("engine=postgresql")) {
                  Class.forName("org.postgresql.Driver");
                  databaseEngine = "postgresql";
               } else if (args[i].equals("engine=oracle")) {
                  Class.forName("oracle.jdbc.driver.OracleDriver");
                  databaseEngine = "oracle";
               } else if (args[i].equals("engine=sqlite")) {
                  Class.forName("org.sqlite.JDBC");
                  databaseEngine = "sqlite";
               } else if (args[i].equals("engine=h2")) {
                  databaseEngine = "h2";
                  Class.forName("org.h2.Driver");
               }
               // Host
               if (args[i].startsWith("host=")) {
                  if (args[i].split("=").length > 1) {
                     databaseHost = args[i].split("=")[1];
                  }
               }
               if (databaseHost != null && databaseHost.isEmpty() && !databaseEngine.equals("h2") &&
                  !databaseEngine.equals("sqlite")) {
                  databaseHost = "localhost";
               }
               // Port
               if (args[i].matches("port=\\d+")) {
                  databasePort = args[i].split("=")[1];
               }
               if (databasePort != null && databasePort.isEmpty()) {
                  if (databaseEngine.equals("mysql")) {
                     databasePort = "3306";
                  } else if (databaseEngine.equals("postgresql")) {
                     databasePort = "5432";
                  } else if (databaseEngine.equals("oracle")) {
                     databasePort = "1521";
                  }
               }
               // Database
               if (args[i].startsWith("database=")) {
                  if (args[i].split("=").length > 1) {
                     databaseName = args[i].split("=")[1];
                  }
               }
               if (databaseName != null && databaseName.isEmpty()) {
                  if (databaseEngine.equals("mysql")) {
                     databaseName = "mysql";
                  } else if (databaseEngine.equals("postgresql")) {
                     databaseName = "postgres";
                  } else if (databaseEngine.equals("oracle")) {
                     databaseName = "xe";
                  } else if (databaseEngine.equals("h2")) {
                     databaseName = "test";
                  }
               }
               // User
               if (args[i].startsWith("user=")) {
                  if (args[i].split("=").length > 1) {
                     databaseUser = args[i].split("=")[1];
                  }
               }
               if (databaseUser != null && databaseUser.isEmpty()) {
                  if (databaseEngine.equals("mysql")) {
                     databaseUser = "root";
                  } else if (databaseEngine.equals("postgresql")) {
                     databaseUser = "postgres";
                  } else if (databaseEngine.equals("oracle")) {
                     databaseUser = "hr";
                  } else if (databaseEngine.equals("h2")) {
                     databaseUser = "sa";
                  }
               }
               // Password
               if (args[i].startsWith("password=")) {
                  if (args[i].split("=").length > 1) {
                     databasePassword = args[i].split("=")[1];
                  }
               }
               if (databasePassword != null && databasePassword.isEmpty()) {
                  if (databaseEngine.equals("mysql")) {
                     databasePassword = "mysql";
                  } else if (databaseEngine.equals("postgresql")) {
                     databasePassword = "postgres";
                  } else if (databaseEngine.equals("oracle")) {
                     databasePassword = "hr";
                  } else if (databaseEngine.equals("h2")) {
                     databasePassword = "1";
                  }
               }
               if (args[i].startsWith("autocommit=")) {
                  if (args[i].split("=").length > 1) {
                     databaseOptionsAutocommit = args[i].split("=")[1];
                  }
               }
               args[i] = args[i].replace("=", " = ");
            }
            if (databaseEngine.equals("mysql")) {
               connection = DriverManager.getConnection(
                  String.format(
                     "jdbc:mysql://%s:%s/%s?user=%s&password=%s",
                     databaseHost,
                     databasePort,
                     databaseName,
                     databaseUser,
                     databasePassword));
            } else if (databaseEngine.equals("postgresql")) {
               connection = DriverManager.getConnection(
                  String.format(
                     "jdbc:postgresql://%s:%s/%s",
                     databaseHost,
                     databasePort,
                     databaseName),
                  databaseUser,
                  databasePassword);
            } else if (databaseEngine.equals("oracle")) {
               String sid = databaseName;
               String url = "jdbc:oracle:thin:@" + databaseHost + ":" + databasePort + ":" + sid;
               connection = DriverManager.getConnection(url, databaseUser, databasePassword);
            } else if (databaseEngine.equals("sqlite")) {
               Class.forName("org.sqlite.JDBC");
               connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName);
            } else if (databaseEngine.equals("h2")) {
               connection = DriverManager.getConnection(
                  String.format("jdbc:%s:~/%s", databaseEngine, databaseName),
                  databaseUser,
                  databasePassword);
            }
            if (connection != null) {
               if (!databaseOptionsAutocommit.isEmpty() &&
                  (databaseOptionsAutocommit.equalsIgnoreCase("true") ||
                     databaseOptionsAutocommit.equalsIgnoreCase("false"))) {
                  connection.setAutoCommit(Boolean.parseBoolean(databaseOptionsAutocommit));
               } else {
                  connection.setAutoCommit(false);
               }
            }
         } catch (SQLException e) {
            String msg =
               "Ocorreram uma ou mais falhas ao tentar obter uma conexão com o banco de dados.";
            System.out.println(msg);
            e.printStackTrace();
         } catch (ClassNotFoundException e) {
            System.out.println("O driver de conexão com o banco de dados não foi encontrado.");
            e.printStackTrace();
         }
      }
      return connection;
   }
   
}
