package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PatchFieldNotBlankValidator.class)
@Documented
public @interface ValidPatchFieldNotBlank {

    String message() default "Must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
