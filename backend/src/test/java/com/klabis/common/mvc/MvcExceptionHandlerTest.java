package com.klabis.common.mvc;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MvcExceptionHandler - error types")
class MvcExceptionHandlerTest {

    private final MvcExceptionHandler handler = new MvcExceptionHandler();

    static class PlainRuleException extends BusinessRuleViolationException {
        PlainRuleException() {
            super("Plain rule broken");
        }
    }

    static class TypedRuleException extends BusinessRuleViolationException implements UserFacingError {
        TypedRuleException() {
            super("English developer text");
        }

        @Override
        public Map<String, Object> messageParameters() {
            return Map.of("deadline", "2026-03-31");
        }

        @Override
        public String field() {
            return "votingDeadline";
        }
    }

    static class TypedConflictException extends ConflictException implements UserFacingError {
        TypedConflictException() {
            super("Already there");
        }
    }

    static class PlainConflictException extends ConflictException {
        PlainConflictException() {
            super("Conflicting");
        }
    }

    static class PlainInvalidDataException extends InvalidDataException {
        PlainInvalidDataException() {
            super("Invalid data");
        }
    }

    static class PlainNotFoundException extends ResourceNotFoundException {
        PlainNotFoundException() {
            super("Missing");
        }
    }

    static class TypedNotFoundException extends ResourceNotFoundException implements UserFacingError {
        TypedNotFoundException() {
            super("Missing member");
        }

        @Override
        public Map<String, Object> messageParameters() {
            return Map.of("memberName", "Jan Novak");
        }
    }

    static class PlainAuthorizationException extends AuthorizationException {
        PlainAuthorizationException() {
            super("Not allowed");
        }
    }

    static class ReservedParameterException extends BusinessRuleViolationException implements UserFacingError {
        ReservedParameterException() {
            super("x");
        }

        @Override
        public Map<String, Object> messageParameters() {
            return Map.of("detail", "clash");
        }
    }

    private static ProblemDetail body(ErrorResponse response) {
        return response.getBody();
    }

    @Nested
    @DisplayName("exceptions implementing UserFacingError")
    class UserFacingErrors {

        @Test
        @DisplayName("should emit type derived from class name, parameters as properties and field, keeping English detail")
        void shouldEmitTypeParametersAndField() {
            ProblemDetail problem = body(handler.handleBusinessRuleViolationException(new TypedRuleException()));

            assertThat(problem.getStatus()).isEqualTo(400);
            assertThat(problem.getType()).isEqualTo(URI.create("/errors/typed-rule"));
            assertThat(problem.getDetail()).isEqualTo("English developer text");
            assertThat(problem.getProperties()).containsEntry("deadline", "2026-03-31")
                    .containsEntry("field", "votingDeadline");
        }

        @Test
        @DisplayName("should omit field property when the error is not bound to a field")
        void shouldOmitFieldWhenNotBound() {
            ProblemDetail problem = body(handler.handleConflictException(new TypedConflictException()));

            assertThat(problem.getStatus()).isEqualTo(409);
            assertThat(problem.getType()).isEqualTo(URI.create("/errors/typed-conflict"));
            assertThat(problem.getProperties()).isNull();
        }

        @Test
        @DisplayName("should emit typed not-found with parameters")
        void shouldEmitTypedNotFound() {
            ProblemDetail problem = body(handler.handleResourceNotFoundException(new TypedNotFoundException()));

            assertThat(problem.getStatus()).isEqualTo(404);
            assertThat(problem.getType()).isEqualTo(URI.create("/errors/typed-not-found"));
            assertThat(problem.getProperties()).containsEntry("memberName", "Jan Novak");
        }
    }

    @Nested
    @DisplayName("exceptions not implementing UserFacingError")
    class GenericTypes {

