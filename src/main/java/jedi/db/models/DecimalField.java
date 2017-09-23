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
public @interface DecimalField {
    public boolean required() default true;
    public boolean unique() default false;
    public int scale() default 5;
    public int precision() default 2;
    public String comment() default "";
    public String default_value() default "";
}
