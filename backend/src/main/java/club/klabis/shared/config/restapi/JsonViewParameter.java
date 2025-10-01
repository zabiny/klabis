package club.klabis.shared.config.restapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Automatically applies JsonView based on given request parameter.
 * <p>
 * Parameter is also properly documented in generated OpenAPI docs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface JsonViewParameter {
    String name() default "view";

    JsonViewMapping[] mapping();

    String defaultValue() default "";

    String description() default "Defines how many data are returned for every item";
}
