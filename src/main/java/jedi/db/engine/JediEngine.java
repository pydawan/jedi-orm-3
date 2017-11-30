package jedi.db.engine;

import static java.lang.String.format;
import static jedi.db.connection.DataSource.connect;
import static jedi.db.models.PoolEngine.C3P0;
import static jedi.db.models.PoolEngine.HIKARI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import jedi.app.JediApp;
import jedi.app.loader.JediAppLoader;
import jedi.db.CharSet;
import jedi.db.Collation;
import jedi.db.Privilege;
import jedi.db.connection.DataSource;
import jedi.db.exceptions.MultipleObjectsReturnedException;
import jedi.db.exceptions.ObjectDoesNotExistException;
import jedi.db.models.AutoClose;
import jedi.db.models.AutoCommit;
import jedi.db.models.BooleanField;
import jedi.db.models.CascadeType;
import jedi.db.models.CharField;
import jedi.db.models.DateField;
import jedi.db.models.DateTimeField;
import jedi.db.models.DecimalField;
import jedi.db.models.EmailField;
import jedi.db.models.ExceptionHandling;
import jedi.db.models.FetchType;
import jedi.db.models.FloatField;
import jedi.db.models.ForeignKeyField;
import jedi.db.models.IPAddressField;
import jedi.db.models.IntegerField;
import jedi.db.models.Manager;
import jedi.db.models.ManyToManyField;
import jedi.db.models.Model;
import jedi.db.models.Models;
import jedi.db.models.OneToOneField;
import jedi.db.models.PoolEngine;
import jedi.db.models.Query;
import jedi.db.models.QuerySet;
import jedi.db.models.Table;
import jedi.db.models.TextField;
import jedi.db.models.TimeField;
import jedi.db.models.TimestampField;
import jedi.db.models.URLField;
import jedi.db.util.ColumnUtil;
import jedi.db.util.TableUtil;
import jedi.generator.CodeGenerator;

