package com.klabis.events.infrastructure.restapi;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The deadline-ordering rule used to be an {@code @AssertTrue} accessor on the hand-written
 * requests. Now that {@link UpdateEventRequest} is generated from the spec it cannot carry one, so
 * the rule lives in a class-level constraint applied via {@code x-klabis-class-constraint}. These
 * tests pin the behaviour that move had to preserve — including that the violation still names the
 * {@code deadlines} property, which is what the 400 response body reports.
 */
@DisplayName("@DeadlinesOrdered")
class DeadlinesOrderedValidatorTest {

    private static final LocalDate EARLIER = LocalDate.of(2026, 5, 1);
    private static final LocalDate LATER = LocalDate.of(2026, 6, 1);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<UpdateEventRequest>> validateDeadlines(
            JsonNullable<List<LocalDate>> deadlines) {
        UpdateEventRequest request = UpdateEventRequestBuilder.builder()
                .deadlines(deadlines)
                .build();
        return validator.validate(request);
    }

    /**
     * Filters out the other constraints on these requests (@NotBlank, @Size, ...) so an assertion
     * speaks only to the ordering rule under test.
     */
    private static List<String> orderingViolationPaths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation() instanceof DeadlinesOrdered)
                .map(v -> v.getPropertyPath().toString())
                .toList();
    }

    @Nested
    @DisplayName("on a generated PATCH request")
    class OnUpdateEventRequest {

        @Test
        @DisplayName("rejects deadlines that decrease, reporting the deadlines property")
        void rejectsDecreasingDeadlines() {
            var violations = validateDeadlines(JsonNullable.of(List.of(LATER, EARLIER)));

            assertThat(orderingViolationPaths(violations)).containsExactly("deadlines");
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Deadlines must be in non-decreasing order");
        }

        @Test
        @DisplayName("accepts deadlines in non-decreasing order")
        void acceptsNonDecreasingDeadlines() {
            var violations = validateDeadlines(JsonNullable.of(List.of(EARLIER, EARLIER, LATER)));

            assertThat(orderingViolationPaths(violations)).isEmpty();
        }

        @Test
        @DisplayName("accepts an absent deadlines field")
        void acceptsAbsentDeadlines() {
            var violations = validateDeadlines(JsonNullable.undefined());

            assertThat(orderingViolationPaths(violations)).isEmpty();
        }

        @Test
        @DisplayName("accepts an explicit null, which clears the deadlines rather than reordering them")
        void acceptsExplicitNull() {
            var violations = validateDeadlines(JsonNullable.of(null));

            assertThat(orderingViolationPaths(violations)).isEmpty();
        }
    }

    /**
     * The same constraint is declared on the POST body, where {@code deadlines} is a bare
     * {@code List} rather than a {@link JsonNullable}. This is the reason the validator reads the
     * component reflectively instead of taking a typed request.
     */
    @Nested
    @DisplayName("on a generated POST request")
    class OnCreateEventRequest {

        private List<String> validate(List<LocalDate> deadlines) {
            CreateEventRequest request = CreateEventRequestBuilder.builder()
                    .name("Event")
                    .organizer("ZBM")
                    .eventDate(LocalDate.of(2026, 7, 1))
                    .deadlines(deadlines)
                    .build();
            return orderingViolationPaths(validator.validate(request));
        }

        @Test
        @DisplayName("rejects deadlines that decrease, reporting the deadlines property")
        void rejectsDecreasingDeadlines() {
            assertThat(validate(List.of(LATER, EARLIER))).containsExactly("deadlines");
        }

        @Test
        @DisplayName("accepts deadlines in non-decreasing order")
        void acceptsNonDecreasingDeadlines() {
            assertThat(validate(List.of(EARLIER, LATER))).isEmpty();
        }

        @Test
        @DisplayName("accepts an omitted deadlines list")
        void acceptsNullDeadlines() {
            assertThat(validate(null)).isEmpty();
        }
    }
}
