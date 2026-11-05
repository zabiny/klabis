package com.klabis.common.mvc;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MvcExceptionHandler - ConstraintViolationException")
class MvcExceptionHandlerConstraintViolationTest {

    private final MvcExceptionHandler handler = new MvcExceptionHandler();

    // Mirrors a generated @Validated *Api interface method whose @RequestParam carries constraints.
    static class ValidatedApi {
        @SuppressWarnings("unused")
        public void validateToken(@NotBlank @Pattern(regexp = "^(?!\\s*$).+") String token) {
        }
    }

    private ConstraintViolationException violationsFor(String token) throws NoSuchMethodException {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            ExecutableValidator executableValidator = validator.forExecutables();
            Method method = ValidatedApi.class.getMethod("validateToken", String.class);
            var violations = executableValidator.validateParameters(
                    new ValidatedApi(), method, new Object[]{token});
            assertThat(violations).isNotEmpty();
            return new ConstraintViolationException(violations);
        }
    }

    @Test
    @DisplayName("should answer 400 validation-failed with the bare constraint messages, no parameter prefix")
    void shouldReportBareMessages() throws NoSuchMethodException {
        ConstraintViolationException exception = violationsFor("");

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getType().toString()).isEqualTo("/errors/validation-failed");

        @SuppressWarnings("unchecked")
        var parameterErrors = (java.util.List<String>) body.getProperties().get("parameterErrors");
        assertThat(parameterErrors).containsExactlyInAnyOrderElementsOf(
                exception.getConstraintViolations().stream().map(v -> v.getMessage()).toList());
        assertThat(parameterErrors).noneMatch(error -> error.startsWith("token: "));
    }

    @Test
    @DisplayName("should omit parameterErrors when the exception carries no violations")
    void shouldOmitParameterErrorsWhenNoViolations() {
        ResponseEntity<ProblemDetail> response =
                handler.handleConstraintViolationException(new ConstraintViolationException("boom", Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("boom");
        assertThat(body.getProperties()).isNull();
    }
}
