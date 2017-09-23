package jedi.db.models;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.00.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKeyField {
    public boolean unique() default false;
    public boolean required() default true;
    public String column_name() default "";
    public String constraint_name() default "";
    public String references() default "";
    public String referenced_column() default "";
    public String comment() default "";
    public String default_value() default "\0";
    // Cascade operations in database level.
    public Models on_delete() default Models.CASCADE;
    public Models on_update() default Models.CASCADE;
    // Cascade operations in application level.
    public CascadeType cascade_type() default CascadeType.ALL;
    public FetchType fetch_type() default FetchType.EAGER;
    public Class<? extends Model> model() default Model.class;
}
