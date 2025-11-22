package club.klabis.shared.config.hateoas.forms;

import org.springframework.core.annotation.AliasFor;
import org.springframework.hateoas.InputType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static club.klabis.shared.config.hateoas.KlabisInputTypes.RADIO_INPUT_TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@InputType("test")
public @interface InputOptions {

    Class<?> sourceEnum() default Object.class;

    @AliasFor(annotation = InputType.class, attribute = "value")
    String inputType() default RADIO_INPUT_TYPE;
}
