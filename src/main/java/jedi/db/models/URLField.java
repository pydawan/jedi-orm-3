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
public @interface URLField {
   public boolean primary_key() default false;
   public boolean required() default true;
   public boolean unique() default false;
   public boolean blank() default false;
   public boolean db_index() default false;
   public boolean editable() default true;
   public int max_length() default 200;
   public String db_column() default "";
   public String comment() default "";
   public String db_tablespace() default "";
   public String default_value() default "\\0";
   public String error_messages() default "";
   public String help_text() default "";
   public String unique_for_date() default "";
   public String unique_for_month() default "";
   public String unique_for_year() default "";
   public String verbose_name() default "";
   public String[]choices() default "";
}
