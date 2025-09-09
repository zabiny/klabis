package club.klabis.shared.config.restapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonViewParameter {
    String name() default "view";

    JsonViewMapping[] mapping();

    String defaultValue() default "";
}
