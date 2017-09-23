package jedi.db;

/**
 * Define as permissões que um usuário pode ter sobre objetos do
 * schema de banco de dados no MySQL.
 * 
 * @author thiagoamm
 * @version v1.0.0 11/08/2017
 * @since v1.0.0
 *
 */
public enum Privilege {
   ALL("ALL"),
   ALL_PRIVILEGES("ALL PRIVILEGES"),
   ALTER("ALTER"),
   ALTER_ROUTINE("ALTER ROUTINE"),
   CREATE("CREATE"),
   CREATE_ROUTINE("CREATE ROUTINE"),
   CREATE_TABLESPACE("CREATE TABLESPACE"),
   CREATE_TEMPORARY_TABLES("CREATE TEMPORARY TABLES"),
   CREATE_USER("CREATE USER"),
   CREATE_VIEW("CREATE VIEW"),
   DELETE("DELETE"),
   DROP("DROP"),
   EVENT("EVENT"),
   EXECUTE("EXECUTE"),
   FILE("FILE"),
   GRANT_OPTION("GRANT OPTION"),
   INDEX("INDEX"),
   INSERT("INSERT"),
   LOCK_TABLES("LOCK TABLES"),
   PROCESS("PROCESS"),
   PROXY("PROXY"),
   REFERENCES("REFERENCES"),
   RELOAD("RELOAD"),
   REPLICATION_CLIENT("REPLICATION CLIENT"),
   REPLICATION_SLAVE("REPLICATION SLAVE"),
   SELECT("SELECT"),
   SHOW_DATABASES("SHOW DATABASES"),
   SHOW_VIEW("SHOW VIEW"),
   SHUTDOWN("SHUTDOWN"),
   SUPER("SUPER"),
   TRIGGER("TRIGGER"),
   UPDATE("UPDATE"),
   USAGE("USAGE");
   
   private String value;
   
   private Privilege(String value) {
      this.value = value;
   }
   
   public String getValue() {
      return value;
   }
   
   @Override
   public String toString() {
      return value;
   }
   
}
