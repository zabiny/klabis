package club.klabis.shared.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class Validations {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    public static <T> void assertValid(T instance) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(instance);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + msg);
        }
    }
}