/**
 * <p>
 * <strong>Jedi's Object-Relational Mapping Engine.</strong>
 * </p>
 *
 * @author thiago-amm
 * @version v1.0.0
 * @version v1.0.1 19/01/2017
 * @version v1.0.2 25/09/2017
 * @since v1.0.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class JediEngine {
   
   private static final File FILE = new File(".");
   public static final String REGEX_FILE_SEPARATOR = Matcher.quoteReplacement(File.separator);
   public static final String FILE_SEPARATOR = File.separator.replaceAll("\\\\", "/");
   public static final String PROJECT_DIR = FILE.getAbsolutePath().replace(".", "");
   public static final String PROJECT_DIR_JEDI_PROPERTIES = PROJECT_DIR + "jedi.properties";
   
   public static final String BIN_DIR = PROJECT_DIR + "bin/";
   public static final String BUILD_DIR = PROJECT_DIR + "build/";
   public static final String DOC_DIR = PROJECT_DIR + "doc/";
   public static final String SRC_DIR = PROJECT_DIR + "src/";
   public static final String LIB_DIR = PROJECT_DIR + "lib/";
   
   public static final String SRC_MAIN_DIR = SRC_DIR + "main/";
   public static final String SRC_MAIN_JAVA_DIR = SRC_MAIN_DIR + "java/";
   public static final String SRC_MAIN_RESOURCES_DIR = SRC_MAIN_DIR + "resources/";
   public static final String SRC_MAIN_RESOURCES_JEDI_PROPERTIES = SRC_MAIN_RESOURCES_DIR + "jedi.properties";
   public static final String SRC_MAIN_RESOURCES_SQL_DIR = SRC_MAIN_RESOURCES_DIR + "sql/";
   public static final String SRC_MAIN_RESOURCES_METAINF_DIR = SRC_MAIN_RESOURCES_DIR + "META-INF/";
   public static final String SRC_MAIN_WEBAPP_DIR = SRC_MAIN_DIR + "webapp/";
   public static final String SRC_MAIN_WEBAPP_METAINF_DIR = SRC_MAIN_WEBAPP_DIR + "META-INF/";
   public static final String SRC_MAIN_WEBAPP_WEBINF_DIR = SRC_MAIN_WEBAPP_DIR + "WEB-INF/";
   public static final String SRC_MAIN_WEBAPP_WEBINF__LIB_DIR = SRC_MAIN_WEBAPP_WEBINF_DIR + "lib/";
   public static final String SRC_MAIN_WEBAPP_WEBINF_JEDI_PROPERTIES = SRC_MAIN_WEBAPP_WEBINF_DIR + "jedi.properties";
   
   public static final String SRC_TEST_DIR = SRC_MAIN_DIR.replace("main", "test");
   public static final String SRC_TEST_JAVA_DIR = SRC_MAIN_JAVA_DIR.replace("main", "test");
   public static final String SRC_TEST_RESOURCES_DIR = SRC_MAIN_RESOURCES_DIR.replace("main", "test");
   public static final String SRC_TEST_RESOURCES_JEDI_PROPERTIES = SRC_TEST_RESOURCES_DIR + "jedi.properties";
   
   private static final File[] SQL_FILES = new File(SRC_MAIN_RESOURCES_SQL_DIR).listFiles();
   private static final Logger logger = Logger.getLogger(JediEngine.class.getName());
   private static final List<File> SCRIPTS = SQL_FILES == null ? new ArrayList<>(0) : Arrays.asList(SQL_FILES);
   
   public static final String WEBINF_CLASSES_JEDI_PROPERTIES = PROJECT_DIR + "WEB-INF/classes/jedi.properties";
   
   public static final String[] JEDI_PROPERTIES_PATHS = {
      PROJECT_DIR_JEDI_PROPERTIES, 
      SRC_MAIN_RESOURCES_JEDI_PROPERTIES,
      SRC_TEST_RESOURCES_JEDI_PROPERTIES, 
      SRC_MAIN_WEBAPP_WEBINF_JEDI_PROPERTIES, 
      WEBINF_CLASSES_JEDI_PROPERTIES
   };
   // Application's root directory.
   // public static String APP_ROOT_DIR = System.getProperty("user.dir");
   public static String APP_ROOT_DIR = JediEngine.class
      .getClassLoader()
      .getResource("jedi.properties")
      .getPath()
      .replace("/bin/jedi.properties", "")
      .replace("/src/jedi.properties", "")
      .replace("/jedi.properties", "");
   // Application's source code directory.
   public static String APP_BIN_DIR = APP_ROOT_DIR + "/bin";
   public static String APP_SRC_DIR = APP_ROOT_DIR + "/src";
   public static String APP_LIBS_DIR = APP_ROOT_DIR + "/lib";
   public static String JEDI_PROPERTIES_PATH = null;
   public static final String[] JEDI_PROPERTIES_LIKELY_PATHS = { 
      JEDI_PROPERTIES_PATH, APP_SRC_DIR + "/jedi.properties",
      APP_ROOT_DIR + "/WEB-INF/classes/jedi.properties", 
      APP_ROOT_DIR + "/WebContent/WEB-INF/jedi.properties",
      APP_ROOT_DIR + "/web/WEB-INF/jedi.properties", 
      APP_SRC_DIR + "/main/resources/jedi.properties",
      APP_SRC_DIR + "/main/webapp/WEB-INF/jedi.properties", 
      APP_SRC_DIR + "/test/resources/jedi.properties"
   };
   public static String TABLE = "";
   public static boolean DATABASE_ENVIRONMENTS = false;
   public static String DATABASE_ENVIRONMENTS_DEVELOPMENT = "development";
   public static String DATABASE_ENVIRONMENTS_TEST = "test";
   public static String DATABASE_ENVIRONMENTS_STAGE = "stage";
   public static String DATABASE_ENVIRONMENTS_PRODUCTION = "production";
   public static String DATABASE_ENGINE = "mysql";
   public static String DATABASE_HOST = "localhost";
   public static String DATABASE_PORT = "3306";
   public static String DATABASE_NAME = "";
   public static String DATABASE_USER = "";
   public static String DATABASE_PASSWORD = "";
   public static String DATABASE_CHARSET = "utf8";
   public static String DATABASE_COLLATE = "utf8_general_ci";
   public static ExceptionHandling EXCEPTION_HANDLING = ExceptionHandling.THROW;
   public static String SQL_COLUMN_IDENTATION = "    ";
   public static PoolEngine DATABASE_POOL_ENGINE = PoolEngine.JEDI;
   public static boolean PREFIX_APP_TABLE = true;
   public static AutoCommit AUTO_COMMIT = AutoCommit.YES;
   public static AutoClose AUTO_CLOSE = AutoClose.NO;
   public static boolean DATABASE_AUTO_INCREMENT = true;
   public static boolean FOREIGN_KEY_CHECKS = true;
   public static boolean WEB_APP = false;
   public static boolean GRADLE_WEB_APP = false;
   public static boolean MAVEN_WEB_APP = false;
   public static boolean DEBUG = false;
   public static boolean MAVEN_PROJECT = false;
   public static boolean GRADLE_PROJECT = true;
   public static boolean JEDI_PROPERTIES_LOADED = false;
   public static boolean CODE_GENERATION = true;
   public static boolean DATABASE_UNICODE = true;
   public static boolean DATABASE_LOGGING = false;
   public static boolean DATABASE_USE_SERVER_PREPARED_STATEMENTS = false;
   public static boolean DATABASE_POOL_HIKARI_CACHE_PREPARED_STATEMENTS = false;
   public static boolean DATABASE_POOL_HIKARI_USE_SERVER_PREPARED_STATEMENTS = false;
   public static boolean DATABASE_POOL_HIKARI_AUTO_RECONNECT = true;
   public static boolean DATABASE_POOL_HIKARI_CONNECTION_TEST = true;
   public static boolean DATABASE_POOL_HIKARI_USE_LOCAL_SESSION_STATE = false;
   public static boolean DATABASE_POOL_HIKARI_USE_LOCAL_TRANSACTION_STATE = false;
   public static boolean DATABASE_POOL_HIKARI_REWRITE_BATCHED_STAMENTS = false;
   public static boolean DATABASE_POOL_HIKARI_CACHE_RESULTSET_METADATA = false;
   public static boolean DATABASE_POOL_HIKARI_CACHE_SERVER_CONFIGURATION = false;
   public static boolean DATABASE_POOL_HIKARI_ELIDE_SET_AUTO_COMMITS = false;
   public static boolean DATABASE_POOL_HIKARI_MAINTAIN_TIME_STATS = false;
   public static boolean DATABASE_POOL_HIKARI_ISOLATE_INTERNAL_QUERIES = false;
   public static boolean DATABASE_POOL_HIKARI_ALLOW_POOL_SUSPENSION = false;
   public static boolean DATABASE_POOL_HIKARI_READ_ONLY = false;
   public static boolean DATABASE_POOL_HIKARI_REGISTER_MBEANS = false;
   
   // TODO - MAPEAR COMO PROPRIEDADES NO ARQUIVO jedi.properties.
   public static boolean DATABASE_POOL_HIKARI_TRANSACTION_ISOLATION = false;
   public static boolean DATABASE_POOL_HIKARI_DATASOURCE = false;
   public static boolean DATABASE_POOL_HIKARI_DATASOURCE_CLASS_NAME = false;
   public static boolean DATABASE_POOL_HIKARI_SCHEMA = false;
   public static boolean DATABASE_POOL_HIKARI_THREAD_FACTORY = false;
   public static boolean DATABASE_POOL_HIKARI_SCHEDULED_EXECUTOR = false;
   public static boolean DATABASE_POOL_HIKARI_JDBC_URL = false;
   
   public static Boolean DATABASE_USE_SSL;
   public static Boolean DATABASE_VERIFY_SERVER_CERTIFICATE;
   public static Boolean DATABASE_AUTO_RECONNECT;
   public static Boolean DATABASE_AUTO_RECONNECT_FOR_POOLS;
   public static Integer DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SQL_LIMIT = 2048;
   public static Integer DATABASE_ACQUIRE_INCREMENT = 1;
   public static Integer DATABASE_ACQUIRE_RETRY_ATTEMPTS = 5;
   public static Integer DATABASE_ACQUIRE_RETRY_DELAY = 500;
   public static Integer DATABASE_INITIAL_POOL_SIZE = 1;
   public static Integer DATABASE_MIN_POOL_SIZE = 1;
   public static Integer DATABASE_POOL_HAKIRI_MIN_SIZE = 1;
   public static Integer DATABASE_MAX_POOL_SIZE = 10;
   public static Integer DATABASE_POOL_HIKARI_MAX_SIZE = 10;
   public static Integer DATABASE_POOL_HIKARI_LEAK_DETECTION = 0;
   public static Integer DATABASE_MAX_STATEMENTS = 500;
   public static Integer DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SIZE = 500;
   public static Integer DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT = 10;
   public static Integer DATABASE_POOL_HIKARI_CONNECTION_TIMEOUT = 30000;
   public static Integer DATABASE_POOL_HIKARI_INITIALIZE_FAIL_TIMEOUT = 1;
//   public static Integer DATABASE_MAX_IDLE_TIME = 600000;
   public static Integer DATABASE_POOL_HIKARI_MINIMUM_IDLE = DATABASE_POOL_HIKARI_MAX_SIZE;
   public static Integer DATABASE_POOL_HIKARI_IDLE_TIMEOUT = 600000;
   public static Integer DATABASE_IDLE_TEST_PERIOD = 550;
   public static Integer DATABASE_MAX_CONNECTION_AGE = 1800000;
   public static Integer DATABASE_POOL_HIKARI_MAX_LIFETIME = 1800000;
   public static Integer DATABASE_POOL_HIKARI_VALIDATION_TIMEOUT = 5000;
   public static String DATABASE_POOL_HIKARI_CONNECTION_TEST_QUERY = "SELECT 1 FROM DUAL";
   public static FetchType FETCH_TYPE = FetchType.EAGER;
   public static CascadeType CASCADE_TYPE = CascadeType.ALL;
   public static List<String> INSTALLED_APPS = new ArrayList<>();
   // List of maps with table names and models.
   public static List<String> SQL_ASSOCIATION_TABLES = new ArrayList<>();
   public static List<String> SQL_CREATE_TABLES;
   public static Map<String, String> CREATE_TABLES;
   public static Map<String, List<String>> SQL_FOREIGN_KEYS = new HashMap<>();
   public static Map<String, List<String>> SQL_INDEXES = new HashMap<>();
   public static Map<String, List<String>> MYSQL_AUTO_NOW = new HashMap<>();
   public static Map<String, List<String>> MYSQL_AUTO_NOW_ADD = new HashMap<>();
   public static Map<String, List<String>> SQL_COMMENTS = new HashMap<>();
   public static Map<String, List<String>> APP_SQL = new HashMap<>();
   public static FileInputStream JEDI_PROPERTIES_FILE;
   public static Properties JEDI_PROPERTIES;
   public static final Class[] JEDI_FIELD_ANNOTATION_CLASSES = {
      CharField.class, 
      EmailField.class, 
      URLField.class, 
      IPAddressField.class,
      TextField.class, 
      IntegerField.class, 
      DecimalField.class, 
      FloatField.class, 
      BooleanField.class, 
      DateField.class, 
      TimeField.class,
      DateTimeField.class, 
      OneToOneField.class, 
      ForeignKeyField.class, 
      ManyToManyField.class
   };
   private static Manager SQLManager;
   private static Integer MYSQL_VERSION = null;
   
   static {
      SQLManager = new Manager(Model.class, false);
      JediEngine.loadJediProperties();
   }
   
   // Framework's model directory.
   public static final String JEDI_DB_MODELS = "jedi/db/models";
   // Application's models that were read and that will be mapped in tables.
   public static List<String> READED_APP_MODELS = new ArrayList<>();
   // Generated tables.
   public static List<String> GENERATED_TABLES = new ArrayList<>();
   
   public static boolean isWindows() {
      boolean isWindows = false;
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         isWindows = true;
      }
      return isWindows;
   }
   
   public static boolean isLinux() {
      boolean isWindows = false;
      if (System.getProperty("os.name").toLowerCase().contains("linux")) {
         isWindows = true;
      }
      return isWindows;
   }
   
//   BUG - causado pela perda da referência path quando o contexto local é a
//   esse método é perdido após o final da execução do mesmo.
//   É resolvido retornando a referência de entrada para o chamador.
//   public static void normalizeFilePath(String path) {
//      if (isWindows()) {
//         path = path.replace("\\", "/");
//         path = path.substring(0, 1).equals("/") ? path.substring(1) : path; 
//      }
//   }
   
   public static String normalizeFilePath(String path) {
      if (isWindows()) {
         path = path.replace("\\", "/");
         path = path.substring(0, 1).equals("/") ? path.substring(1) : path; 
      }
      return path;
   }
   
   /**
    * Converte um PATH do sistema de arquivos em um
    * CLASSPATH do Java.
    *
    * @param path
    *           PATH do sistema de arquivos
    * @return CLASSPATH do Java
    */
   public static String convertFilePathToClassPath(String path) {
      path = path == null ? "" : path.trim();
      if (!path.isEmpty()) {
         APP_SRC_DIR = normalizeFilePath(APP_SRC_DIR);
         path = normalizeFilePath(path);
         if (pathExists(APP_SRC_DIR)) {
            path = path.replace(APP_SRC_DIR, "");
            path = path.replace(APP_SRC_DIR + FILE_SEPARATOR, "");
         } else {
            path = path.replace(APP_ROOT_DIR, "");
            path = path.replace(APP_ROOT_DIR + FILE_SEPARATOR, "");
         }
         path = path.substring(0, 1).equals("/") ? path.substring(1) : path;
         path = path.replace(".class", "");
         path = path.replace(".java", "");
         path = path.replace(FILE_SEPARATOR, ".");
      }
      return path;
   }
   
   /**
    * Converte um CLASSPATH do Java em um PATH do
    * sistema de arquivos.
    *
    * @param path
    *           CLASSPATH do Java
    * @return PATH do sistema de arquivos
    */
   public static String convertClassPathToFilePath(String path) {
      path = path == null ? "" : path.trim();
      if (!path.isEmpty()) {
         APP_SRC_DIR = normalizeFilePath(APP_SRC_DIR);
         path = normalizeFilePath(path);
         path = path.replace(".", FILE_SEPARATOR);
         if (pathExists(APP_SRC_DIR)) {
            path = APP_SRC_DIR + FILE_SEPARATOR + path + ".java";
         } else {
            path = APP_ROOT_DIR + FILE_SEPARATOR + path + ".java";
            
         }
      }
      return path;
   }
   
   /**
    * Converte as classes de modelo armazenadas na árvore de diretórios
    * iniciada em path, em tabelas no banco de dados.
    *
    * @param path
    *           PATH do sistema de arquivos
    */
   public static void syncdb(String path) {
      CREATE_TABLES = new HashMap<>();
      SQL_CREATE_TABLES = new ArrayList<>();
      MYSQL_VERSION = JediEngine.getMySQLVersion();
      getSQLOfInstalledApps();
      // Get SQL from Jedi Models
      getSQL(path);
      /*
       * If the create table statements exists then probably exists
       * foreign keys, indexes, etc.
       * All the other things are related to it.
       */
      System.out.println();
      if (!SQL_CREATE_TABLES.isEmpty()) {
         System.out.println("-- Creating tables...\n");
         for (Entry<String, String> e : CREATE_TABLES.entrySet()) {
            System.out.printf("-- Creating table %s\n\n", e.getKey());
            JediEngine.execute(e.getValue());
         }
         for (String sql : SQL_ASSOCIATION_TABLES) {
            System.out.println("-- Creating association tables...\n");
            JediEngine.execute(sql);
         }
         System.out.println("-- Creating foreign keys...\n");
         for (Entry<String, List<String>> sql : SQL_FOREIGN_KEYS.entrySet()) {
            for (String fk : sql.getValue()) {
               JediEngine.execute(fk);
            }
         }
         System.out.println("-- Installing indexes...\n");
         for (Entry<String, List<String>> sql : SQL_INDEXES.entrySet()) {
            for (String ix : sql.getValue()) {
               JediEngine.execute(ix);
            }
         }
         System.out.println();
         if (CODE_GENERATION) {
            CodeGenerator.generateCode(path);
         }
      }
   }
   
   /**
    * Converte em tabelas as classes de modelo
    * presentes no código fonte da aplicação.
    */
   public static void syncdb() {
      syncdb(JediEngine.APP_SRC_DIR);
   }
   
   /**
    * @param file
    */
   public static void sqlall(File file) {
//      String path = file == null ? "" : file.getAbsolutePath();
      String path = file == null ? "" : normalizeFilePath(file.getAbsolutePath());
      CREATE_TABLES = new HashMap<>();
      SQL_CREATE_TABLES = new ArrayList<>();
      getSQLOfInstalledApps();
      getSQL(path);
      System.out.println();
      System.out.println("BEGIN;\n");
      if (!SQL_CREATE_TABLES.isEmpty()) {
         for (String sql : SQL_CREATE_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         for (String sql : SQL_ASSOCIATION_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         for (Entry<String, List<String>> sql : SQL_FOREIGN_KEYS.entrySet()) {
            for (String fk : sql.getValue()) {
               if (!fk.trim().isEmpty()) {
                  System.out.println(fk + ";");
               }
            }
         }
         System.out.println("\n");
         for (Entry<String, List<String>> sql : SQL_INDEXES.entrySet()) {
            for (String ix : sql.getValue()) {
               if (!ix.trim().isEmpty()) {
                  System.out.println(ix + ";");
               }
            }
         }
         System.out.println("\nCOMMIT;");
         System.out.println();
      }
   }
   
   /**
    * Gera como saída a instrução SQL correspondente a
    * cada classe de modelo encontrada no diretório corrente.
    */
   public static void sqlall() {
      sqlall(new File(JediEngine.APP_SRC_DIR));
   }
   
   public static void sqlall(String app) {
      CREATE_TABLES = new HashMap<>();
      SQL_CREATE_TABLES = new ArrayList<>();
      getSQLOfInstalledApp(app);
      System.out.println();
      System.out.println("BEGIN;\n");
      if (!SQL_CREATE_TABLES.isEmpty()) {
         for (String sql : SQL_CREATE_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         for (String sql : SQL_ASSOCIATION_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         for (Entry<String, List<String>> sql : SQL_FOREIGN_KEYS.entrySet()) {
            for (String fk : sql.getValue()) {
               if (!fk.trim().isEmpty()) {
                  System.out.println(fk + ";");
               }
            }
         }
         System.out.println("\n");
         for (Entry<String, List<String>> sql : SQL_INDEXES.entrySet()) {
            for (String ix : sql.getValue()) {
               if (!ix.trim().isEmpty()) {
                  System.out.println(ix + ";");
               }
            }
         }
         System.out.println("\nCOMMIT;");
         System.out.println();
      }
   }
   
   public static void sql(String app) {
      CREATE_TABLES = new HashMap<>();
      SQL_CREATE_TABLES = new ArrayList<>();
      getSQLOfInstalledApp(app);
      System.out.println("BEGIN;\n");
      if (!SQL_CREATE_TABLES.isEmpty()) {
         for (String sql : SQL_CREATE_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         System.out.println("\nCOMMIT;");
      }
   }
   
   public static void sql() {
      CREATE_TABLES = new HashMap<>();
      SQL_CREATE_TABLES = new ArrayList<>();
      getSQLOfInstalledApps();
      getSQL(JediEngine.APP_SRC_DIR);
      System.out.println("BEGIN;\n");
      if (!SQL_CREATE_TABLES.isEmpty()) {
         for (String sql : SQL_CREATE_TABLES) {
            if (!sql.trim().isEmpty()) {
               System.out.println(sql + ";\n");
            }
         }
         System.out.println("\nCOMMIT;");
      }
   }
   
   /**
    * Obtém os arquivos de modelo presentes
    * no código fonte da aplicação.
    *
    * @return model files
    */
   public static List<File> getModelFiles() {
      if (pathExists(APP_SRC_DIR)) {
         return getModelFiles(APP_SRC_DIR);
      }
      return getModelFiles(APP_ROOT_DIR);
   }
   
   /**
    * Obtém os arquivos de modelo presentes
    * na árvore de diretórios iniciada em path.
    * 
    * @param path
    *           path (caminho) do sistema de arquivos
    * @return lista de arquivos de modelo
    */
   public static List<File> getModelFiles(String path) {
      List<File> modelFiles = new ArrayList<>();
      // Reference to the application's directory.
      File appDir = new File(path);
      // Checks if the appDir exists.
      if (appDir != null && appDir.exists()) {
         // Checks if appDir is a directory and not a
         // file (both are referenced as a File object).
         if (appDir.isDirectory()) {
            // Gets the appDir content.
            File[] appDirContents = appDir.listFiles();
            // Search by app/models subdirectory in the appDir content.
            for (File appDirContent : appDirContents) {
               // Gets all directories named as "models"
               // (except the models directory of the Framework).
               if (!appDirContent.getAbsolutePath().contains(JediEngine.JEDI_DB_MODELS) &&
                  (appDirContent.getAbsolutePath().endsWith("model") ||
                   appDirContent.getAbsolutePath().endsWith("models") ||
                   appDirContent.getAbsolutePath().endsWith("modelo") ||
                   appDirContent.getAbsolutePath().endsWith("modelos"))) {
                  // Gets all files in app/models.
                  File[] appModelsFiles = appDirContent.listFiles();
                  for (File appModelFile : appModelsFiles) {
                     if (isJediModelFile(appModelFile)) {
                        modelFiles.add(appModelFile);
                     }
                  }
               } else {
//                  TODO - Possível BUG Jedi ORM carregamento Modelo no Windows
//                  TODO - https://pastebin.com/UG9Hrbkd
//                  modelFiles.addAll(getModelFiles(appDirContent.getAbsolutePath()));
                  modelFiles.addAll(getModelFiles(normalizeFilePath(appDirContent.getAbsolutePath())));
               }
            }
         }
      }
      return modelFiles;
   }
   
   /**
    * Obtém um arquivo de modelo a partir da classe de modelo.
    *
    * @param clazz
    *           classe de modelo
    * @return arquivo de modelo
    */
   public static File getModelFile(Class clazz) {
      File file = null;
      if (clazz != null) {
         String path = JediEngine.convertClassPathToFilePath(clazz.getName());
         file = new File(path);
      }
      return file;
   }
   
   /**
    * Obtém uma classe de modelo a partir de seu arquivo.
    *
    * @param file
    *           arquivo de modelo
    * @return file classe de modelo
    */
   public static Class<? extends jedi.db.models.Model> getModel(File file) {
      Class clazz = null;
      if (JediEngine.isJediModelFile(file)) {
         String className = JediEngine.convertFilePathToClassPath(file.getAbsolutePath());
         try {
            clazz = Class.forName(className);
         } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
         }
      }
      return clazz;
   }
   
   /**
    * @return models
    */
   public static List<Class<? extends jedi.db.models.Model>> getModels() {
      if (pathExists(APP_SRC_DIR)) {
         return getModels(APP_SRC_DIR);
      }
      return getModels(APP_ROOT_DIR);
   }
   
   /**
    * @param name
    * @return
    */
   public static Class<? extends jedi.db.models.Model> getModel(String name) {
      Class<? extends jedi.db.models.Model> clazz = null;
      name = name == null ? "" : name.trim();
      if (!name.isEmpty()) {
         for (Class model : JediEngine.getModels()) {
            if (model.getSimpleName().equalsIgnoreCase(name)) {
               clazz = model;
            } else {
               Table table = (Table) model.getAnnotation(Table.class);
               if (table != null) {
                  // if (model.getSimpleName().equalsIgnoreCase(table.name())) {
                  if (name.equalsIgnoreCase(table.name())) {
                     clazz = model;
                  }
               }
            }
         }
      }
      return clazz;
   }
   
   /**
    * Obtém as classes de modelo armazenadas na árvore de diretórios
    * do caminho informado.
    *
    * @param path
    *           path (caminho) do diretório raiz da árvore de diretórios.
    * @return models lista de modelos encontrados.
    */
   public static List<Class<? extends jedi.db.models.Model>> getModels(String path) {
      List<Class<? extends jedi.db.models.Model>> models = new ArrayList<>();
      path = path == null ? "" : path.trim();
      if (!path.isEmpty() && new File(path).exists()) {
         Class modelClass = null;
         for (File file : JediEngine.getModelFiles(path)) {
            modelClass = JediEngine.getModel(file);
            if (!isJediModel(modelClass)) {
               continue;
            }
            models.add(modelClass);
         }
      }
      return models;
   }
   
   /**
    * Obtém o código SQL de cada classe de modelo
    * localizada na árvore de diretórios do caminho informado.
    * 
    * @param path
    *           path
    */
   private static void getSQL(String path) {
      for (Class clazz : JediEngine.getModels(path)) {
         if (SQL_CREATE_TABLES != null) {
            String sql = getSQL(clazz);
            SQL_CREATE_TABLES.add(sql);
            String tableName = TableUtil.getTableName(clazz);
            if (CREATE_TABLES != null) {
               CREATE_TABLES.put(tableName, sql);
            }
            if (!GENERATED_TABLES.contains(tableName)) {
               GENERATED_TABLES.add(tableName);
            }
         }
      }
   }
   
   /**
    * Delete as tabelas informadas.
    *
    * @param tables
    *           tables
    */
   public static void droptables(String... tables) {
      boolean foreignKeyChecks = FOREIGN_KEY_CHECKS;
      setForeignKeyChecks(false);
      if (tables != null && tables.length > 0) {
         System.out.println("-- Dropping tables...\n");
         for (String table : tables) {
            System.out.println(String.format("-- Dropping table %s\n", table));
            JediEngine.execute(String.format("DROP TABLE %s", table));
         }
      }
      setForeignKeyChecks(foreignKeyChecks);
   }
   
   /**
    * Deleta as tabelas informadas.
    *
    * @param tables
    *           tables
    */
   public static void droptables(List<String> tables) {
      boolean foreignKeyChecks = FOREIGN_KEY_CHECKS;
      setForeignKeyChecks(false);
      if (tables != null && tables.size() > 0) {
         System.out.println("-- Dropping tables...\n");
         for (String table : tables) {
            System.out.println(String.format("-- Dropping table %s\n", table));
            JediEngine.execute(String.format("DROP TABLE %s", table));
         }
      }
      setForeignKeyChecks(foreignKeyChecks);
   }
   
   /**
    * drop tables.
    */
   public static void droptables() {
      if (JediEngine.GENERATED_TABLES.isEmpty()) {
         droptables(JediEngine.getTables());
      } else {
         droptables(JediEngine.GENERATED_TABLES);
      }
   }
   
   /**
    * drop tables.
    */
   public static void droptables(String app) {
      app = app == null ? "" : app.trim();
      if (app.isEmpty()) {
         if (JediEngine.GENERATED_TABLES.isEmpty()) {
            droptables(JediEngine.getTables(app));
         } else {
            droptables(JediEngine.GENERATED_TABLES);
         }
      }
   }
   
   private static void truncateTable(String table) {
      table = table == null ? "" : table.trim();
      if (!table.isEmpty()) {
         System.out.println(String.format("-- Truncating table %s\n", table));
         JediEngine.execute(String.format("TRUNCATE TABLE %s", table));
      }
   }
   
   public static void truncateTables(String... tables) {
      boolean foreignKeyChecks = FOREIGN_KEY_CHECKS;
      setForeignKeyChecks(false);
      if (tables != null && tables.length > 0) {
         System.out.println("-- Truncating tables...\n");
         for (String table : tables) {
            truncateTable(table);
         }
      }
      setForeignKeyChecks(foreignKeyChecks);
   }
   
   public static void truncateTables(List<String> tables) {
      boolean foreignKeyChecks = FOREIGN_KEY_CHECKS;
      setForeignKeyChecks(false);
      if (tables != null && tables.size() > 0) {
         System.out.println("-- Truncating tables...\n");
         for (String table : tables) {
            truncateTable(table);
         }
      }
      setForeignKeyChecks(foreignKeyChecks);
   }
   
   public static void truncateTables() {
      if (JediEngine.GENERATED_TABLES.isEmpty()) {
         truncateTables(JediEngine.getTables());
      } else {
         truncateTables(JediEngine.GENERATED_TABLES);
      }
   }
   
   /**
    * Recria toda a estrutura de banco de dados.
    */
   public static void resetdb() {
      System.out.println("-- Reseting the database schema...");
      droptables();
      syncdb();
   }
   
   public static void recreateTables() {
      System.out.println("-- Recreating the database schema...");
      droptables();
      syncdb();
   }
   
   /**
    * Mostra as instruções SQL que excluem todas as tabelas criadas
    * para todas as aplicações.
    */
   public static void sqlclear() {
      List<String> tables = JediEngine.getTables();
      if (tables != null && tables.size() > 0) {
         System.out.println();
         for (String table : tables) {
            String statement = String.format("DROP TABLE %s;", table);
            System.out.println(statement);
         }
         System.out.println();
      }
   }
   
   /**
    * Mostra as instruções SQL que excluem todas as tabelas
    * criadas para uma aplicação.
    * 
    * @param app
    *           nome da aplicação
    */
   public static void sqlclear(String app) {
      List<String> tables = JediEngine.getTables();
      if (tables != null && tables.size() > 0) {
         String prefix = String.format("%s_", app);
         System.out.println();
         for (String table : tables) {
            if (table.startsWith(prefix)) {
               String statement = String.format("DROP TABLE %s;", table);
               System.out.println(statement);
               break;
            }
         }
         System.out.println();
      }
   }
   
   /**
    * Imprime as instruções DELETE para cada tabela criada.
    */
   public static void sqlflush() {
      List<String> tables = JediEngine.getTables();
      if (tables != null && tables.size() > 0) {
         System.out.println();
         for (String table : tables) {
            String statement = String.format("DELETE FROM TABLE %s;", table);
            System.out.println(statement);
         }
         System.out.println();
      }
   }
   
   public static void sqlflush(String app) {
      List<String> tables = JediEngine.getTables();
      if (tables != null && tables.size() > 0) {
         String prefix = String.format("%s_", app);
         System.out.println();
         for (String table : tables) {
            if (table.startsWith(prefix)) {
               String statement = String.format("DELETE FROM TABLE %s;", table);
               System.out.println(statement);
            }
         }
         System.out.println();
      }
   }
   
   /**
    * Mostra as instruções SQL referentes aos índices criados
    * para todas as aplicações.
    */
   public static void sqlindexes() {
      // TODO
      // Uma das deficiências encontradas é não conseguir obter os indexes para
      // UNIQUE.
   }
   
   /**
    * Mostra as instruções SQL referentes aos índices criados
    * para uma aplicação específica.
    * 
    * @param app
    *           nome da aplicação
    */
   public static void sqlindexes(String app) {
      // TODO
   }
   
   /**
    * Mostra as instruções SQL que excluem os índices criados
    * para todas as aplicações.
    */
   public static void sqldropindexes() {
      // TODO
   }
   
   /**
    * Mostra as instruções SQL que excluem os índices criados
    * para uma aplicação específica.
    * 
    * @param app
    *           nome da aplicação
    */
   public static void sqldropindexes(String app) {
      // TODO
   }
   
   /**
    * Executa o código SQL (Structured Query Language) informado.
    *
    * @param sql
    *           SQL
    */
   public static void execute(String sql) {
      if (sql != null && !sql.trim().isEmpty()) {
         SQLManager.raw(sql);
      }
   }
   
   public static void executeSQL(String sql) {
      execute(sql);
   }
   
   public static void execute(StringBuilder sql) {
      if (sql != null && !sql.toString().isEmpty()) {
         JediEngine.execute(sql.toString());
      }
   }
   
   public static void executeSQL(StringBuilder sql) {
      execute(sql);
   }
   
   /**
    * Executa um script SQL informado.
    * 
    * @param file
    */
   public static void execute(File script) {
      SQLManager.execute(script);
   }
   
   public static void executeSQL(File sqlScript) {
      execute(sqlScript);
   }
   
   /**
    * Habilita ou desabilita a verificação das chaves estrangeiras
    * ou de integridade referencial.
    */
   public static void setForeignKeyChecks(boolean checks) {
      if (DATABASE_ENGINE.isEmpty()) {
         loadJediProperties();
      }
      if (JediEngine.DATABASE_ENGINE.equals("mysql")) {
         System.out.println();
         FOREIGN_KEY_CHECKS = checks;
         if (checks) {
            // Enable
            System.out.println("-- Enabling foreign key constraint.\n");
            JediEngine.execute("SET FOREIGN_KEY_CHECKS = 1");
         } else {
            // Disable
            System.out.println("-- Disabling foreign key constraint\n");
            JediEngine.execute("SET FOREIGN_KEY_CHECKS = 0");
         }
      }
   }
   
   /**
    * Cria as tabelas no banco de dados correspondentes
    * aos modelos das aplicações instaladas.
    */
   private static void getSQLOfInstalledApps() {
      JediAppLoader.setDir(APP_LIBS_DIR);
      for (String installedApp : INSTALLED_APPS) {
         JediApp app = JediAppLoader.get(installedApp);
         if (app == null) {
            continue;
         }
         List<Class<?>> classes = (List<Class<?>>) app.getClasses().get("models");
         for (Class<?> clazz : classes) {
            Class<? extends Model> modelClass = (Class<? extends Model>) clazz;
            if (SQL_CREATE_TABLES != null) {
               String tableName = TableUtil.getTableName(modelClass);
               String sql = "";
               if (PREFIX_APP_TABLE) {
                  sql = JediEngine.getSQL(app, modelClass);
               } else {
                  sql = JediEngine.getSQL(modelClass);
               }
               SQL_CREATE_TABLES.add(sql);
               if (CREATE_TABLES != null) {
                  CREATE_TABLES.put(tableName, sql);
               }
               if (!GENERATED_TABLES.contains(tableName)) {
                  GENERATED_TABLES.add(tableName);
               }
            }
         }
      }
   }
   
   /**
    * Carrega o SQL correspondente a cada classe de modelo
    * da aplicação informada.
    * 
    * @param appName
    *           nome da aplicação
    */
   private static void getSQLOfInstalledApp(String appName) {
      appName = appName == null ? "" : appName.trim();
      if (!appName.isEmpty()) {
         JediAppLoader.setDir(APP_LIBS_DIR);
         for (String installedApp : INSTALLED_APPS) {
            if (installedApp.endsWith(appName)) {
               JediApp app = JediAppLoader.get(installedApp);
               List<Class<?>> classes = (List<Class<?>>) app.getClasses().get("models");
               for (Class<?> clazz : classes) {
                  Class<? extends Model> modelClass = (Class<? extends Model>) clazz;
                  if (SQL_CREATE_TABLES != null) {
                     String tableName = TableUtil.getTableName(modelClass);
                     tableName = String.format("%s_%s", app.getDBTable(), tableName);
                     String sql = JediEngine.getSQL(app, modelClass);
                     SQL_CREATE_TABLES.add(sql);
                     if (CREATE_TABLES != null) {
                        CREATE_TABLES.put(tableName, sql);
                     }
                     if (!GENERATED_TABLES.contains(tableName)) {
                        GENERATED_TABLES.add(tableName);
                     }
                  }
               }
               return;
            }
         }
      }
   }
   
   private static int getMySQLVersion() {
      if (DATABASE_ENGINE.equals("mysql")) {
         if (MYSQL_VERSION == null) {
            Manager manager = new Manager(Model.class);
            String[] array = manager.raw("SELECT VERSION()").get(0).get(0).get("VERSION()").toString().split("\\.");
            MYSQL_VERSION = Integer.parseInt(String.format("%s%s", array[0], array[1]));
         }
      } else {
         MYSQL_VERSION = null;
      }
      return MYSQL_VERSION;
   }
   
   private static String getComment(Annotation annotation) {
      String co = null;
      Class ac = annotation == null ? null : annotation.annotationType();
      if (isJediFieldAnnotation(ac)) {
         if (ac == CharField.class) {
            co = ((CharField) annotation).comment();
         } else if (ac == EmailField.class) {
            co = ((EmailField) annotation).comment();
         } else if (ac == URLField.class) {
            co = ((URLField) annotation).comment();
         } else if (ac == IPAddressField.class) {
            co = ((IPAddressField) annotation).comment();
         } else if (ac == TextField.class) {
            co = ((TextField) annotation).comment();
         } else if (ac == IntegerField.class) {
            co = ((IntegerField) annotation).comment();
         } else if (ac == DecimalField.class) {
            co = ((DecimalField) annotation).comment();
         } else if (ac == FloatField.class) {
            co = ((FloatField) annotation).comment();
         } else if (ac == BooleanField.class) {
            co = ((BooleanField) annotation).comment();
         } else if (ac == DateField.class) {
            co = ((DateField) annotation).comment();
         } else if (ac == TimeField.class) {
            co = ((TimeField) annotation).comment();
         } else if (ac == DateTimeField.class) {
            co = ((DateTimeField) annotation).comment();
            // if (DATABASE_ENGINE.equals("mysql")) {
            // co = co.isEmpty() ? "" : String.format(" COMMENT '%s'", co);
            // }
         } else if (ac == OneToOneField.class) {
            co = ((OneToOneField) annotation).comment();
         } else if (ac == ForeignKeyField.class) {
            co = ((ForeignKeyField) annotation).comment();
         } else if (ac == ManyToManyField.class) {
            co = ((ManyToManyField) annotation).comment();
         } else {
            
         }
      }
      return co == null ? "" : co.trim();
   }
   
   public static String getDefaultValue(Annotation annotation) {
      String dv = null;
      Class ac = annotation == null ? null : annotation.annotationType();
      if (isJediFieldAnnotation(ac)) {
         if (ac == CharField.class) {
            dv = ((CharField) annotation).default_value();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == EmailField.class) {
            dv = ((EmailField) annotation).default_value();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == URLField.class) {
            dv = ((URLField) annotation).default_value();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == IPAddressField.class) {
            dv = ((IPAddressField) annotation).default_value();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == TextField.class) {
            dv = ((TextField) annotation).default_value();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == IntegerField.class) {
            dv = ((IntegerField) annotation).default_value();
            dv = dv == null || dv.trim().isEmpty() ? "" : dv.trim();
         } else if (ac == DecimalField.class) {
            dv = ((DecimalField) annotation).default_value();
            dv = dv == null || dv.isEmpty() ? "" : dv.trim();
         } else if (ac == FloatField.class) {
            dv = ((FloatField) annotation).default_value();
            dv = dv == null || dv.isEmpty() ? "" : dv.trim();
         } else if (ac == BooleanField.class) {
            dv = ((BooleanField) annotation).default_value() + "";
            dv = dv.equals("false") ? "0" : "1";
         } else if (ac == DateField.class) {
            dv = ((DateField) annotation).default_value();
            dv = dv == null ? "" : dv.trim();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == TimeField.class) {
            dv = ((TimeField) annotation).default_value();
            dv = dv == null ? "" : dv.trim();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else if (ac == DateTimeField.class) {
            dv = ((DateTimeField) annotation).default_value();
            dv = dv == null ? "" : dv.trim();
            dv = dv.equalsIgnoreCase("null") ? dv.toUpperCase() : dv;
         } else {
            
         }
      }
      return dv;
   }
   
   public static int getPrecision(Annotation annotation) {
      int precision = 0;
      Class ac = annotation == null ? null : annotation.annotationType();
      if (ac == TimeField.class) {
         precision = ((TimeField) annotation).precision();
      } else if (ac == DateTimeField.class) {
         precision = ((DateTimeField) annotation).precision();
      } else if (ac == TimestampField.class) {
         precision = ((TimestampField) annotation).precision();
      } else {
         
      }
      if (precision < 0) {
         precision = 0;
      }
      if (precision > 6) {
         precision = 6;
      }
      return precision;
   }
   
   private static String getSQLFormatter(Annotation annotation, String databaseEngine) {
      String formatter = null;
      Class ac = annotation == null ? null : annotation.annotationType();
      if (databaseEngine != null && !databaseEngine.isEmpty()) {
         if (databaseEngine.equalsIgnoreCase("mysql")) {
            if (ac == CharField.class || ac == EmailField.class || ac == URLField.class || ac == IPAddressField.class) {
               formatter = "%s VARCHAR(%d)%s%s%s%s";
            } else if (ac == TextField.class) {
               formatter = "%s TEXT%s%s%s%s";
            } else if (ac == IntegerField.class) {
               formatter = "%s INT(%d)%s%s%s%s";
            } else if (ac == DecimalField.class) {
               formatter = "%s DECIMAL(%d,%d)%s%s%s%s";
            } else if (ac == FloatField.class) {
               formatter = "%s FLOAT(%d,%d)%s%s%s%s";
            } else if (ac == BooleanField.class) {
               formatter = "%s TINYINT(1)%s%s%s%s";
            } else if (ac == DateField.class) {
               formatter = "%s DATE%s%s%s%s";
            } else if (ac == TimeField.class) {
               formatter = "%s TIME%s%s%s%s";
            } else if (ac == DateTimeField.class) {
               formatter = "%s DATETIME%s%s%s%s";
            } else {
               
            }
         }
      }
      return formatter == null ? "" : formatter.trim();
   }
   
   @Deprecated
   public static void _setJediPropertiesPath() {
      // Percorre os caminhos prováveis do arquivo jedi.properties.
      for (String path : JediEngine.JEDI_PROPERTIES_LIKELY_PATHS) {
         File file = new File(path);
         if (file.exists()) {
            JediEngine.JEDI_PROPERTIES_PATH = path;
            break;
         }
      }
   }
   
   public static File getJediProperties() {
      for (String path : JEDI_PROPERTIES_PATHS) {
//         System.out.println(path);
         File file = new File(path);
         if (file.exists()) {
            JEDI_PROPERTIES_PATH = path;
            return new File(path);
         }
      }
      return null;
   }
   
   public static void loadJediProperties() {
      try {
         String exceptionMessage = "";
         JEDI_PROPERTIES_PATH = JediEngine.class.getClassLoader().getResource("jedi.properties").getPath();
         JEDI_PROPERTIES_PATH = URLDecoder.decode(JEDI_PROPERTIES_PATH, System.getProperty("file.encoding"));
         File jediProperties = new File(JEDI_PROPERTIES_PATH);
         if (jediProperties.exists()) {
            JEDI_PROPERTIES_FILE = new FileInputStream(jediProperties);
         } else {
            File file = getJediProperties();
            if (file != null) {
               JEDI_PROPERTIES_PATH = file.getCanonicalPath();
               JEDI_PROPERTIES_FILE = new FileInputStream(file);
            } else {
               exceptionMessage = "ATENÇÃO: Não foi possível encontrar o arquivo de configurações jedi.properties!";
               throw new IllegalStateException(exceptionMessage);
            }
         }
         JEDI_PROPERTIES = new Properties();
         JEDI_PROPERTIES.load(JEDI_PROPERTIES_FILE);
         for (Object o : JEDI_PROPERTIES.keySet()) {
            String key = o.toString().toLowerCase().trim();
            String value = JEDI_PROPERTIES.getProperty(key);
            value = value == null ? "" : value.trim().toLowerCase();
            if (key.equals("database.environment") || 
                key.equals("database.env") || 
                key.equals("environment")) {
               DATABASE_ENVIRONMENTS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("db.environment") || 
                  key.equals("db.env") || 
                  key.equals("env")) {
               DATABASE_ENVIRONMENTS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("database.environment.development") || 
                  key.equals("database.environment.dev") || 
                  key.equals("environment.development") || 
                  key.equals("environment.dev")) {
               DATABASE_ENVIRONMENTS_DEVELOPMENT = value;
            } else if (
                  key.equals("database.env.development") || 
                  key.equals("database.env.dev") || 
                  key.equals("env.development") || 
                  key.equals("env.dev")) {
               DATABASE_ENVIRONMENTS_DEVELOPMENT = value;
            } else if (
                  key.equals("db.environment.development") || 
                  key.equals("db.environment.dev")) {
               DATABASE_ENVIRONMENTS_DEVELOPMENT = value;
            } else if (
                  key.equals("db.env.development") || 
                  key.equals("db.env.dev")) {
               DATABASE_ENVIRONMENTS_DEVELOPMENT = value;
            } else if (
                  key.equals("database.environment.test") || 
                  key.equals("environment.test")) {
               DATABASE_ENVIRONMENTS_TEST = value;
            } else if (
                  key.equals("database.env.test") || 
                  key.equals("env.test")) {
               DATABASE_ENVIRONMENTS_TEST = value;
            } else if (
                  key.equals("db.environment.test") || 
                  key.equals("db.env.test")) {
               DATABASE_ENVIRONMENTS_TEST = value;
            } else if (
                  key.equals("database.environment.stage") || 
                  key.equals("environment.stage")) {
               DATABASE_ENVIRONMENTS_STAGE = value;
            } else if (
                  key.equals("database.env.stage") || 
                  key.equals("env.stage")) {
               DATABASE_ENVIRONMENTS_STAGE = value;
            } else if (
                  key.equals("db.environment.stage") || 
                  key.equals("db.env.stage")) {
               DATABASE_ENVIRONMENTS_STAGE = value;
            } else if (
                  key.equals("database.environment.production") || 
                  key.equals("database.environment.prod") ||
                  key.equals("environment.production") || 
                  key.equals("environment.prod")) {
               DATABASE_ENVIRONMENTS_PRODUCTION = value;
            } else if (
                  key.equals("database.env.production") || 
                  key.equals("database.env.prod") ||
                  key.equals("env.production") || 
                  key.equals("env.prod")) {
               DATABASE_ENVIRONMENTS_PRODUCTION = value;
            } else if (
                  key.equals("db.environment.production") || 
                  key.equals("db.environment.prod")) {
               DATABASE_ENVIRONMENTS_PRODUCTION = value;
            } else if (
                  key.equals("db.env.production") || 
                  key.equals("db.env.prod")) {
               DATABASE_ENVIRONMENTS_PRODUCTION = value;
            } else if (
                  key.equals("engine") || 
                  key.equals("db.engine") || 
                  key.equals("database.engine")) {
               DATABASE_ENGINE = value;
            } else if (
                  key.equals("host") || 
                  key.equals("db.host") || 
                  key.equals("database.host")) {
               DATABASE_HOST = value;
            } else if (
                  key.equals("port") || 
                  key.equals("db.port") || 
                  key.equals("database.port")) {
               DATABASE_PORT = value;
            } else if (
                  key.equals("db") || 
                  key.equals("database") || 
                  key.equals("db.database") || 
                  key.equals("db.name") || 
                  key.equals("database.name")) {
               DATABASE_NAME = value;
               Set<Object> keySet = JEDI_PROPERTIES.keySet(); 
               if (!keySet.contains("user") && 
                   !keySet.contains("db.user") && 
                   !keySet.contains("database.user") &&
                   !keySet.contains("usr") && 
                   !keySet.contains("db.usr") && 
                   !keySet.contains("database.usr")) {
                  DATABASE_USER = value;
               }
               if (!keySet.contains("password") && 
                   !keySet.contains("db.password") && 
                   !keySet.contains("database.password") &&
                   !keySet.contains("passwd") && 
                   !keySet.contains("db.passwd") && 
                   !keySet.contains("database.passwd")) {
                  DATABASE_PASSWORD = value;
               }
            } else if (
                  key.equals("user") || 
                  key.equals("db.user") || 
                  key.equals("database.user") ||
                  key.equals("usr") || 
                  key.equals("db.usr") || 
                  key.equals("database.usr")) {
               DATABASE_USER = value;
            } else if (
                  key.equals("password") || 
                  key.equals("db.password") || 
                  key.equals("database.password") ||
                  key.equals("passwd") || 
                  key.equals("db.passwd") || 
                  key.equals("database.passwd")) {
               DATABASE_PASSWORD = value;
            } else if (
                  key.equals("pool.commit.auto") || 
                  key.equals("db.pool.commit.auto") ||
                  key.equals("database.pool.commit.auto")) {
               AUTO_COMMIT = AutoCommit.of(value);
            } else if (
                  key.equals("pool.commit") || 
                  key.equals("db.pool.commit") || 
                  key.equals("database.pool.commit")) {
               AUTO_COMMIT = value.equals("auto") ? AutoCommit.YES : AutoCommit.NO;
            } else if (
                  key.equals("pool.autocommit") ||
                  key.equals("db.pool.autocommit") ||
                  key.equals("database.pool.autocommit")) {
                  boolean autoCommit = Boolean.parseBoolean(value);
                  if (autoCommit) {
                     AUTO_COMMIT = AutoCommit.YES;
                  } else {
                     AUTO_COMMIT = AutoCommit.NO;
                  }
            } else if (
                  key.equals("pool.close.auto") || 
                  key.equals("db.pool.close.auto") || 
                  key.equals("database.pool.close.auto")) {
               AUTO_CLOSE = AutoClose.of(value);
            } else if (
                  key.equals("pool.close") || 
                  key.equals("db.pool.close") || 
                  key.equals("database.pool.close")) {
               AUTO_CLOSE = value.equals("auto") ? AutoClose.YES : AutoClose.NO;
            } else if (
                  key.equals("pool.autoclose") ||
                  key.equals("db.pool.autoclose") ||
                  key.equals("database.pool.autoclose")) {
                  boolean autoClose = Boolean.parseBoolean(value);
                  if (autoClose) {
                     AUTO_CLOSE = AutoClose.YES;
                  } else {
                     AUTO_CLOSE = AutoClose.NO;
                  }
            } else if (
                  key.equals("pool") || 
                  key.equals("db.pool") || 
                  key.equals("database.pool") ||
                  key.equals("pool.engine") || 
                  key.equals("db.pool.engine") || 
                  key.equals("database.pool.engine")) {
               if (value.equals("c3p0")) {
                  DATABASE_POOL_ENGINE = PoolEngine.C3P0;
               } else if (value.equals("hikari")) {
                  DATABASE_POOL_ENGINE = PoolEngine.HIKARI;
               } else if (value.equals("jedi")) {
                  DATABASE_POOL_ENGINE = PoolEngine.JEDI;
               } else {
                  exceptionMessage = "ATENÇÃO: A propriedade pool ou pool.engine ";
                  exceptionMessage += "de jedi.properties só admite os valores c3p0, hikari ou jedi!";
                  throw new IllegalArgumentException(exceptionMessage);
               }
            } else if (
                  key.equals("pool.inc") ||
                  key.equals("pool.increment") ||
                  key.equals("pool.size.inc") ||
                  key.equals("pool.size.increment") ||
                  key.equals("db.pool.inc") ||
                  key.equals("db.pool.increment") ||
                  key.equals("db.pool.size.inc") ||
                  key.equals("db.pool.size.increment") ||
                  key.equals("database.pool.inc") ||
                  key.equals("database.pool.increment") ||
                  key.equals("database.pool.size.inc") ||
                  key.equals("database.pool.size.increment")) {
               DATABASE_ACQUIRE_INCREMENT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.ini") ||
                  key.equals("pool.initial") ||
                  key.equals("pool.size.ini") ||
                  key.equals("pool.size.initial") ||
                  key.equals("db.pool.ini") ||
                  key.equals("db.pool.initial") ||
                  key.equals("db.pool.size.ini") ||
                  key.equals("db.pool.size.initial") ||
                  key.equals("database.pool.ini") ||
                  key.equals("database.pool.initial") ||
                  key.equals("database.pool.size.ini") ||
                  key.equals("database.pool.size.initial")) {
               DATABASE_INITIAL_POOL_SIZE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.max") ||
                  key.equals("pool.maximum") ||
                  key.equals("db.pool.max") ||
                  key.equals("db.pool.maximum") ||
                  key.equals("database.pool.max") ||
                  key.equals("database.pool.maximum") ||
                  key.equals("pool.size.max") ||
                  key.equals("pool.size.maximum") ||
                  key.equals("db.pool.size.max") ||
                  key.equals("db.pool.size.maximum") ||
                  key.equals("database.pool.size.max") ||
                  key.equals("database.pool.size.maximum")) {
               DATABASE_MAX_POOL_SIZE = Integer.parseInt(value);
               DATABASE_POOL_HIKARI_MAX_SIZE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.min") ||
                  key.equals("pool.minimum") ||
                  key.equals("db.pool.min") ||
                  key.equals("db.pool.minimum") ||
                  key.equals("database.pool.min") ||
                  key.equals("database.pool.minimum") ||
                  key.equals("pool.size.min") ||
                  key.equals("pool.size.minimum") ||
                  key.equals("db.pool.size.min") ||
                  key.equals("db.pool.size.minimum") ||
                  key.equals("database.pool.size.min") ||
                  key.equals("database.pool.size.minimum")) {
               DATABASE_MIN_POOL_SIZE = Integer.parseInt(value);
               DATABASE_POOL_HAKIRI_MIN_SIZE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.serverPreparedStatements.cache") ||
                  key.equals("db.pool.serverPreparedStatements.cache") ||
                  key.equals("database.pool.serverPreparedStatements.cache") ||
                  key.equals("pool.prepstatements.server.cache") ||
                  key.equals("db.pool.prepstatements.server.cache") ||
                  key.equals("database.pool.prepstatements.server.cache") ||
                  key.equals("pool.cache.server.preparedStatements") ||
                  key.equals("db.pool.cache.server.preparedStatements") ||
                  key.equals("database.pool.cache.server.preparedStatements") ||
                  key.equals("pool.cache.server.prepstatements") ||
                  key.equals("db.pool.cache.server.prepstatements") ||
                  key.equals("database.pool.cache.server.prepstatements")) {
               if (value.matches("(true|false)")) {
                  boolean enabled = Boolean.parseBoolean(value);
                  if (enabled) {
                     DATABASE_USE_SERVER_PREPARED_STATEMENTS = true;
                     DATABASE_POOL_HIKARI_USE_SERVER_PREPARED_STATEMENTS = true;
                  } else {
                     DATABASE_USE_SERVER_PREPARED_STATEMENTS = false;
                     DATABASE_POOL_HIKARI_USE_SERVER_PREPARED_STATEMENTS = false;
                  }
               }
            } else if (
                  key.equals("pool.cache.preparedStatements") ||
                  key.equals("db.pool.cache.preparedStatements") ||
                  key.equals("database.pool.cache.preparedStatements") ||
                  key.equals("pool.cache.prepstatements") ||
                  key.equals("db.pool.cache.prepstatements") ||
                  key.equals("database.pool.cache.prepstatements")) {
               DATABASE_POOL_HIKARI_CACHE_PREPARED_STATEMENTS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.preparedStatements.cache.size") ||
                  key.equals("db.pool.preparedStatements.cache.size") ||
                  key.equals("database.pool.preparedStatements.cache.size") ||
                  key.equals("pool.prepstatements.cache.size") ||
                  key.equals("db.pool.prepstatements.cache.size") ||
                  key.equals("database.pool.prepstatements.cache.size") ||
                  key.equals("pool.cache.preparedStatements.size") ||
                  key.equals("db.pool.cache.preparedStatements.size") ||
                  key.equals("database.pool.cache.preparedStatements.size") ||
                  key.equals("pool.cache.prepstatements.size") ||
                  key.equals("db.pool.cache.prepstatements.size") ||
                  key.equals("database.pool.cache.prepstatements.size")) {
               DATABASE_MAX_STATEMENTS = Integer.parseInt(value);
               DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SIZE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.preparedStatements.cache.sql.limit") ||
                  key.equals("db.pool.preparedStatements.cache.sql.limit") ||
                  key.equals("database.pool.preparedStatements.cache.sql.limit") ||
                  key.equals("pool.prepstatements.cache.sql.limit") ||
                  key.equals("db.pool.prepstatements.cache.sql.limit") ||
                  key.equals("database.pool.prepstatements.cache.sql.limit") ||
                  key.equals("pool.cache.preparedStatements.sql.limit") ||
                  key.equals("db.pool.cache.preparedStatements.sql.limit") ||
                  key.equals("database.pool.cache.preparedStatements.sql.limit") ||
                  key.equals("pool.cache.prepstatements.sql.limit") ||
                  key.equals("db.pool.cache.prepstatements.sql.limit") ||
                  key.equals("database.pool.cache.prepstatements.sql.limit")) {
               DATABASE_POOL_HIKARI_PREPARED_STATEMENTS_CACHE_SQL_LIMIT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.validation") ||
                  key.equals("db.pool.validation") ||
                  key.equals("database.pool.validation") ||
                  key.equals("pool.validation.timeout") || 
                  key.equals("db.pool.validation.timeout") ||
                  key.equals("database.pool.validation.timeout")) {
               DATABASE_POOL_HIKARI_VALIDATION_TIMEOUT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.reconnect") || 
                  key.equals("database.pool.reconnect") || 
                  key.equals("db.pool.reconnect")) {
               DATABASE_POOL_HIKARI_AUTO_RECONNECT = value.equals("auto") ? true : false;
            } else if (
                  key.equals("pool.connection.test") ||
                  key.equals("db.pool.connection.test") ||
                  key.equals("database.pool.connection.test") ||
                  key.equals("pool.test") ||
                  key.equals("db.pool.test") ||
                  key.equals("database.pool.test")) {
               DATABASE_POOL_HIKARI_CONNECTION_TEST = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.connection.test.query") ||
                  key.equals("db.pool.connection.test.query") ||
                  key.equals("database.pool.connection.test.query") ||
                  key.equals("pool.test.query") ||
                  key.equals("db.pool.test.query") ||
                  key.equals("database.pool.test.query")) {
               DATABASE_POOL_HIKARI_CONNECTION_TEST_QUERY = value;
            } else if (
                  key.equals("pool.test.period") || 
                  key.equals("database.test.period") || 
                  key.equals("db.test.period")) {
               DATABASE_IDLE_TEST_PERIOD = Integer.parseInt(value);
            } else if (
                  key.equals("pool.idle.timeout") ||
                  key.equals("db.pool.idle.timeout") ||
                  key.equals("database.pool.idle.timeout") ||
                  key.equals("pool.idle") ||
                  key.equals("db.pool.idle") ||
                  key.equals("database.pool.idle")) {
//               DATABASE_MAX_IDLE_TIME = Integer.parseInt(value);
               DATABASE_POOL_HIKARI_IDLE_TIMEOUT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.idle.minimum") ||
                  key.equals("db.pool.idle.minimum") ||
                  key.equals("database.pool.idle.minimum") ||
                  key.equals("pool.idle.min") ||
                  key.equals("db.pool.idle.min") ||
                  key.equals("database.pool.idle.min")) {
               DATABASE_POOL_HIKARI_MINIMUM_IDLE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.checkout.timeout") ||
                  key.equals("db.pool.checkout.timeout") ||
                  key.equals("database.pool.checkout.timeout") ||
                  key.equals("pool.checkout") ||
                  key.equals("db.pool.checkout") ||
                  key.equals("database.pool.checkout")) {
               DATABASE_POOL_HIKARI_CHECKOUT_TIMEOUT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.connection.timeout") ||
                  key.equals("db.pool.connection.timeout") ||
                  key.equals("database.pool.connection.timeout") ||
                  key.equals("pool.timeout") ||
                  key.equals("db.pool.timeout") ||
                  key.equals("database.pool.timeout")) {
               DATABASE_POOL_HIKARI_CONNECTION_TIMEOUT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.retry") || 
                  key.equals("db.pool.retry") ||
                  key.equals("database.pool.retry") ||
                  key.equals("pool.retry.attempts") || 
                  key.equals("db.pool.retry.attempts") ||
                  key.equals("database.pool.retry.attempts")) {
               DATABASE_ACQUIRE_RETRY_ATTEMPTS = Integer.parseInt(value);
            } else if (
                  key.equals("pool.delay") || 
                  key.equals("database.pool.delay") || 
                  key.equals("db.pool.delay") ||
                  key.equals("pool.retry.delay") || 
                  key.equals("database.pool.retry.delay") || 
                  key.equals("db.pool.retry.delay")) {
               DATABASE_ACQUIRE_RETRY_DELAY = Integer.parseInt(value);
            } else if (
                  key.equals("pool.connection.age.max") ||
                  key.equals("pool.connection.age.maximum") ||
                  key.equals("db.pool.connection.age.max") ||
                  key.equals("db.pool.connection.age.maximum") ||
                  key.equals("database.pool.connection.age.max") ||
                  key.equals("database.pool.connection.age.maximum") ||
                  key.equals("pool.age.max") ||
                  key.equals("pool.age.maximum") ||
                  key.equals("db.pool.age.max") ||
                  key.equals("db.pool.age.maximum") ||
                  key.equals("database.pool.age.max") ||
                  key.equals("database.pool.age.maximum") ||
                  key.equals("pool.age") ||
                  key.equals("db.pool.age") ||
                  key.equals("database.pool.age")) {
               DATABASE_MAX_CONNECTION_AGE = Integer.parseInt(value);
            } else if (
                  key.equals("pool.lifetime.max") ||
                  key.equals("pool.lifetime.maximum") ||
                  key.equals("db.pool.lifetime.max") ||
                  key.equals("db.pool.lifetime.maximum") ||
                  key.equals("database.pool.lifetime.max") ||
                  key.equals("database.pool.lifetime.maximum")) {
               DATABASE_POOL_HIKARI_MAX_LIFETIME = Integer.parseInt(value);
            } else if (
                  key.equals("pool.leak.detection") ||
                  key.equals("db.pool.leak.detection") ||
                  key.equals("database.pool.leak.detection") ||
                  key.equals("pool.leak") ||
                  key.equals("db.pool.leak") ||
                  key.equals("database.pool.leak")) {
               DATABASE_POOL_HIKARI_LEAK_DETECTION = Integer.parseInt(value);
            } else if (
                  key.equals("pool.session.state.local") ||
                  key.equals("db.pool.session.state.local") ||
                  key.equals("database.pool.session.state.local") ||
                  key.equals("pool.sessionState.local") ||
                  key.equals("db.pool.sessionState.local") ||
                  key.equals("database.pool.sessionState.local")) {
                  DATABASE_POOL_HIKARI_USE_LOCAL_SESSION_STATE = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.transaction.state.local") ||
                  key.equals("db.pool.transaction.state.local") ||
                  key.equals("database.pool.transaction.state.local") ||
                  key.equals("pool.transactionState.local") ||
                  key.equals("db.pool.transactionState.local") ||
                  key.equals("database.pool.transactionState.local")) {
                  DATABASE_POOL_HIKARI_USE_LOCAL_TRANSACTION_STATE = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.statements.batch.rewrite") ||
                  key.equals("db.pool.statements.batch.rewrite") ||
                  key.equals("database.pool.statements.batch.rewrite") ||
                  key.equals("pool.batchStatements.rewrite") ||
                  key.equals("db.pool.batchStatements.rewrite") ||
                  key.equals("database.pool.batchStatements.rewrite") ||
                  key.equals("pool.batch.rewrite") ||
                  key.equals("db.pool.batch.rewrite") ||
                  key.equals("database.pool.batch.rewrite")) {
                  DATABASE_POOL_HIKARI_REWRITE_BATCHED_STAMENTS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.cache.resultset.metadata") ||
                  key.equals("db.pool.cache.resultset.metadata") ||
                  key.equals("database.pool.cache.resultset.metadata")) {
                  DATABASE_POOL_HIKARI_CACHE_RESULTSET_METADATA = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.cache.server.configuration") ||
                  key.equals("db.pool.cache.server.configuration") ||
                  key.equals("database.pool.cache.server.configuration")) {
                  DATABASE_POOL_HIKARI_CACHE_SERVER_CONFIGURATION = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.elide.set.autocommits") ||
                  key.equals("db.pool.elide.set.autocommits") ||
                  key.equals("database.pool.elide.set.autocommits")) {
                  DATABASE_POOL_HIKARI_ELIDE_SET_AUTO_COMMITS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.maintain.timestats") ||
                  key.equals("db.pool.maintain.timestats") ||
                  key.equals("database.pool.maintain.timestats") ||
                  key.equals("pool.maintain.time.statistics") ||
                  key.equals("db.pool.maintain.time.statistics") ||
                  key.equals("database.pool.maintain.time.statistics")) {
                  DATABASE_POOL_HIKARI_MAINTAIN_TIME_STATS = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.initialize.fail.timeout") ||
                  key.equals("db.pool.initialize.fail.timeout") ||
                  key.equals("database.pool.initialize.fail.timeout") ||
                  key.equals("pool.ini.fail.timeout") ||
                  key.equals("db.pool.ini.fail.timeout") ||
                  key.equals("database.pool.ini.fail.timeout")) {
                  DATABASE_POOL_HIKARI_INITIALIZE_FAIL_TIMEOUT = Integer.parseInt(value);
            } else if (
                  key.equals("pool.isolate.internal.queries") ||
                  key.equals("db.pool.isolate.internal.queries") ||
                  key.equals("database.pool.isolate.internal.queries") ||
                  key.equals("pool.queries.internal.isolate") ||
                  key.equals("db.pool.queries.internal.isolate") ||
                  key.equals("database.pool.queries.internal.isolate")) {
                  DATABASE_POOL_HIKARI_ISOLATE_INTERNAL_QUERIES = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.suspension.allow") ||
                  key.equals("db.pool.suspension.allow") ||
                  key.equals("database.pool.suspension.allow")) {
                  DATABASE_POOL_HIKARI_ALLOW_POOL_SUSPENSION = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.readonly") ||
                  key.equals("db.pool.readonly") ||
                  key.equals("database.pool.readonly")) {
                  DATABASE_POOL_HIKARI_READ_ONLY = Boolean.parseBoolean(value);
            } else if (
                  key.equals("pool.mbeans.register") ||
                  key.equals("db.pool.mbeans.register") ||
                  key.equals("database.pool.mbeans.register")) {
                  DATABASE_POOL_HIKARI_REGISTER_MBEANS = Boolean.parseBoolean(value);
            }
            else if (
                  key.equals("foreignkey.checks") ||
                  key.equals("db.foreignkey.checks") ||
                  key.equals("database.foreignkey.checks") ||
                  key.equals("fk.checks") ||
                  key.equals("db.fk.checks") ||
                  key.equals("database.fk.checks")) {
               FOREIGN_KEY_CHECKS = new Boolean(value.toLowerCase());
            } else if (
                  key.equals("ssl") || 
                  key.equals("db.ssl") || 
                  key.equals("database.ssl")) {
               DATABASE_USE_SSL = Boolean.parseBoolean(value);
            } else if (
                  key.equals("verifyServerCertificate") ||
                  key.equals("verify.server.certificate") ||
                  key.equals("db.verify.server.certificate") ||
                  key.equals("database.verify.server.certificate") ||
                  key.equals("certificate") ||
                  key.equals("db.certificate") ||
                  key.equals("database.certificate")) {
               if (value.matches("(false|true)")) {
                  DATABASE_VERIFY_SERVER_CERTIFICATE = Boolean.parseBoolean(value);
               }
               if (value.equals("verify")) {
                  DATABASE_VERIFY_SERVER_CERTIFICATE = true;
               }
               if (value.equals("ignore")) {
                  DATABASE_VERIFY_SERVER_CERTIFICATE = false;
               }
            } else if (
                  key.equals("autoReconnect") || 
                  key.equals("db.autoReconnect") || 
                  key.equals("database.autoReconnect") ||
                  key.equals("reconnect") || 
                  key.equals("db.reconnect") || 
                  key.equals("database.reconnect")) {
               if (value.matches("(false|true)")) {
                  DATABASE_AUTO_RECONNECT = Boolean.parseBoolean(value);
               }
               if (value.equals("auto")) {
                  DATABASE_AUTO_RECONNECT = true;
               }
            } else if (
                  key.equals("autoReconnectForPools") || 
                  key.equals("db.autoReconnectForPools") || 
                  key.equals("database.autoReconnectForPools") || 
                  key.equals("pool.reconnect") ||
                  key.equals("db.pool.reconnect") || 
                  key.equals("database.pool.reconnect")) {
               if (value.matches("(false|true)")) {
                  DATABASE_AUTO_RECONNECT_FOR_POOLS = Boolean.parseBoolean(value);
               }
               if (value.equals("auto")) {
                  DATABASE_AUTO_RECONNECT_FOR_POOLS = true;
               }
            } else if (
                  key.equals("debug") || 
                  key.equals("database.debug") || 
                  key.equals("db.debug") ||
                  key.equals("sql.show") ||  
                  key.equals("db.sql.show") ||
                  key.equals("database.sql.show")) {
               if (value.matches("(false|true)")) {
                  DEBUG = Boolean.parseBoolean(value);
               }
            } else if (
                  key.equals("fetch") ||
                  key.equals("db.fetch") ||
                  key.equals("database.fetch") ||
                  key.equals("fetch.type") ||
                  key.equals("db.fetch.type") ||
                  key.equals("database.fetch.type")) {
               if (value.equals("none")) {
                  FETCH_TYPE = FetchType.NONE;
               } else if (value.equals("eager")) {
                  FETCH_TYPE = FetchType.EAGER;
               } else if (value.equals("lazy")) {
                  FETCH_TYPE = FetchType.LAZY;
               }
            } else if (
                  key.equals("cascade") || 
                  key.equals("db.cascade") ||
                  key.equals("database.cascade") || 
                  key.equals("cascade.type") ||
                  key.equals("db.cascade.type") || 
                  key.equals("database.cascade.type")) {
               if (value.equals("none")) {
                  CASCADE_TYPE = CascadeType.NONE;
               } else if (value.equals("insert")) {
                  CASCADE_TYPE = CascadeType.INSERT;
               } else if (value.equals("update")) {
                  CASCADE_TYPE = CascadeType.UPDATE;
               } else if (value.equals("save")) {
                  CASCADE_TYPE = CascadeType.SAVE;
               } else if (value.equals("delete")) {
                  CASCADE_TYPE = CascadeType.DELETE;
               } else if (value.equals("all")) {
                  CASCADE_TYPE = CascadeType.ALL;
               }
            } else if (
                  key.equals("charset") || 
                  key.equals("db.charset") || 
                  key.equals("database.charset")) {
               DATABASE_CHARSET = value;
            } else if (
                  key.equals("collate") || 
                  key.equals("db.collate") || 
                  key.equals("database.collate")) {
               DATABASE_COLLATE = value;
            } else if (
                  key.equals("unicode") || 
                  key.equals("db.unicode") || 
                  key.equals("database.unicode")) {
               DATABASE_UNICODE = Boolean.parseBoolean(value);
            } else if (
                  key.startsWith("app.install") ||
                  key.startsWith("db.app.install") ||
                  key.startsWith("database.app.install")) {
               if (!INSTALLED_APPS.contains(value)) {
                  INSTALLED_APPS.add(value);
               }
            } else if (
                  key.startsWith("app.table.prefix") ||
                  key.startsWith("db.app.table.prefix") ||
                  key.startsWith("database.app.table.prefix")) {
                  PREFIX_APP_TABLE = Boolean.parseBoolean(value);
            } else if (
                  key.equals("codegen") || 
                  key.equals("db.codegen") || 
                  key.equals("database.codegen") ||
                  key.equals("code.gen") || 
                  key.equals("db.code.gen") || 
                  key.equals("database.code.gen") ||
                  key.equals("code.generation") || 
                  key.equals("db.code.generation") || 
                  key.equals("database.code.generation") ||
                  key.equals("code") || 
                  key.equals("db.code") || 
                  key.equals("database.code")) {
               if (key.equals("code") || 
                   key.equals("db.code") || 
                   key.equals("database.code")) {
                  if (value.equals("generate")) {
                     CODE_GENERATION = true;
                  } else {
                     CODE_GENERATION = false;
                  }
               } else {
                  CODE_GENERATION = Boolean.parseBoolean(value);
               }
            } else if (
                  key.equals("build.maven") ||
                  key.equals("db.build.maven") ||
                  key.equals("database.build.maven") ||
                  key.equals("build.mvn") ||
                  key.equals("db.build.mvn") ||
                  key.equals("database.build.mvn") ||
                  key.equals("maven") || 
                  key.equals("db.maven") || 
                  key.equals("database.maven") ||
                  key.equals("proj.maven") || 
                  key.equals("db.proj.maven") || 
                  key.equals("database.project.maven") ||
                  key.equals("project.maven") || 
                  key.equals("db.project.maven") || 
                  key.equals("database.project.maven") ||
                  key.equals("mvn") || 
                  key.equals("db.mvn") || 
                  key.equals("database.maven") ||
                  key.equals("proj.mvn") || 
                  key.equals("db.proj.mvn") || 
                  key.equals("database.proj.mvn") ||
                  key.equals("project.mvn") || 
                  key.equals("db.project.mvn") || 
                  key.equals("database.project.mvn")) {
               MAVEN_PROJECT = Boolean.parseBoolean(value);
            } else if (
                  key.equals("build.gradle") ||
                  key.equals("db.build.gradle") ||
                  key.equals("database.build.gradle") ||
                  key.equals("gradle") || 
                  key.equals("db.gradle") || 
                  key.equals("database.gradle") ||
                  key.equals("proj.gradle") || 
                  key.equals("db.proj.gradle") || 
                  key.equals("db.project.gradle") ||
                  key.equals("project.gradle") || 
                  key.equals("db.project.gradle") || 
                  key.equals("database.project.gradle")) {
               GRADLE_PROJECT = Boolean.parseBoolean(value);
            } else if (
                  key.equals("exception.print") || 
                  key.equals("db.exception.print") || 
                  key.equals("database.exception.print")) {
               boolean print = Boolean.parseBoolean(value);
               if (print) {
                  EXCEPTION_HANDLING = ExceptionHandling.PRINT;
               } else {
                  EXCEPTION_HANDLING = ExceptionHandling.THROW;
               }
            } else if (
                  key.equals("exception") ||
                  key.equals("db.exception") ||
                  key.equals("database.exception")) {
               if (value.equals("print")) {
                  EXCEPTION_HANDLING = ExceptionHandling.PRINT;
               }
               if (value.equals("throw")) {
                  EXCEPTION_HANDLING = ExceptionHandling.THROW;
               }
            } else if (
                  key.equals("project.type") || 
                  key.equals("db.project.type") || 
                  key.equals("database.project.type") ||
                  key.equals("build") ||
                  key.equals("db.build") ||
                  key.equals("database.build")) {
               if (value.equals("gradle")) {
                  GRADLE_PROJECT = true;
               } else if (value.equals("maven") || value.equals("mvn")) {
                  MAVEN_PROJECT = true;
               } else {
                  
               }
            } else if (
                  key.equals("logging") || 
                  key.equals("db.logging") || 
                  key.equals("database.logging") ||
                  key.equals("log4jdbc") || 
                  key.equals("db.log4jdbc") || 
                  key.equals("database.log4jdbc") ||
                  key.equals("jdbc.log") ||
                  key.equals("db.jdbc.log") ||
                  key.equals("database.jdbc.log")) {
               if (value.matches("(true|false)")) {
                  DATABASE_LOGGING = Boolean.parseBoolean(value);
               }
            } else if (
                  key.equals("webapp") ||
                  key.equals("db.webapp") ||
                  key.equals("database.webapp")) {
                  WEB_APP = Boolean.parseBoolean(value);
            } else if (
                  key.equals("gradle.webapp") ||
                  key.equals("db.gradle.webapp") ||
                  key.equals("database.gradle.webapp") ||
                  key.equals("build.gradle.webapp") ||
                  key.equals("db.build.gradle.webapp") ||
                  key.equals("database.build.gradle.webapp")) {
                  boolean gradleWebApp = Boolean.parseBoolean(value);
                  GRADLE_PROJECT = gradleWebApp;
                  GRADLE_WEB_APP = gradleWebApp;
            } else if (
                  key.equals("maven.webapp") ||
                  key.equals("db.maven.webapp") ||
                  key.equals("database.maven.webapp") ||
                  key.equals("build.maven.webapp") ||
                  key.equals("db.build.maven.webapp") ||
                  key.equals("database.build.maven.webapp")) {
                  boolean mavenWebApp = Boolean.parseBoolean(value);
                  MAVEN_PROJECT = mavenWebApp;
                  MAVEN_WEB_APP = mavenWebApp;
            } else {
               
            }
            if (MAVEN_PROJECT || GRADLE_PROJECT) {
               setAppDirs(APP_ROOT_DIR);
            }
            if (GRADLE_WEB_APP || MAVEN_WEB_APP) {
               APP_LIBS_DIR = SRC_MAIN_WEBAPP_WEBINF__LIB_DIR;
            } else {
               APP_LIBS_DIR = LIB_DIR;
            }
         }
         JEDI_PROPERTIES_LOADED = true;
         JEDI_PROPERTIES_FILE.close();
         if (DATABASE_ENGINE.equals("mysql")) {
            getMySQLVersion();
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
         System.exit(0);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static List<Field> getFields(Class<? extends jedi.db.models.Model> c, Class a) {
      List<Field> fields = new ArrayList<>();
      if (c != null) {
         try {
            if (!isJediFieldAnnotation(a)) {
               throw new Exception("Não foi informada uma classe de anotação válida para o tipo de field.");
            } else {
               for (Field field : getAllFields(c)) {
                  field.setAccessible(true);
                  Annotation annotation = field.getAnnotation(a);
                  if (annotation != null) {
                     fields.add(field);
                  }
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return fields;
   }
   
   public static List<Field> getFields(Class<? extends jedi.db.models.Model> c) {
      List<Field> fields = new ArrayList<>();
      if (isJediModel(c)) {
         for (Field field : getAllFields(c)) {
            if (isJediField(field)) {
               fields.add(field);
            }
         }
      }
      return fields;
   }
   
   public static Field getField(String name, Class<? extends jedi.db.models.Model> c) {
      if (name != null && !name.trim().isEmpty() && c != null) {
         for (Field f : getAllFields(c)) {
            if (f.getName().equals(name)) {
               return f;
            }
         }
      }
      return null;
   }
   
   public static List<Field> getCharFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, CharField.class);
   }
   
   public static List<Field> getEmailFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, EmailField.class);
   }
   
   public static List<Field> getURLFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, URLField.class);
   }
   
   public static List<Field> getIPAddressFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, IPAddressField.class);
   }
   
   public static List<Field> getTextFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, TextField.class);
   }
   
   public static List<Field> getIntegerFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, IntegerField.class);
   }
   
   public static List<Field> getDecimalFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, DecimalField.class);
   }
   
   public static List<Field> getFloatFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, FloatField.class);
   }
   
   public static List<Field> getBooleanFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, BooleanField.class);
   }
   
   public static List<Field> getDateFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, DateField.class);
   }
   
   public static List<Field> getTimeFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, TimeField.class);
   }
   
   public static List<Field> getDateTimeFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, DateTimeField.class);
   }
   
   public static List<Field> getOneToOneFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, OneToOneField.class);
   }
   
   public static List<Field> getForeignKeyFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, ForeignKeyField.class);
   }
   
   public static List<Field> getManyToManyFields(Class<? extends jedi.db.models.Model> c) {
      return getFields(c, ManyToManyField.class);
   }
   
   private static String getPrimaryKeySQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         String format = "";
         String columnName = "";
         if (DATABASE_ENGINE.equals("mysql")) {
            format = "%s INT NOT NULL PRIMARY KEY AUTO_INCREMENT";
            columnName = ColumnUtil.getColumnName(field);
         }
         sql = String.format(format, columnName);
      }
      return sql;
   }
   
   public static String getPrimaryKeySQL() {
      String sql = "";
      try {
         sql = getPrimaryKeySQL(jedi.db.models.Model.class.getDeclaredField("id"));
      } catch (NoSuchFieldException e) {
         e.printStackTrace();
      } catch (SecurityException e) {
         e.printStackTrace();
      }
      return sql;
   }
   
   public static String getCharFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         CharField charFieldAnnotation = field.getAnnotation(CharField.class);
         if (charFieldAnnotation != null) {
            String comment = getComment(charFieldAnnotation);
            String defaultValue = getDefaultValue(charFieldAnnotation);
            sql = String.format(
               getSQLFormatter(charFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               charFieldAnnotation.max_length(),
               charFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.equals("\\0")
                  ? "" : String.format(defaultValue.equalsIgnoreCase("null") ? " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               charFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            if (DATABASE_ENGINE.equals("postgresql") || DATABASE_ENGINE.equals("oracle")) {
               String tableName = TableUtil.getTableName(field.getDeclaringClass());
               List<String> comments = new ArrayList<>();
               comment = String.format("COMMENT ON COLUMN %s.%s IS '%s';\n\n", tableName, TableUtil.getColumnName(field), comment);
               comments.add(comment);
               SQL_COMMENTS.put(tableName, comments);
            }
         }
      }
      return sql;
   }
   
   public static String getEmailFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         EmailField emailFieldAnnotation = field.getAnnotation(EmailField.class);
         if (emailFieldAnnotation != null) {
            String comment = getComment(emailFieldAnnotation);
            String defaultValue = getDefaultValue(emailFieldAnnotation);
            sql = String.format(
               getSQLFormatter(emailFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               emailFieldAnnotation.max_length(),
               emailFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.equals("\\0")
                  ? "" : String.format(defaultValue.equalsIgnoreCase("null") ? " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               emailFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getURLFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         URLField urlFieldAnnotation = field.getAnnotation(URLField.class);
         if (urlFieldAnnotation != null) {
            String comment = getComment(urlFieldAnnotation);
            String defaultValue = getDefaultValue(urlFieldAnnotation);
            sql = String.format(
               getSQLFormatter(urlFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               urlFieldAnnotation.max_length(),
               urlFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.equals("\\0")
                  ? "" : String.format(defaultValue.equalsIgnoreCase("null") ? " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               urlFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getIPAddressFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         IPAddressField ipAddressFieldAnnotation = field.getAnnotation(IPAddressField.class);
         if (ipAddressFieldAnnotation != null) {
            String comment = getComment(ipAddressFieldAnnotation);
            String defaultValue = getDefaultValue(ipAddressFieldAnnotation);
            sql = String.format(
               getSQLFormatter(ipAddressFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               ipAddressFieldAnnotation.max_length(),
               ipAddressFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.equals("\\0")
                  ? "" : String.format(defaultValue.equalsIgnoreCase("null") ? " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               ipAddressFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getTextFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         TextField textFieldAnnotation = field.getAnnotation(TextField.class);
         if (textFieldAnnotation != null) {
            String comment = getComment(textFieldAnnotation);
            String defaultValue = getDefaultValue(textFieldAnnotation);
            sql = String.format(
               getSQLFormatter(textFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               textFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.equals("\\0")
                  ? "" : String.format(defaultValue.equalsIgnoreCase("null") ? " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               textFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getIntegerFieldSQL(Field field) {
      String sql = "";
      if (isJediField(field)) {
         IntegerField integerFieldAnnotation = field.getAnnotation(IntegerField.class);
         if (integerFieldAnnotation != null) {
            String comment = getComment(integerFieldAnnotation);
            String defaultValue = getDefaultValue(integerFieldAnnotation);
            sql = String.format(
               getSQLFormatter(integerFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               integerFieldAnnotation.size(),
               integerFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.isEmpty() ? defaultValue : String.format(" DEFAULT %s", defaultValue),
               integerFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getDecimalFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         DecimalField decimalFieldAnnotation = field.getAnnotation(DecimalField.class);
         if (decimalFieldAnnotation != null) {
            String comment = getComment(decimalFieldAnnotation);
            String defaultValue = getDefaultValue(decimalFieldAnnotation);
            sql = String.format(
               getSQLFormatter(decimalFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               decimalFieldAnnotation.scale(),
               decimalFieldAnnotation.precision(),
               decimalFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.isEmpty() ? defaultValue : String.format(" DEFAULT %s", defaultValue),
               decimalFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getFloatFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         FloatField floatFieldAnnotation = field.getAnnotation(FloatField.class);
         if (floatFieldAnnotation != null) {
            String comment = getComment(floatFieldAnnotation);
            String defaultValue = getDefaultValue(floatFieldAnnotation);
            sql = String.format(
               getSQLFormatter(floatFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               floatFieldAnnotation.scale(),
               floatFieldAnnotation.precision(),
               floatFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.isEmpty() ? defaultValue : String.format(" DEFAULT %s", defaultValue),
               floatFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getBooleanFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         BooleanField booleanFieldAnnotation = field.getAnnotation(BooleanField.class);
         if (booleanFieldAnnotation != null) {
            String comment = getComment(booleanFieldAnnotation);
            String defaultValue = getDefaultValue(booleanFieldAnnotation);
            sql = String.format(
               getSQLFormatter(booleanFieldAnnotation, DATABASE_ENGINE),
               ColumnUtil.getColumnName(field),
               booleanFieldAnnotation.required() == true ? " NOT NULL" : "",
               booleanFieldAnnotation.unique() == true ? " UNIQUE" : "",
               defaultValue.isEmpty() ? defaultValue : (defaultValue == "true" ? " DEFAULT 1" : " DEFAULT 0"),
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
         }
      }
      return sql;
   }
   
   public static String getDateFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         DateField dateFieldAnnotation = field.getAnnotation(DateField.class);
         if (dateFieldAnnotation != null) {
            Class c = field.getDeclaringClass();
            String fieldName = ColumnUtil.getColumnName(field);
            String tableName = TableUtil.getTableName(c);
            String comment = getComment(dateFieldAnnotation);
            String defaultValue = getDefaultValue(dateFieldAnnotation);
            sql = String.format(
               getSQLFormatter(dateFieldAnnotation, DATABASE_ENGINE),
               fieldName,
               dateFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.isEmpty() ? defaultValue : String.format(defaultValue.equalsIgnoreCase("NULL") ? 
               " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               dateFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            if (MYSQL_VERSION != null) {
               if (dateFieldAnnotation.auto_now_add()) {
                  if (MYSQL_AUTO_NOW_ADD.get(tableName) == null) {
                     MYSQL_AUTO_NOW_ADD.put(tableName, new ArrayList<String>());
                     MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                  } else {
                     MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                  }
               }
               if (dateFieldAnnotation.auto_now()) {
                  if (MYSQL_AUTO_NOW.get(tableName) == null) {
                     MYSQL_AUTO_NOW.put(tableName, new ArrayList<String>());
                     MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                  } else {
                     MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                  }
               }
            }
         }
      }
      return sql;
   }
   
   public static String getTimeFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         TimeField timeFieldAnnotation = field.getAnnotation(TimeField.class);
         if (timeFieldAnnotation != null) {
            Class c = field.getDeclaringClass();
            String fieldName = ColumnUtil.getColumnName(field);
            String tableName = TableUtil.getTableName(c);
            String defaultValue = getDefaultValue(timeFieldAnnotation);
            String comment = getComment(timeFieldAnnotation);
            sql = String.format(
               getSQLFormatter(timeFieldAnnotation, DATABASE_ENGINE),
               fieldName,
               timeFieldAnnotation.required() ? " NOT NULL" : "",
               defaultValue.isEmpty() ? defaultValue : String.format(defaultValue.equalsIgnoreCase("NULL") ? 
               " DEFAULT %s" : " DEFAULT '%s'", defaultValue),
               timeFieldAnnotation.unique() ? " UNIQUE" : "",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            if (MYSQL_VERSION != null) {
               if (timeFieldAnnotation.auto_now_add()) {
                  if (MYSQL_AUTO_NOW_ADD.get(tableName) == null) {
                     MYSQL_AUTO_NOW_ADD.put(tableName, new ArrayList<String>());
                     MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                  } else {
                     MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                  }
               }
               if (timeFieldAnnotation.auto_now()) {
                  if (MYSQL_AUTO_NOW.get(tableName) == null) {
                     MYSQL_AUTO_NOW.put(tableName, new ArrayList<String>());
                     MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                  } else {
                     MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                  }
               }
            }
         }
      }
      return sql;
   }
   
   @Deprecated
   public static String _getDateTimeFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         DateTimeField dateTimeFieldAnnotation = field.getAnnotation(DateTimeField.class);
         if (dateTimeFieldAnnotation != null) {
            Class c = field.getDeclaringClass();
            String fieldName = ColumnUtil.getColumnName(field);
            String tableName = TableUtil.getColumnName(c);
            String defaultValue = getDefaultValue(dateTimeFieldAnnotation);
            String comment = getComment(dateTimeFieldAnnotation);
            MYSQL_VERSION = MYSQL_VERSION != null ? MYSQL_VERSION : getMySQLVersion();
            sql = String
               .format(
                  getSQLFormatter(dateTimeFieldAnnotation, DATABASE_ENGINE),
                  fieldName,
                  dateTimeFieldAnnotation.required() ? " NOT NULL" : "",
                  defaultValue.isEmpty() ? defaultValue : String.format(" DEFAULT '%s'", defaultValue),
                  dateTimeFieldAnnotation.auto_now() && (MYSQL_VERSION != null && MYSQL_VERSION >= 56)
                     ? (defaultValue.isEmpty()
                        ? String.format(" DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP") : " ON UPDATE CURRENT_TIMESTAMP")
                     : "",
                  dateTimeFieldAnnotation.unique() ? " UNIQUE" : "",
                  DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            if (dateTimeFieldAnnotation.auto_now_add() || dateTimeFieldAnnotation.auto_now()) {
               if (MYSQL_VERSION != null) {
                  if (MYSQL_VERSION >= 56) {
                     if (dateTimeFieldAnnotation.auto_now_add()) {
                        defaultValue = " DEFAULT CURRENT_TIMESTAMP";
                     }
                  } else {
                     if (dateTimeFieldAnnotation.auto_now_add()) {
                        if (MYSQL_AUTO_NOW_ADD.get(tableName) == null) {
                           MYSQL_AUTO_NOW_ADD.put(tableName, new ArrayList<String>());
                           MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                        } else {
                           MYSQL_AUTO_NOW_ADD.get(tableName).add(fieldName);
                        }
                     }
                     if (dateTimeFieldAnnotation.auto_now()) {
                        if (MYSQL_AUTO_NOW.get(tableName) == null) {
                           MYSQL_AUTO_NOW.put(tableName, new ArrayList<String>());
                           MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                        } else {
                           MYSQL_AUTO_NOW.get(tableName).add(fieldName);
                        }
                     }
                  }
               }
            }
         }
      }
      return sql;
   }
   
   public static String getDateTimeFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         DateTimeField annotation = field.getAnnotation(DateTimeField.class);
         if (annotation != null) {
            Class c = field.getDeclaringClass();
            String column = ColumnUtil.getColumnName(field);
            String table = TableUtil.getColumnName(c);
            String defaultValue = getDefaultValue(annotation);
            String comment = getComment(annotation);
            String formatter = getSQLFormatter(annotation, DATABASE_ENGINE);
            String notNull = annotation.required() ? " NOT NULL" : "";
            String unique = annotation.unique() ? " UNIQUE" : "";
            String _default = defaultValue.isEmpty() ? defaultValue : String.format(defaultValue.equalsIgnoreCase("NULL") ? 
            " DEFAULT %s" : " DEFAULT '%s'", defaultValue);
            int precision = getPrecision(annotation);
            // TODO - essa tratativa deve ficar no método getComment().
            if (DATABASE_ENGINE.equals("mysql")) {
               comment = comment.isEmpty() ? "" : String.format(" COMMENT '%s'", comment);
            }
            if (MYSQL_VERSION >= 56) {
               if (annotation.auto_now_add()) {
                  if (precision <= 0) {
                     _default = " DEFAULT CURRENT_TIMESTAMP";
                  } else {
                     _default = String.format(" DEFAULT CURRENT_TIMESTAMP(%s)", precision);
                  }
               }
               if (annotation.auto_now()) {
                  if (precision <= 0) {
                     _default = String.format("%s ON UPDATE CURRENT_TIMESTAMP", _default);
                  } else {
                     _default = String.format("%s ON UPDATE CURRENT_TIMESTAMP(%s)", _default, precision);
                  }
               }
            } else {
               if (annotation.auto_now_add()) {
                  if (MYSQL_AUTO_NOW_ADD.get(table) == null) {
                     MYSQL_AUTO_NOW_ADD.put(table, new ArrayList<String>());
                     MYSQL_AUTO_NOW_ADD.get(table).add(column);
                  } else {
                     MYSQL_AUTO_NOW_ADD.get(table).add(column);
                  }
               }
               if (annotation.auto_now()) {
                  if (MYSQL_AUTO_NOW.get(table) == null) {
                     MYSQL_AUTO_NOW.put(table, new ArrayList<String>());
                     MYSQL_AUTO_NOW.get(table).add(column);
                  } else {
                     MYSQL_AUTO_NOW.get(table).add(column);
                  }
               }
            }
            if (precision <= 0) {
               sql = String.format(formatter, column, notNull, _default, unique, comment);
            } else {
               formatter = formatter.replace("DATETIME", "DATETIME(%s)");
               sql = String.format(formatter, column, precision, notNull, _default, unique, comment);
            }
         }
      }
      return sql;
   }
   
   public static String getOneToOneFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         OneToOneField oneToOneFieldAnnotation = field.getAnnotation(OneToOneField.class);
         if (oneToOneFieldAnnotation != null) {
            Class modelClass = field.getDeclaringClass();
            String fieldName = ColumnUtil.getColumnName(field);
            String columnName = oneToOneFieldAnnotation.column_name();
            String tableName = TableUtil.getTableName(modelClass);
            String referencedColumn = oneToOneFieldAnnotation.referenced_column();
            String fk = "";
            String comment = getComment(oneToOneFieldAnnotation);
            boolean required = oneToOneFieldAnnotation.required();
            columnName = columnName == null ? "" : columnName;
            referencedColumn = referencedColumn == null ? "" : referencedColumn;
            if (columnName.isEmpty()) {
               fieldName = String.format("%s_id", fieldName);
            } else {
               fieldName = ColumnUtil.getColumnName(columnName);
            }
            if (referencedColumn.isEmpty()) {
               referencedColumn = "id";
            } else {
               referencedColumn = ColumnUtil.getColumnName(referencedColumn);
            }
            sql = String.format(
               "%s INT %s UNIQUE%s",
               fieldName,
               required ? "NOT NULL" : "NULL",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            String onDeleteString = "";
            if (oneToOneFieldAnnotation.on_delete().equals(Models.PROTECT)) {
               onDeleteString = " ON DELETE RESTRICT";
            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
               onDeleteString = " ON DELETE SET NULL";
            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.CASCADE)) {
               onDeleteString = " ON DELETE CASCADE";
            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
               onDeleteString = " ON DELETE SET DEFAULT";
            }
            String onUpdateString = " ON UPDATE";
            if (oneToOneFieldAnnotation.on_update().equals(Models.PROTECT)) {
               onUpdateString = " ON UPDATE RESTRICT";
            } else if (oneToOneFieldAnnotation.on_update().equals(Models.SET_NULL)) {
               onUpdateString = " ON UPDATE SET NULL";
            } else if (oneToOneFieldAnnotation.on_update().equals(Models.CASCADE)) {
               onUpdateString = " ON UPDATE CASCADE";
               if (DATABASE_ENGINE != null && DATABASE_ENGINE.equalsIgnoreCase("oracle")) {
                  onUpdateString = "";
               }
            } else if (oneToOneFieldAnnotation.on_update().equals(Models.SET_DEFAULT)) {
               onUpdateString = " ON UPDATE SET DEFAULT";
            }
            String model = oneToOneFieldAnnotation.model().getSimpleName();
            model = Model.class.getSimpleName().equals(model) ? "" : model;
            if (model.isEmpty()) {
               String packageName = field.getType().getPackage().getName();
               model = field.getType().getName();
               model = model.replace(packageName + ".", "");
            }
            String constraintName = oneToOneFieldAnnotation.constraint_name();
            constraintName = constraintName == null ? "" : constraintName.trim();
            if (constraintName.isEmpty()) {
               constraintName = String.format("fk_%s_%s", tableName, TableUtil.getTableName(field.getType()));
            }
            String references = oneToOneFieldAnnotation.references();
            references = references == null ? "" : references.trim();
            if (references.isEmpty()) {
               references = TableUtil.getTableName(field.getType());
            } else {
               references = TableUtil.getColumnName(references);
            }
            if (SQL_FOREIGN_KEYS.get(modelClass.toString()) == null) {
               SQL_FOREIGN_KEYS.put(modelClass.toString(), new ArrayList<String>());
            }
            if (MYSQL_VERSION != null) {
               StringBuilder formatter = new StringBuilder();
               formatter.append("ALTER TABLE %s ADD CONSTRAINT %s ");
               formatter.append("FOREIGN KEY(%s) REFERENCES %s(%s)%s%s");
               fk = String.format(
                  formatter.toString(),
                  tableName,
                  constraintName,
                  fieldName,
                  references,
                  referencedColumn,
                  onDeleteString,
                  onUpdateString);
            }
            List<String> fks = SQL_FOREIGN_KEYS.get(modelClass.toString());
            if (!fks.contains(fk)) {
               fks.add(fk);
            }
         }
      }
      return sql;
   }
   
   public static String getForeignKeyFieldSQL(Field field) {
      String sql = "";
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         ForeignKeyField foreignKeyFieldAnnotation = field.getAnnotation(ForeignKeyField.class);
         if (foreignKeyFieldAnnotation != null) {
            Class modelClass = field.getDeclaringClass();
            String fieldName = ColumnUtil.getColumnName(field);
            String columnName = foreignKeyFieldAnnotation.column_name();
            String referencedColumn = foreignKeyFieldAnnotation.referenced_column();
            String comment = getComment(foreignKeyFieldAnnotation);
            String tableName = TableUtil.getTableName(modelClass);
            String fk = "";
            boolean required = foreignKeyFieldAnnotation.required();
            columnName = columnName == null ? "" : columnName;
            referencedColumn = referencedColumn == null ? "" : referencedColumn;
            if (columnName.isEmpty()) {
               fieldName = String.format("%s_id", fieldName);
            } else {
               fieldName = ColumnUtil.getColumnName(columnName);
            }
            if (referencedColumn.isEmpty()) {
               referencedColumn = "id";
            } else {
               referencedColumn = ColumnUtil.getColumnName(referencedColumn);
            }
            sql = String.format(
               "%s INT %s%s",
               fieldName,
               required ? "NOT NULL" : "NULL",
               DATABASE_ENGINE.equals("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : "");
            String onDeleteString = "";
            if (foreignKeyFieldAnnotation.on_delete().equals(Models.PROTECT)) {
               onDeleteString = " ON DELETE RESTRICT";
            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
               onDeleteString = " ON DELETE SET NULL";
            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.CASCADE)) {
               onDeleteString = " ON DELETE CASCADE";
            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
               onDeleteString = " ON DELETE SET DEFAULT";
            }
            String onUpdateString = " ON UPDATE";
            if (foreignKeyFieldAnnotation.on_update().equals(Models.PROTECT)) {
               onUpdateString = " ON UPDATE RESTRICT";
            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.SET_NULL)) {
               onUpdateString = " ON UPDATE SET NULL";
            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.CASCADE)) {
               onUpdateString = " ON UPDATE CASCADE";
               if (DATABASE_ENGINE != null && DATABASE_ENGINE.equalsIgnoreCase("oracle")) {
                  onUpdateString = "";
               }
            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.SET_DEFAULT)) {
               onUpdateString = " ON UPDATE SET DEFAULT";
            }
            String model = foreignKeyFieldAnnotation.model().getSimpleName();
            model = Model.class.getSimpleName().equals(model) ? "" : model;
            if (model.isEmpty()) {
               String packageName = field.getType().getPackage().getName();
               String typeName = field.getType().getName();
               model = typeName.replace(packageName + ".", "");
            }
            String constraintName = foreignKeyFieldAnnotation.constraint_name();
            constraintName = constraintName == null ? "" : constraintName;
            if (constraintName.isEmpty()) {
               constraintName = String.format("fk_%s_%s", tableName, TableUtil.getTableName(field.getType()));
            }
            String references = foreignKeyFieldAnnotation.references();
            references = references == null ? "" : references;
            if (references.isEmpty()) {
               references = TableUtil.getTableName(field.getType());
            } else {
               references = TableUtil.getColumnName(references);
            }
            if (SQL_FOREIGN_KEYS.get(modelClass.toString()) == null) {
               SQL_FOREIGN_KEYS.put(modelClass.toString(), new ArrayList<String>());
            }
            if (DATABASE_ENGINE.trim().equalsIgnoreCase("mysql")) {
               StringBuilder formatter = new StringBuilder();
               formatter.append("ALTER TABLE %s ADD CONSTRAINT %s ");
               formatter.append("FOREIGN KEY(%s) REFERENCES %s(%s)%s%s");
               fk = String.format(
                  formatter.toString(),
                  tableName,
                  constraintName,
                  fieldName,
                  references,
                  referencedColumn,
                  onDeleteString,
                  onUpdateString);
            }
            List<String> fks = SQL_FOREIGN_KEYS.get(modelClass.toString());
            if (!fks.contains(fk)) {
               fks.add(fk);
            }
         }
      }
      return sql;
   }
   
   public static Map<String, Object> getManyToManyFieldSQL(Field field) {
      Map<String, Object> sqls = new HashMap<>();
      if (field != null && Model.class.isAssignableFrom(field.getDeclaringClass())) {
         ManyToManyField manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
         if (manyToManyFieldAnnotation != null) {
            Class clazz = null;
            Class superClazz = null;
            Class modelClass = field.getDeclaringClass();
            ParameterizedType genericType = null;
            StringBuilder format = new StringBuilder();
            String through = manyToManyFieldAnnotation.through().getSimpleName();
            through = Model.class.getSimpleName().equals(through) ? "" : through;
            String model = manyToManyFieldAnnotation.model().getSimpleName();
            String references = manyToManyFieldAnnotation.references();
            String sqlManyToManyAssociation = "";
            String modelName = modelClass.getSimpleName();
            String tableName = TableUtil.getTableName(modelClass);
            String fk = "";
            String ix = "";
            through = through != null ? through.trim() : "";
            model = Model.class.getSimpleName().equals(model) ? "" : model;
            references = references != null ? references.trim() : "";
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
            if (references.isEmpty()) {
               if (clazz != null) {
                  references = TableUtil.getTableName(clazz);
               } else {
                  references = TableUtil.getTableName(model);
               }
            }
            if (through.isEmpty()) {
               if (DATABASE_ENGINE.equalsIgnoreCase("mysql")) {
                  format.append("CREATE TABLE IF NOT EXISTS %s_%s (\n");
                  format.append("    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n");
                  format.append("    %s_id INT NOT NULL,\n");
                  format.append("    %s_id INT NOT NULL,\n");
                  format.append("    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n");
                  format.append(")");
               } else {
                  
               }
               sqlManyToManyAssociation = String.format(
                  format.toString(),
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(modelName),
                  TableUtil.getColumnName(model),
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(modelName),
                  TableUtil.getColumnName(model));
               List<String> associationTables = new ArrayList<String>();
               if (!SQL_ASSOCIATION_TABLES.contains(sqlManyToManyAssociation)) {
                  SQL_ASSOCIATION_TABLES.add(sqlManyToManyAssociation);
                  associationTables.add(sqlManyToManyAssociation);
               }
               sqls.put("association_tables", associationTables);
               String tbName = String.format("%s_%s", tableName, TableUtil.getColumnName(references));
               if (!JediEngine.GENERATED_TABLES.contains(tbName)) {
                  JediEngine.GENERATED_TABLES.add(tbName);
               }
               String onDeleteString = " ON DELETE";
               if (manyToManyFieldAnnotation.on_delete().equals(Models.PROTECT)) {
                  onDeleteString = " ON DELETE RESTRICT";
               } else if (manyToManyFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
                  onDeleteString = " ON DELETE SET NULL";
               } else if (manyToManyFieldAnnotation.on_delete().equals(Models.CASCADE)) {
                  onDeleteString = " ON DELETE CASCADE";
               } else if (manyToManyFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
                  onDeleteString = " ON DELETE SET DEFAULT";
               }
               String onUpdateString = " ON UPDATE";
               if (manyToManyFieldAnnotation.on_update().equals(Models.PROTECT)) {
                  onUpdateString = " ON UPDATE RESTRICT";
               } else if (manyToManyFieldAnnotation.on_update().equals(Models.SET_NULL)) {
                  onUpdateString = " ON UPDATE SET NULL";
               } else if (manyToManyFieldAnnotation.on_update().equals(Models.CASCADE)) {
                  onUpdateString = " ON UPDATE CASCADE";
                  if (DATABASE_ENGINE != null && DATABASE_ENGINE.equalsIgnoreCase("oracle")) {
                     onUpdateString = "";
                  }
               } else if (manyToManyFieldAnnotation.on_update().equals(Models.SET_DEFAULT)) {
                  onUpdateString = " ON UPDATE SET DEFAULT";
               }
               if (SQL_FOREIGN_KEYS.get(modelClass.toString()) == null) {
                  SQL_FOREIGN_KEYS.put(modelClass.toString(), new ArrayList<String>());
               }
               if (SQL_INDEXES.get(modelClass.toString()) == null) {
                  SQL_INDEXES.put(modelClass.toString(), new ArrayList<String>());
               }
               fk = String.format(
                  "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id)%s%s",
                  tableName,
                  TableUtil.getColumnName(references),
                  tableName,
                  TableUtil.getColumnName(references),
                  tableName,
                  TableUtil.getColumnName(modelName),
                  tableName,
                  onDeleteString,
                  onUpdateString);
               List<String> fks = SQL_FOREIGN_KEYS.get(modelClass.toString());
               List<String> foreignKeys = new ArrayList<>();
               if (!fks.contains(fk)) {
                  fks.add(fk);
                  foreignKeys.add(fk);
               }
               sqls.put("foreign_keys", foreignKeys);
               ix = String.format(
                  "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id)",
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(modelName),
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(modelName));
               List<String> ixs = SQL_INDEXES.get(modelClass.toString());
               List<String> indexes = new ArrayList<>();
               if (!ixs.contains(ix)) {
                  ixs.add(ix);
                  indexes.add(ix);
               }
               sqls.put("indexes", indexes);
               fk = String.format(
                  "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id)%s%s",
                  tableName,
                  TableUtil.getColumnName(references),
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(model),
                  TableUtil.getColumnName(references),
                  onDeleteString,
                  onUpdateString);
               if (!fks.contains(fk)) {
                  fks.add(fk);
                  foreignKeys.add(fk);
               }
               ix = String.format(
                  "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id)",
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(model),
                  tableName,
                  TableUtil.getColumnName(references),
                  TableUtil.getColumnName(model));
               if (!ixs.contains(ix)) {
                  ixs.add(ix);
                  indexes.add(ix);
               }
            }
         }
      }
      return sqls;
   }
   
   public static boolean isJediModel(Class c) {
      return c != null && jedi.db.models.Model.class.isAssignableFrom(c);
   }
   
   public static boolean isJediModelFile(File file) {
      boolean is = false;
      if (file != null && file.exists()) {
         String modelClassName = file.getAbsolutePath();
//         String modelClassName = normalizeFilePath(file.getAbsolutePath());
         if (modelClassName.endsWith("class") || modelClassName.endsWith("java")) {
            modelClassName = JediEngine.convertFilePathToClassPath(modelClassName);
            Class modelClass = null;
            try {
               modelClass = Class.forName(modelClassName);
            } catch (ClassNotFoundException classNotFoundException) {
               classNotFoundException.printStackTrace();
            }
            if (isJediModel(modelClass)) {
               is = true;
            }
         }
      }
      return is;
   }
   
   public static boolean isJediField(Field field) {
      if (field != null && isJediModel(field.getDeclaringClass())) {
         field.setAccessible(true);
         List<Class> list = Arrays.asList(JEDI_FIELD_ANNOTATION_CLASSES);
         for (Annotation annotation : field.getAnnotations()) {
            if (list.contains(annotation.annotationType())) {
               return true;
            }
         }
      }
      return false;
   }
   
   public static boolean isJediFieldAnnotation(Class fieldAnnotationClass) {
      return (fieldAnnotationClass != null && fieldAnnotationClass.isAnnotation() &&
         fieldAnnotationClass.getPackage().getName().equals("jedi.db.models"));
   }
   
   public static boolean isOneToOneField(Field field) {
      boolean response = false;
      if (JediEngine.isJediField(field) && field.getAnnotation(OneToOneField.class) != null) {
         response = true;
      }
      return response;
   }
   
   public static boolean isForeignKeyField(Field field) {
      boolean response = false;
      if (JediEngine.isJediField(field) && field.getAnnotation(ForeignKeyField.class) != null) {
         response = true;
      }
      return response;
   }
   
   public static boolean isManyToManyField(Field field) {
      boolean response = false;
      if (JediEngine.isJediField(field) && field.getAnnotation(ManyToManyField.class) != null) {
         response = true;
      }
      return response;
   }
   
   public static Class getFieldAnnotationClass(Field field) {
      if (isJediField(field)) {
         field.setAccessible(true);
         for (Class clazz : JEDI_FIELD_ANNOTATION_CLASSES) {
            Annotation annotation = field.getAnnotation(clazz);
            if (annotation != null) {
               return annotation.annotationType();
            }
         }
      }
      return null;
   }
   
   public static Object getSQL(Field field) {
      Object sql = null;
      if (isJediField(field)) {
         Class clazz = getFieldAnnotationClass(field);
         if (clazz == CharField.class) {
            sql = getCharFieldSQL(field);
         } else if (clazz == EmailField.class) {
            sql = getEmailFieldSQL(field);
         } else if (clazz == URLField.class) {
            sql = getURLFieldSQL(field);
         } else if (clazz == IPAddressField.class) {
            sql = getIPAddressFieldSQL(field);
         } else if (clazz == TextField.class) {
            sql = getTextFieldSQL(field);
         } else if (clazz == IntegerField.class) {
            sql = getIntegerFieldSQL(field);
         } else if (clazz == DecimalField.class) {
            sql = getDecimalFieldSQL(field);
         } else if (clazz == FloatField.class) {
            sql = getFloatFieldSQL(field);
         } else if (clazz == BooleanField.class) {
            sql = getBooleanFieldSQL(field);
         } else if (clazz == DateField.class) {
            sql = getDateFieldSQL(field);
         } else if (clazz == TimeField.class) {
            sql = getTimeFieldSQL(field);
         } else if (clazz == DateTimeField.class) {
            sql = getDateTimeFieldSQL(field);
         } else if (clazz == OneToOneField.class) {
            sql = getOneToOneFieldSQL(field);
         } else if (clazz == ForeignKeyField.class) {
            sql = getForeignKeyFieldSQL(field);
         } else if (clazz == ManyToManyField.class) {
            sql = getManyToManyFieldSQL(field);
         } else {
            
         }
      }
      return sql.toString().trim();
   }
   
   public static String getCreateTableSQL(JediApp app, Class<? extends jedi.db.models.Model> c) {
      StringBuilder statement = new StringBuilder();
      if (c != null) {
         String tableName = TableUtil.getTableName(c);
         if (app != null) {
//            tableName = String.format("%s_%s", app.getDBTable(), tableName);
            tableName = String.format("%s_%s", app.name().toLowerCase(), tableName);
         }
         if (!GENERATED_TABLES.contains(tableName)) {
            GENERATED_TABLES.add(tableName);
         }
         if (DATABASE_ENGINE.equals("mysql")) {
            statement.append(String.format("CREATE TABLE IF NOT EXISTS %s (\n", tableName));
         } else {
            statement.append(String.format("CREATE TABLE %s (\n", tableName));
         }
         List<Field> fields = getFields(c);
         if (fields.isEmpty()) {
            statement.append(String.format("%s%s\n", SQL_COLUMN_IDENTATION, getPrimaryKeySQL()));
         } else {
            statement.append(String.format("%s%s,\n", SQL_COLUMN_IDENTATION, getPrimaryKeySQL()));
         }
         List<Field> manyToManyFields = new ArrayList<Field>();
         Annotation a = null;
         for (Field field : fields) {
            a = field.getAnnotation(ManyToManyField.class);
            if (a != null) {
               getSQL(field);
               manyToManyFields.add(field);
            }
         }
         fields.removeAll(manyToManyFields);
         Iterator i = fields.iterator();
         Field f = null;
         while (i.hasNext()) {
            f = (Field) i.next();
            f.setAccessible(true);
            String sql = getSQL(f).toString();
            statement.append(String.format("%s%s", SQL_COLUMN_IDENTATION, sql));
            if (i.hasNext()) {
               statement.append(",\n");
            } else {
               statement.append("\n");
            }
         }
         Table tableAnnotation = c.getAnnotation(Table.class);
         if (tableAnnotation != null) {
            String engine = tableAnnotation.engine();
            String charset = tableAnnotation.charset();
            String comment = tableAnnotation.comment();
            String formatter = "";
            formatter = engine == null || engine.isEmpty() ? "" : " ENGINE = %s";
            if (!formatter.isEmpty()) {
               engine = String.format(formatter, engine);
            }
            formatter = charset == null || charset.isEmpty() ? "" : " DEFAULT CHARSET = %s";
            if (!formatter.isEmpty()) {
               charset = String.format(formatter, charset);
            }
            formatter = comment == null || comment.isEmpty() ? "" : " COMMENT '%s'";
            if (!formatter.isEmpty()) {
               comment = String.format(formatter, comment);
            }
            statement.append(String.format(")%s%s%s", engine, charset, comment));
         } else {
            statement.append(")");
         }
      }
      return statement.toString();
   }
   
   public static String getSQL(JediApp app, Class<? extends jedi.db.models.Model> c) {
      return getCreateTableSQL(app, c);
   }
   
   public static String getSQL(Class<? extends jedi.db.models.Model> c) {
      return getCreateTableSQL(null, c);
   }
   
   /**
    * Retorna a lista de instruções SQL para um determinado tipo de field em um
    * modelo.
    * 
    * @param c
    * @param a
    * @return
    */
   public static List<String> listSQL(Class<? extends jedi.db.models.Model> c, Class a) {
      List<String> l = new ArrayList<>();
      if (isJediModel(c) && isJediFieldAnnotation(a)) {
         for (Field f : c.getDeclaredFields()) {
            if (isJediField(f)) {
               l.add(getSQL(f).toString());
            }
         }
      }
      return l;
   }
   
   public static List<String> listSQL(Class<? extends jedi.db.models.Model> c) {
      List<String> l = new ArrayList<>();
      if (isJediModel(c)) {
         for (Field f : c.getDeclaredFields()) {
            if (isJediField(f)) {
               l.add(getSQL(f).toString());
            }
         }
      }
      return l;
   }
   
   public static Map<Field, String> mapSQL(Class<? extends jedi.db.models.Model> c, Class a) {
      Map<Field, String> m = new HashMap<>();
      if (isJediModel(c) && isJediFieldAnnotation(a)) {
         for (Field f : c.getDeclaredFields()) {
            if (f.getAnnotation(a) != null) {
               m.put(f, getSQL(f).toString());
            }
         }
      }
      return m;
   }
   
   public static Map<Field, String> mapSQL(Class<? extends jedi.db.models.Model> c) {
      Map<Field, String> m = new HashMap<>();
      if (isJediModel(c)) {
         for (Field f : c.getDeclaredFields()) {
            if (isJediField(f)) {
               m.put(f, getSQL(f).toString());
            }
         }
      }
      return m;
   }
   
   public static List<Field> getAllFields(List<Field> fields, Class c) {
      if (c != null) {
         Class sc = c.getSuperclass();
         /*
          * Verifies if the superclass exists and is a subclass
          * of jedi.db.models.Model class.
          */
         if (sc != null && jedi.db.models.Model.class.isAssignableFrom(sc) && jedi.db.models.Model.class != sc) {
            fields = getAllFields(fields, c.getSuperclass());
         }
         for (Field field : c.getDeclaredFields()) {
            if (isJediField(field)) {
               fields.add(field);
            }
         }
      }
      return fields;
   }
   
   public static List<Field> getAllFields(Class<? extends jedi.db.models.Model> c) {
      return getAllFields(new ArrayList<Field>(), c);
   }
   
   private static List<String> getTables(String app, String schema) {
      List<String> l = new ArrayList<>();
      String sql = String
         .format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' AND table_name LIKE '%s_%%'", schema, app);
      boolean debug = DEBUG;
      DEBUG = false;
      Object o = null;
      for (List<Map<String, Object>> e : SQLManager.raw(sql)) {
         for (Map<String, Object> m : e) {
            o = m.get("table_name");
            l.add(o.toString());
         }
      }
      DEBUG = debug;
      return l;
   }
   
   private static List<String> getTables(String app) {
      return getTables(app, DATABASE_NAME);
   }
   
   private static List<String> getTables() {
      return getTables("", DATABASE_NAME);
   }
   
   public static void usedb(String name) {
      SQLManager.connection(connect());
      SQLManager.execute("USE " + name);
   }

   public static void createdb() {
      if (DATABASE_ENGINE.equals("mysql")) {
         String engine = String.format("engine=%s", DATABASE_ENGINE);
         String host = String.format("host=%s", DATABASE_HOST);
         String port = String.format("port=%s", DATABASE_PORT);
         String user = String.format("user=%s", DATABASE_USER);
         String password = String.format("password=%s", DATABASE_PASSWORD);
         String db = DATABASE_NAME;
         String charset = DATABASE_CHARSET;
         String collate = DATABASE_COLLATE;
         String stmt = "CREATE DATABASE %s DEFAULT CHARACTER SET %s DEFAULT COLLATE %s";
         SQLManager.connection(DataSource.getConnection(engine, host, port, user, password));
         System.out.println();
         System.out.println("Creating database(s) ...\n");
         if (DATABASE_USER.equals("root") && DATABASE_ENVIRONMENTS) {
            String env = DATABASE_ENVIRONMENTS_DEVELOPMENT;
            String db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Creating database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_, charset, collate));
            env = DATABASE_ENVIRONMENTS_TEST;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Creating database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_, charset, collate));
            env = DATABASE_ENVIRONMENTS_STAGE;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Creating database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_, charset, collate));
            env = DATABASE_ENVIRONMENTS_PRODUCTION;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Creating database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_, charset, collate));
         } else {
            System.out.println(String.format("Creating database %s\n", db));
            SQLManager.raw(String.format(stmt, db, charset, collate));
         }
         System.out.println();
         MYSQL_VERSION = getMySQLVersion();
         try {
            SQLManager.getConnection().close();
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
   
   public static void dropdb() {
      if (DATABASE_USER.equals("root")) {
         String engine = String.format("engine=%s", DATABASE_ENGINE);
         String host = String.format("host=%s", DATABASE_HOST);
         String port = String.format("port=%s", DATABASE_PORT);
         String user = String.format("user=%s", DATABASE_USER);
         String password = String.format("password=%s", DATABASE_PASSWORD);
         String db = DATABASE_NAME;
         String stmt = "DROP DATABASE %s IF EXISTS";
         SQLManager.setConnection(DataSource.getConnection(engine, host, port, user, password));
         System.out.println();
         System.out.println("Dropping databases ...\n");
         if (DATABASE_USER.equals("root") && DATABASE_ENVIRONMENTS) {
            String env = DATABASE_ENVIRONMENTS_DEVELOPMENT;
            String db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Dropping database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_));
            env = DATABASE_ENVIRONMENTS_TEST;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Dropping database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_));
            env = DATABASE_ENVIRONMENTS_STAGE;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Dropping database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_));
            env = DATABASE_ENVIRONMENTS_PRODUCTION;
            db_ = String.format("%s_%s", db, env);
            System.out.println(String.format("Dropping database %s\n", db_));
            SQLManager.raw(String.format(stmt, db_));
         } else {
            System.out.println(String.format("Dropping database %s\n", db));
            SQLManager.raw(String.format(stmt, db));
         }
         System.out.println();
         try {
            SQLManager.getConnection().close();
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
   
   /**
    * Disponibiliza alguns comandos para o gerenciamento do banco de dados
    * através da linha de comando.
    * 
    * @param args
    *           argumentos recebidos via linha de comando
    */
   public static void manage(String[] args) {
      if (args != null) {
         String command = "";
         String app = "";
         if (args.length >= 2) {
            command = args[1] == null ? command : args[1].trim();
         }
         if (args.length == 3) {
            app = args[2] == null ? app : args[2].trim();
         }
         switch (command) {
            case "createdb":
               JediEngine.createdb();
               break;
            case "dropdb":
               JediEngine.dropdb();
               break;
            case "syncdb":
               // TODO fazer com que getModels() verifique se o diretório src
               // não é vazio.
               JediEngine.syncdb();
               break;
            case "reset":
               JediEngine.resetdb();
               break;
            case "sqlall":
               if (app.isEmpty()) {
                  JediEngine.sqlall();
               } else {
                  JediEngine.sqlall(app);
               }
               break;
            case "sql":
               if (app.isEmpty()) {
                  JediEngine.sql();
               } else {
                  JediEngine.sql(app);
               }
               break;
            case "sqlclear":
               if (app.isEmpty()) {
                  JediEngine.sqlclear();
               } else {
                  JediEngine.sqlclear(app);
               }
               break;
            case "sqlflush":
               if (app.isEmpty()) {
                  JediEngine.sqlflush();
               } else {
                  JediEngine.sqlflush(app);
               }
               break;
            case "sqlindexes":
               if (app.isEmpty()) {
                  JediEngine.sqlindexes();
               } else {
                  JediEngine.sqlindexes(app);
               }
               break;
            case "sqldropindexes":
               if (app.isEmpty()) {
                  JediEngine.sqldropindexes();
               } else {
                  JediEngine.sqldropindexes(app);
               }
               break;
            case "droptables":
               if (app.isEmpty()) {
                  JediEngine.droptables();
               } else {
                  JediEngine.droptables(app);
               }
               break;
            case "dbshell":
               Runtime r = Runtime.getRuntime();
               Process p;
               try {
                  String cmd = String.format(
                     "mysql -h %s -P %s -u %s -p%s %s",
                     DATABASE_HOST,
                     DATABASE_PORT,
                     DATABASE_USER,
                     DATABASE_PASSWORD,
                     DATABASE_NAME);
                  System.out.println(cmd);
                  p = r.exec(cmd);
                  p.waitFor();
                  BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
                  String line = "";
                  while ((line = b.readLine()) != null) {
                     System.out.println(line);
                  }
                  b.close();
               } catch (IOException e) {
                  e.printStackTrace();
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
               break;
            case "help":
               System.out.println("Comandos disponíveis:\n");
               System.out.println("createdb");
               System.out.println("dropdb");
               System.out.println("syncdb");
               System.out.println("reset");
               System.out.println("sql [app]");
               System.out.println("sqlall [app]");
               System.out.println("sqlclear [app]");
               System.out.println("sqlflush [app]");
               System.out.println("sqlindexes [app]");
               System.out.println("sqldropindexes [app]");
               System.out.println("droptables [app]");
               System.out.println("dbshell");
               System.out.println("help");
               break;
            default:
               System.out.println("Comando desconhecido.\nUtilize help para obter ajuda.");
         }
      }
   }
   
   /**
    * @param model
    * @return
    */
   public static <T extends Model> T loadRelationships(T model) {
      if (model != null) {
         for (Field field : getFields(model.getClass())) {
            OneToOneField oofa = field.getAnnotation(OneToOneField.class);
            ForeignKeyField ffka = field.getAnnotation(ForeignKeyField.class);
            ManyToManyField mmfa = field.getAnnotation(ManyToManyField.class);
            if (oofa != null) {
               System.out.println(oofa);
            } else if (ffka != null) {
               System.out.println(ffka);
            } else if (mmfa != null) {
               System.out.println(mmfa);
            } else {
               
            }
            System.out.println(field);
         }
      }
      return model;
   }
   
   /**
    * @param resultSet
    * @param clazz
    * @return
    */
   public static <T extends Model> QuerySet<T> convert(ResultSet resultSet, Class<T> clazz) {
      QuerySet<T> list = new QuerySet<>();
      list.setEntity(clazz);
      if (resultSet != null && clazz != null) {
         try {
            T model = null;
            while (resultSet.next()) {
               model = clazz.newInstance();
               List<Field> fields = new ArrayList<>();
               Field id = jedi.db.models.Model.class.getDeclaredField("id");
               id.setAccessible(true);
               fields.add(id);
               for (Field field : getAllFields(clazz)) {
                  field.setAccessible(true);
                  fields.add(field);
               }
               for (Field field : fields) {
                  OneToOneField oofa = field.getAnnotation(OneToOneField.class);
                  ForeignKeyField fkfa = field.getAnnotation(ForeignKeyField.class);
                  ManyToManyField mmfa = field.getAnnotation(ManyToManyField.class);
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  Class associatedModelClass = null;
                  Manager manager = null;
                  if (oofa != null || fkfa != null) {
                     if (oofa != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oofa.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? fkfa.fetch_type() : fetchType;
                     }
                     if (fetchType.equals(FetchType.EAGER)) {
                        associatedModelClass = Class.forName(field.getType().getName());
                        manager = new Manager(associatedModelClass);
                        String columnName = null;
                        Object o = null;
                        try {
                           columnName = TableUtil.getColumnName(field.getType().getSimpleName());
                           o = resultSet.getObject(String.format("%s_id", columnName));
                        } catch (SQLException e) {
                           columnName = TableUtil.getColumnName(field.getName());
                           o = resultSet.getObject(String.format("%s_id", columnName));
                        }
                        Model associatedModel = manager.get("id", o);
                        field.set(model, associatedModel);
                     } else {
                        field.set(model, null);
                     }
                  } else if (mmfa != null) {
                     fetchType = fetchType.equals(FetchType.NONE) ? mmfa.fetch_type() : fetchType;
                     if (fetchType.equals(FetchType.EAGER)) {
                        if (!JediEngine.isJediModel(mmfa.through())) {
                           continue;
                        }
                        Class superClazz = null;
                        Class _clazz = null;
                        String referencedModel = null;
                        String referencedTable = null;
                        referencedModel = mmfa.model().getSimpleName();
                        referencedModel = Model.class.getSimpleName().equals(referencedModel) ? "" : referencedModel;
                        if (referencedModel.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClazz == Model.class) {
                                 _clazz = (Class) genericType.getActualTypeArguments()[0];
                                 referencedModel = _clazz.getSimpleName();
                              }
                           }
                        }
                        referencedTable = mmfa.references();
                        if (referencedTable == null || referencedTable.trim().isEmpty()) {
                           if (_clazz != null) {
                              referencedTable = TableUtil.getTableName(_clazz);
                           } else {
                              referencedTable = TableUtil.getTableName(referencedModel);
                           }
                        }
                        String packageName = _clazz.getPackage().getName();
                        associatedModelClass = Class.forName(String.format("%s.%s", packageName, referencedModel));
                        manager = new Manager(associatedModelClass);
                        QuerySet associatedModelsQuerySet = manager.raw(
                           String.format(
                              "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                              referencedTable,
                              referencedModel.toLowerCase(),
                              TableUtil.getTableName(clazz),
                              referencedTable,
                              TableUtil.getColumnName(model.getClass()),
                              model.getId()),
                           associatedModelClass);
                        field.set(model, associatedModelsQuerySet);
                     } else {
                        field.set(model, null);
                     }
                  } else {
                     field.set(model, resultSet.getObject(TableUtil.getColumnName(field)));
                  }
               }
               model.setPersisted(true);
               list.add(model);
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
         } catch (ObjectDoesNotExistException e) {
            e.printStackTrace();
         } catch (MultipleObjectsReturnedException e) {
            e.printStackTrace();
         }
      }
      return list;
   }
   
   public static void setAppDirs(String path) {
      path = path == null ? "" : path.trim();
      File file = new File(path);
      if (file.exists()) {
         APP_ROOT_DIR = path;
         String src = "/src";
         if (MAVEN_PROJECT || GRADLE_PROJECT) {
            src = "/src/main/java";
         }
         src = src.replaceAll("/", Matcher.quoteReplacement(FILE_SEPARATOR));
         APP_SRC_DIR = String.format("%s%s", APP_ROOT_DIR, src);
         APP_LIBS_DIR = String.format("%s%s%s", APP_ROOT_DIR, FILE_SEPARATOR, "lib");
      }
   }
   
   /**
    * Método que converte um ResultSet em uma lista de Model.
    * 
    * @param resultSet
    *           ResultSet
    * @param class_
    *           Class
    * @return Uma lista de models.
    */
   public static <T extends Model> QuerySet<T> _convert(ResultSet resultSet, Class<T> class_) {
      // Cria uma lista vazia.
      QuerySet<T> list = new QuerySet<>();
      // Verifica se os parâmetros são validos.
      if (resultSet != null && class_ != null) {
         try {
            T model = null;
            // Percorre o ResultSet.
            while (resultSet.next()) {
               // Cria uma instância da classe passada como parâmetro.
               model = class_.newInstance();
               // Cria uma lista para armazenar os atributos da classe.
               List<Field> fields = new ArrayList<>();
               // Obtém os atributos e os configura como acessíveis.
               Field id = jedi.db.models.Model.class.getDeclaredField("id");
               id.setAccessible(true);
               fields.add(id);
               for (Field field : getAllFields(class_)) {
                  field.setAccessible(true);
                  fields.add(field);
               }
               // Percorre os atributos da classe.
               for (Field field : fields) {
                  // Obtém de um atributo as anotações que representam
                  // associações entre modelos:
                  // one-to-one
                  OneToOneField oofa = field.getAnnotation(OneToOneField.class);
                  // many-to-one
                  ForeignKeyField fkfa = field.getAnnotation(ForeignKeyField.class);
                  // many-to-many
                  ManyToManyField mmfa = field.getAnnotation(ManyToManyField.class);
                  // Obtém o tipo de recuperação (fetch type) de dados padrão do
                  // ORM.
                  FetchType fetchType = JediEngine.FETCH_TYPE;
                  // Cria uma referência para classe de modelo associado.
                  Class associatedModelClass = null;
                  // Cria um gerenciador de modelos.
                  Manager manager = null;
                  // Verifica qual a associação é feita pelo atributo.
                  if (oofa != null || fkfa != null) {
                     // Obtém o fetch type da anotação de associação.
                     if (oofa != null) {
                        fetchType = fetchType.equals(FetchType.NONE) ? oofa.fetch_type() : fetchType;
                     } else {
                        fetchType = fetchType.equals(FetchType.NONE) ? fkfa.fetch_type() : fetchType;
                     }
                     // Tipo de recuperação impaciente (automática).
                     if (fetchType.equals(FetchType.EAGER)) {
                        associatedModelClass = Class.forName(field.getType().getName());
                        manager = new Manager(associatedModelClass);
                        String columnName = TableUtil.getColumnName(field.getType().getSimpleName());
                        Object o = resultSet.getObject(String.format("%s_id", columnName));
                        Model associatedModel = manager.get("id", o);
                        field.set(model, associatedModel);
                     } else { // Tipo de recuperação preguiçosa (manual ou sob
                        // demanda).
                        field.set(model, null);
                     }
                  } else if (mmfa != null) { // Associação many-to-many
                     // Obtém o tipo de recuperação de dados na associação.
                     fetchType = fetchType.equals(FetchType.NONE) ? mmfa.fetch_type() : fetchType;
                     // Tipo de recuperação impaciente (automática).
                     if (fetchType.equals(FetchType.EAGER)) {
                        // Verifica se a associação many-to-many é feita
                        // por uma classe definida pelo usuário.
                        if (!JediEngine.isJediModel(mmfa.through())) {
                           continue;
                        }
                        Class superClass = null;
                        Class _class = null;
                        String referencedModel = null;
                        String referencedTable = null;
                        referencedModel = mmfa.model().getSimpleName();
                        referencedModel = Model.class.getSimpleName().equals(referencedModel) ? "" : referencedModel;
                        if (referencedModel.isEmpty()) {
                           ParameterizedType genericType = null;
                           if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                              genericType = (ParameterizedType) field.getGenericType();
                              superClass = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                              if (superClass == Model.class) {
                                 _class = (Class) genericType.getActualTypeArguments()[0];
                                 referencedModel = _class.getSimpleName();
                              }
                           }
                        }
                        referencedTable = mmfa.references();
                        if (referencedTable == null || referencedTable.trim().isEmpty()) {
                           if (_class != null) {
                              referencedTable = TableUtil.getTableName(_class);
                           } else {
                              referencedTable = TableUtil.getTableName(referencedModel);
                           }
                        }
                        String packageName = _class.getPackage().getName();
                        associatedModelClass = Class.forName(String.format("%s.%s", packageName, referencedModel));
                        manager = new Manager(associatedModelClass);
                        QuerySet associatedModelsQuerySet = manager.raw(
                           String.format(
                              "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                              referencedTable,
                              referencedModel.toLowerCase(),
                              TableUtil.getTableName(class_),
                              referencedTable,
                              TableUtil.getColumnName(model.getClass()),
                              model.getId()),
                           associatedModelClass);
                        field.set(model, associatedModelsQuerySet);
                     } else { // Tipo de recuperação preguiçosa (manual ou sob
                        // demanda).
                        field.set(model, null);
                     }
                  } else {
                     field.set(model, resultSet.getObject(TableUtil.getColumnName(field)));
                  }
               }
               model.setPersisted(true);
               list.add(model);
               // list.forEach(o -> System.out.println(o.repr()));
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
         } catch (ObjectDoesNotExistException e) {
            e.printStackTrace();
         } catch (MultipleObjectsReturnedException e) {
            e.printStackTrace();
         }
      }
      return list;
   }
   
   public static final class scripts {
      
      public static void list() {
         logger.info("Migrações:");
         SCRIPTS.forEach(System.out::println);
      }
      
      public static void show(String name) {
         name = name == null ? "" : name;
         if (!name.isEmpty() && !SCRIPTS.isEmpty()) {
            System.out.println(SCRIPTS.get(SCRIPTS.indexOf(name)));
         }
      }
      
      public static File get(String name) {
         File script = null;
         name = name == null ? "" : name;
         if (!name.isEmpty() && !SCRIPTS.isEmpty()) {
            return SCRIPTS.get(SCRIPTS.indexOf(name));
         }
         return script;
      }
   }
   
   public static boolean pathExists(String path) {
      path = path == null ? "" : path;
      if (!path.isEmpty()) {
         File file = new File(path);
         if (file.exists()) {
            return true;
         }
      }
      return false;
   }
   
   public static final boolean isAutoCommit() {
      return AUTO_COMMIT.isValue();
   }
   
   public static final boolean isAutoClose() {
      return AUTO_CLOSE.isValue();
   }
   
   public static final class Pool {
      public static boolean equals(PoolEngine pool) {
         return pool != null && DATABASE_POOL_ENGINE.equals(pool);
      }
      
      public static boolean isActive() {
         return Pool.equals(C3P0) || Pool.equals(HIKARI);
      }
      
      public static boolean isNotActive() {
         return !isActive();
      }
   }
   
   
   public static Class<? extends Model> getModelByName(String name) {
      Class<? extends Model> model = null;
      name = name == null ? "" : name;
      if (!name.isEmpty()) {
         for (Class clazz : JediEngine.getModels()) {
            if (clazz.getSimpleName().equalsIgnoreCase(name)) {
               model = clazz;
               break;
            }
         }
      }
      return model;
   }
   
   public static Class<? extends Model> getModelByField(Field field) {
      Class<? extends Model> model = field == null ? null : (Class<? extends Model>) field.getType();
      return model;
   }
   
   public static Table getTableAnnotation(Class<? extends Model> model) {
      Table table = model == null ? null : model.getAnnotation(Table.class);
      return table;
   }
   
   public static Table getTable(String modelName, String fieldName) {
      Table table = null;
      modelName = modelName == null ? "" : modelName;
      fieldName = fieldName == null ? "" : fieldName;
      if (!modelName.isEmpty() && !fieldName.isEmpty()) {
         Class<? extends Model> model = getModelByName(modelName);
         if (model != null) {
            try {
               Class<? extends Model> field = getModelByField(model.getDeclaredField(fieldName));
               table = getTableAnnotation(field);
            } catch (NoSuchFieldException | SecurityException e) {
               e.printStackTrace();
            }
         }
      }
      return table;
   }
   
   public static void changeAutoIncrement(Model model, int autoIncrement) {
      autoIncrement = autoIncrement <= 0 ? 1 : autoIncrement;
      if (model != null) {
         String sql = format("ALTER TABLE %s AUTO_INCREMENT = %d", model.tableName(), autoIncrement);
         executeSQL(sql);
      }
   }
   
   public static void changeAutoIncrement(Model model) {
      changeAutoIncrement(model, 0);
   }
   
   public static void resetAutoIncrement(Model model) {
      changeAutoIncrement(model);
   }
   
   public static void foreignKeyChecks(boolean foreignKeyChecks) {
      JediEngine.FOREIGN_KEY_CHECKS = foreignKeyChecks;
   }
   
   public static void enableForeignKeyChecks() {
      foreignKeyChecks(true);
   }
   
   public static void disableForeignKeyChecks() {
      foreignKeyChecks(false);
   }
   
   public static boolean isRootUser() {
      return JediEngine.DATABASE_USER != null && JediEngine.DATABASE_USER.equals("root");
   }
   
   public static boolean isNotRootUser() {
      return !isRootUser();
   }
   
   public static void createDatabase(String database, CharSet charset, Collation collation) {
      database = database == null ? "" : database.trim();
      if (!database.isEmpty() && charset != null && collation != null) {
         String sql = String.format("CREATE DATABASE IF NOT EXISTS %s DEFAULT CHARACTER SET %s DEFAULT COLLATE %s", database, charset, collation);
         executeSQL(sql);
      }
   }
   
   public static void createUser(String user) {
      user = user == null ? "" : user.trim();
      if (!user.isEmpty()) {
         String sql = String.format("CREATE USER IF NOT EXISTS %s", user);
         executeSQL(sql);
      }
   }
   
   public static void dropDatabase(String database) {
      database = database == null ? "" : database.trim();
      if (!database.isEmpty()) {
         String sql = String.format("DROP DATABASE IF EXISTS %s", database);
         executeSQL(sql);
      }
   }
   
   public static void dropUser(String user) {
      user = user == null ? "" : user.trim();
      if (!user.isEmpty()) {
         String sql = String.format("DROP USER IF NOT EXISTS %s", user);
         executeSQL(sql);
      }
   }
   
   public static String grantOn(String schemaObject) {
      schemaObject = schemaObject == null ? "" : schemaObject.trim();
      if (schemaObject.isEmpty()) {
         throw new IllegalArgumentException("ATENÇÃO: É necessário informar o objeto do banco de dados ao qual se está concedendo a permissão!");
      } else {
         return schemaObject;
      }
   }
   
   public static String on(String schemaObject) {
      return grantOn(schemaObject);
   }
   
   public static String grantTo(String user) {
      user = user == null ? "" : user.trim();
      if (user.isEmpty()) {
         throw new IllegalArgumentException("ATENÇÃO: É necessário informar o usuário ao qual a permissão é concedida!");
      } else {
         return user;
      }
   }
   
   public static String to(String user) {
      return grantTo(user);
   }
   
   public static String grantAt(String host) {
      host = host == null ? "" : host.trim();
      if (host.isEmpty()) {
         throw new IllegalArgumentException("ATENÇÃO: É necessário informar o computador no qual a permissão será concedida!");
      } else {
         return host;
      }
   }
   
   public static String at(String host) {
      return grantAt(host);
   }
   
   public static String identifiedBy(String password) {
      password = password == null ? "" : password.trim();
      if (password.isEmpty()) {
         throw new IllegalArgumentException("ATENÇÃO: Ao criar o usuário e conceder-lhe permissões é necessário informar a senha de acesso do mesmo!");
      } else {
         return password;
      }
   }
   
   public static Privilege[] grantPrivilege(Privilege... privilege) {
      if (privilege == null) {
         throw new IllegalArgumentException("ATENÇÃO: É necessário informar as permissões a serem concedidas!");
      } else {
         return privilege;
      }
   }
   
   public static Privilege[] privilege(Privilege... privilege) {
      return grantPrivilege(privilege);
   }
   
   public static void grantPrivileges(String on, String to, String at, String identifiedBy, Privilege... privileges) {
      on = on == null ? "" : on.trim();
      to = to == null ? "" : to.trim();
      at = at == null ? "" : at.trim();
      identifiedBy = identifiedBy == null ? "" : identifiedBy.trim();
      if (on.isEmpty() || to.isEmpty() || at.isEmpty() || identifiedBy.isEmpty()) return;
      if (privileges != null) {
         String _privileges = "";
         for (Privilege privilege : privileges) {
            if (privilege != null) {
               _privileges += String.format("%s, ", privilege);
            }
         }
         if (!_privileges.isEmpty()) {
            _privileges = _privileges.substring(0, _privileges.lastIndexOf(", "));
            String sql = String.format("GRANT %s ON %s TO '%s'@'%s' IDENTIFIED BY '%s'", _privileges, on, to, at, identifiedBy);
            executeSQL(sql);
         }
      }
   }
   
   public static void grant(String on, String to, String at, String identifiedBy, Privilege... privileges) {
      grantPrivileges(on, to, at, identifiedBy, privileges);
   }
   
   public static void grantPrivileges(String on, String to, Privilege... privileges) {
      on = on == null ? "" : on.trim();
      to = to == null ? "" : to.trim();
      if (on.isEmpty() || to.isEmpty()) return;
      if (privileges != null) {
         String _privileges = "";
         for (Privilege privilege : privileges) {
            if (privilege != null) {
               _privileges += String.format("%s, ", privilege);
            }
         }
         if (!_privileges.isEmpty()) {
            _privileges = _privileges.substring(0, _privileges.lastIndexOf(", "));
            String sql = String.format("GRANT %s ON %s TO '%s'", _privileges, on, to);
            executeSQL(sql);
         }
      }
   }
   
   public static void revokePrivileges(String on, String to, Privilege... privileges) {
      on = on == null ? "" : on.trim();
      to = to == null ? "" : to.trim();
      if (on.isEmpty() || to.isEmpty()) return;
      if (privileges != null) {
         String _privileges = "";
         for (Privilege privilege : privileges) {
            if (privilege != null) {
               _privileges += String.format("%s, ", privilege);
            }
         }
         if (!_privileges.isEmpty()) {
            _privileges = _privileges.substring(0, _privileges.lastIndexOf(", "));
            String sql = String.format("REVOKE %s ON %s FROM '%s'", _privileges, on, to);
            executeSQL(sql);
         }
      }
   }
   
   public static void revoke(String on, String to, Privilege... privilege) {
      revokePrivileges(on, to, privilege);
   }
   
   public static void useDatabase(String database) {
      database = database == null ? "" : database.trim();
      if (!database.isEmpty()) {
         String sql = String.format("USE %s", database);
         executeSQL(sql);
      }
   }
   
   public static void clean() {
      droptables();
   }
   
   public static void cleandb() {
      droptables();
   }
   
   public static void createtables() {
      syncdb();
   }
   
   public static void createTables() {
      syncdb();
   }
   
   public static void dropTables() {
      droptables();
   }
   
   public static <T extends Model> List<T> select(Class<T> clazz) {
      return SQLManager.raw(Query.selectFrom(clazz), clazz);
   }
   
}
