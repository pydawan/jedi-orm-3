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
public @interface IntegerField {
    public boolean required() default true;
    public boolean unique() default false;
    public int size() default 11;
    public String[] choices() default {};
    public String default_value() default "";
    public String comment() default "";
}
