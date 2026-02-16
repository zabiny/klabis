package com.klabis.common.ui;

import org.springframework.core.annotation.AliasFor;
import org.springframework.hateoas.InputType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
@InputType(value = "")
public @interface HalForms {
    enum Access {
        /**
         * property will be present and will have isReadOnly=true in HalForms metadata
         */
        READ_ONLY,
        /**
         * Property won't be present in HalForms metadata
         */
        NONE,
        /**
         * property will be present and will have isReadOnly=false in HalForms metadata
         */
        READ_WRITE,
        /**
         * property will have default behavior. Class property with setter will have isReadOnly=false, without setter will have isReadOnly=true. Record component will have isReadOnly=false.
         */
        DEFAULT
    }

    /**
     * Overrides readOnly value for property
     * @return
     */
    Access access() default Access.DEFAULT;

    @AliasFor(annotation = InputType.class, attribute = "value", value = "value")
    String formInputType() default "";
}
