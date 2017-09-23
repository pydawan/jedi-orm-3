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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
   public String name() default "\\0";
   public String engine() default ""; // criar Enum
   public String charset() default ""; // criar Enum
   public String comment() default "";
}
