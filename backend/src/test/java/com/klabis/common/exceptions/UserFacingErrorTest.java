package com.klabis.common.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserFacingError")
class UserFacingErrorTest {

    static class DuplicatePaymentRuleException extends RuntimeException implements UserFacingError {
    }

    static class NotAnExceptionSuffixError implements UserFacingError {
    }

    static class OverriddenException extends RuntimeException implements UserFacingError {
        @Override
        public String errorType() {
            return "custom-name";
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

    @Test
    @DisplayName("should derive kebab-case error type from class name without Exception suffix")
    void shouldDeriveErrorTypeFromClassName() {
        assertThat(new DuplicatePaymentRuleException().errorType()).isEqualTo("duplicate-payment-rule");
    }

    @Test
    @DisplayName("should keep class name without Exception suffix as is")
    void shouldDeriveErrorTypeWhenSuffixIsMissing() {
        assertThat(new NotAnExceptionSuffixError().errorType()).isEqualTo("not-an-exception-suffix-error");
    }

    @Test
    @DisplayName("should have no parameters and no field by default")
    void shouldHaveNoParametersAndFieldByDefault() {
        var error = new DuplicatePaymentRuleException();

        assertThat(error.messageParameters()).isEmpty();
        assertThat(error.field()).isNull();
    }

    @Test
    @DisplayName("should allow overriding error type, parameters and field")
    void shouldAllowOverride() {
        var error = new OverriddenException();

        assertThat(error.errorType()).isEqualTo("custom-name");
        assertThat(error.messageParameters()).containsEntry("deadline", "2026-03-31");
        assertThat(error.field()).isEqualTo("votingDeadline");
    }
}
