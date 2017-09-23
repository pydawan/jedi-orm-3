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
public @interface ManyToManyField {
   public boolean self() default false;
   public boolean symmetrical() default false;
   public boolean required() default true;
   public String column_name() default "";
   public String references() default "";
   public String referenced_column() default "";
   public String comment() default "";
   public String default_value() default "\0";
   public Models on_delete() default Models.CASCADE;
   public Models on_update() default Models.CASCADE;
   public CascadeType cascade_type() default CascadeType.ALL;
   public FetchType fetch_type() default FetchType.EAGER;
   public Class<? extends Model>model() default Model.class;
   public Class<? extends Model>through() default Model.class;
}
