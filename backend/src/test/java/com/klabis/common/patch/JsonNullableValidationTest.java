package com.klabis.common.patch;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeAll;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableJakartaValueExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that standard JSR 303 constraint annotations work on JsonNullable fields
 * via JsonNullableJakartaValueExtractor registered through META-INF/services.
 * <p>
 * The validator is built with explicit extractor registration to guarantee the
 * META-INF/services discovery mechanism is active in the test JVM classloader context.
 */
@DisplayName("JsonNullable — standard JSR 303 validation")
class JsonNullableValidationTest {

    private static Validator validator;

    private record NestedDto(
            @NotBlank(message = "nested field must not be blank")
            String name
    ) {}

    private record TestRequest(
            @NotBlank(message = "notBlank field must not be blank")
            JsonNullable<String> notBlankField,

            @Size(max = 5, message = "size field must not exceed 5 characters")
            JsonNullable<String> sizeField,

            @Pattern(regexp = "[0-9]+", message = "pattern field must contain only digits")
            JsonNullable<String> patternField,

            @Valid
            JsonNullable<NestedDto> nestedField
    ) {}

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .addValueExtractor(new JsonNullableJakartaValueExtractor())
                .buildValidatorFactory()
                .getValidator();
    }

    private Set<ConstraintViolation<TestRequest>> validateField(TestRequest request, String propertyName) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().startsWith(propertyName))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Nested
    @DisplayName("Not-provided JsonNullable — constraints never evaluated")
    class NotProvidedTests {

        @Test
        @DisplayName("Not-provided field with @NotBlank produces no violations")
        void notProvided_notBlank_noViolations() {
            var request = new TestRequest(
                    JsonNullable.undefined(),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "notBlankField");

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Not-provided field with @Size produces no violations")
        void notProvided_size_noViolations() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.undefined(),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "sizeField");

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Not-provided field with @Pattern produces no violations")
        void notProvided_pattern_noViolations() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.undefined(),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "patternField");

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Provided null — only @NotBlank detects it; @Size and @Pattern skip null per JSR 303 spec")
    class ProvidedNullTests {

        @Test
        @DisplayName("Provided null + @NotBlank produces violation")
        void providedNull_notBlank_violation() {
            var request = new TestRequest(
                    JsonNullable.of(null),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "notBlankField");

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("notBlank field must not be blank");
        }

        @Test
        @DisplayName("Provided null + @Size produces no violation (JSR 303 null-permissive)")
        void providedNull_size_noViolation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of(null),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "sizeField");

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Provided null + @Pattern produces no violation (JSR 303 null-permissive)")
        void providedNull_pattern_noViolation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of(null),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "patternField");

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Provided blank string — @NotBlank detects it")
    class ProvidedBlankTests {

        @Test
        @DisplayName("Provided blank + @NotBlank produces violation")
        void providedBlank_notBlank_violation() {
            var request = new TestRequest(
                    JsonNullable.of("   "),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "notBlankField");

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("notBlank field must not be blank");
        }
    }

    @Nested
    @DisplayName("@Size constraint on provided values")
    class SizeTests {

        @Test
        @DisplayName("Provided too-long string + @Size(max=5) produces violation")
        void providedTooLong_size_violation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("toolong"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "sizeField");

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("size field must not exceed 5 characters");
        }

        @Test
        @DisplayName("Provided valid-length string + @Size(max=5) produces no violation")
        void providedValidLength_size_noViolation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "sizeField");

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("@Pattern constraint on provided values")
    class PatternTests {

        @Test
        @DisplayName("Provided non-matching string + @Pattern produces violation")
        void providedNonMatching_pattern_violation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("abc123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "patternField");

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("pattern field must contain only digits");
        }

        @Test
        @DisplayName("Provided matching string + @Pattern produces no violation")
        void providedMatching_pattern_noViolation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("12345"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "patternField");

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("@Valid cascade into nested DTO inside JsonNullable")
    class ValidCascadeTests {

        @Test
        @DisplayName("Provided valid nested DTO produces no violations")
        void providedValidNested_noViolations() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.of(new NestedDto("valid name"))
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "nestedField");

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Provided invalid nested DTO produces violation for nested field")
        void providedInvalidNested_violation() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.of(new NestedDto(""))
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "nestedField");

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("nested field must not be blank");
        }

        @Test
        @DisplayName("Not-provided nested JsonNullable produces no violations")
        void notProvided_nested_noViolations() {
            var request = new TestRequest(
                    JsonNullable.of("ok"),
                    JsonNullable.of("ok"),
                    JsonNullable.of("123"),
                    JsonNullable.undefined()
            );

            Set<ConstraintViolation<TestRequest>> violations = validateField(request, "nestedField");

            assertThat(violations).isEmpty();
        }
    }
}
