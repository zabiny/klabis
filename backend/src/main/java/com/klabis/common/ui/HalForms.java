package com.klabis.common.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface HalForms {
    enum Access {
        READ_ONLY, WRITE_ONLY, NONE, READ_WRITE
    }

    /**
     * Overrides readOnly value for property
     * @return
     */
    Access access() default Access.READ_WRITE;
}