        @Test
        @DisplayName("business rule violation maps to invalid-request")
        void businessRule() {
            ProblemDetail problem = body(handler.handleBusinessRuleViolationException(new PlainRuleException()));

            assertThat(problem.getStatus()).isEqualTo(400);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.INVALID_REQUEST));
            assertThat(problem.getDetail()).isEqualTo("Plain rule broken");
        }

        @Test
        @DisplayName("conflict maps to conflict")
        void conflict() {
            ProblemDetail problem = body(handler.handleConflictException(new PlainConflictException()));

            assertThat(problem.getStatus()).isEqualTo(409);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.CONFLICT));
        }

        @Test
        @DisplayName("invalid data maps to invalid-request")
        void invalidData() {
            ProblemDetail problem = body(handler.handleInvalidDataException(new PlainInvalidDataException()));

            assertThat(problem.getStatus()).isEqualTo(400);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.INVALID_REQUEST));
        }

        @Test
        @DisplayName("resource not found maps to not-found")
        void notFound() {
            ProblemDetail problem = body(handler.handleResourceNotFoundException(new PlainNotFoundException()));

            assertThat(problem.getStatus()).isEqualTo(404);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.NOT_FOUND));
        }

        @Test
        @DisplayName("authorization exception maps to forbidden")
        void authorization() {
            ProblemDetail problem = body(handler.handleAuthorizationException(new PlainAuthorizationException()));

            assertThat(problem.getStatus()).isEqualTo(403);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.FORBIDDEN));
        }

        @Test
        @DisplayName("optimistic locking failure maps to concurrent-update")
        void concurrentUpdate() {
            ProblemDetail problem = body(handler.handleOptimisticLockingFailureException(
                    new OptimisticLockingFailureException("stale")));

            assertThat(problem.getStatus()).isEqualTo(409);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.CONCURRENT_UPDATE));
        }
    }

    @Nested
    @DisplayName("internal checks")
    class InternalChecks {

        @Test
        @DisplayName("IllegalArgumentException sends fixed generic detail, never its message")
        void illegalArgument() {
            ProblemDetail problem = body(handler.handleIllegalArgumentException(
                    new IllegalArgumentException("secret internal id 42")));

            assertThat(problem.getStatus()).isEqualTo(400);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.INVALID_REQUEST));
            assertThat(problem.getDetail()).isEqualTo("Invalid request").doesNotContain("42");
        }

        @Test
        @DisplayName("IllegalStateException sends fixed generic detail, never its message")
        void illegalState() {
            ProblemDetail problem = body(handler.handleIllegalStateException(
                    new IllegalStateException("secret internal id 42")));

            assertThat(problem.getStatus()).isEqualTo(400);
            assertThat(problem.getType()).isEqualTo(URI.create(CommonErrorTypes.INVALID_REQUEST));
            assertThat(problem.getDetail()).isEqualTo("Invalid request");
        }
    }

    @Test
    @DisplayName("message parameter named like a ProblemDetail property is rejected")
    void reservedParameterName() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> handler.handleBusinessRuleViolationException(new ReservedParameterException()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("detail");
    }

    @Test
    @DisplayName("5xx framework errors get the unexpected type")
    void serverErrorGetsUnexpected() throws Exception {
        var request = new org.springframework.web.context.request.ServletWebRequest(
                new org.springframework.mock.web.MockHttpServletRequest());
        var result = handler.handleExceptionInternal(new org.springframework.web.ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE), null,
                new org.springframework.http.HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(((ProblemDetail) result.getBody()).getType()).isEqualTo(URI.create(CommonErrorTypes.UNEXPECTED));
    }

    @Test
    @DisplayName("validation failure through ConstraintViolationException uses validation-failed")
    void constraintViolation() {
        var response = handler.handleConstraintViolationException(
                new jakarta.validation.ConstraintViolationException("boom", java.util.Set.of()));

        assertThat(response.getBody().getType()).isEqualTo(URI.create(CommonErrorTypes.VALIDATION_FAILED));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
