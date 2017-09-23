package jedi.db.models;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoField {
   public String db_column() default "";
   public String db_tablespace() default "";
   public String default_value() default "";
   public String verbose_name() default "";
   public String comment() default "";
   public boolean primary_key() default false;
   public boolean required() default false;
   public boolean unique() default false;
   public boolean db_index() default false;
   public boolean editable() default true;
   public int size() default 11;
}
