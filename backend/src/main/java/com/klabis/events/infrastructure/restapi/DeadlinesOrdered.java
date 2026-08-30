package com.klabis.events.infrastructure.restapi;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Asserts that the annotated request's {@code deadlines} are in non-decreasing order.
 *
 * <p>This is a class-level constraint rather than an {@code @AssertTrue} accessor because the
 * requests it guards are generated from the OpenAPI spec, and a generated record cannot carry a
 * method body. The spec applies it via {@code x-klabis-class-constraint}.
 */
@Documented
@Constraint(validatedBy = DeadlinesOrderedValidator.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeadlinesOrdered {

    String message() default "Deadlines must be in non-decreasing order";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
