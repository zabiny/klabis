package club.klabis.shared.config.ddd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines use case driven by UI form. Such use case have method to fetch form data, fetch validation schema for form data and method to process submitted form data including validation.
 * <p>
 * Validation is done by JSR-303 validation of form object
 * <p>
 * Class where this annotation is used should implement {@link FormHandler} to meet expected behavior
 */
@UseCase
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface FormUseCase {
}
