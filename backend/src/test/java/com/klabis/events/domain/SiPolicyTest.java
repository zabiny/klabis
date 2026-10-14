package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SiPolicy value object")
class SiPolicyTest {

    @Test
    @DisplayName("default policy requires an SI card and allows rental")
    void defaultPolicy() {
        SiPolicy policy = SiPolicy.defaultPolicy();

        assertThat(policy.usage()).isEqualTo(SiUsage.REQUIRED);
        assertThat(policy.rentalAllowed()).isTrue();
    }

    @Test
    @DisplayName("keeps rental allowed when SI is optional or required")
    void keepsRentalWhenSiUsed() {
        assertThat(SiPolicy.of(SiUsage.OPTIONAL, true).rentalAllowed()).isTrue();
        assertThat(SiPolicy.of(SiUsage.REQUIRED, true).rentalAllowed()).isTrue();
        assertThat(SiPolicy.of(SiUsage.REQUIRED, false).rentalAllowed()).isFalse();
    }

    @Test
    @DisplayName("forces rental off when SI is not used")
    void forcesRentalOffWhenNotUsed() {
        SiPolicy policy = SiPolicy.of(SiUsage.NOT_USED, true);

        assertThat(policy.usage()).isEqualTo(SiUsage.NOT_USED);
        assertThat(policy.rentalAllowed()).isFalse();
    }

    @Test
    @DisplayName("rejects missing usage")
    void rejectsNullUsage() {
        assertThatThrownBy(() -> SiPolicy.of(null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    private static final SiCardNumber SI = SiCardNumber.of("123456");

    @Test
    @DisplayName("validate accepts anything when SI is not used")
    void validateNotUsed() {
        SiPolicy policy = SiPolicy.of(SiUsage.NOT_USED, false);

        policy.validate(null, false);
        policy.validate(SI, true);
    }

    @Test
    @DisplayName("validate accepts empty or valid number when SI is optional")
    void validateOptional() {
        SiPolicy policy = SiPolicy.of(SiUsage.OPTIONAL, false);

        policy.validate(null, false);
        policy.validate(SI, false);
    }

    @Test
    @DisplayName("validate accepts a valid number when SI is required")
    void validateRequiredWithNumber() {
        SiPolicy.of(SiUsage.REQUIRED, false).validate(SI, false);
    }

    @Test
    @DisplayName("validate accepts rental instead of a number when SI is required and rental allowed")
    void validateRequiredWithRental() {
        SiPolicy.of(SiUsage.REQUIRED, true).validate(null, true);
    }

    @Test
    @DisplayName("validate rejects a missing number when SI is required and no rental is requested")
    void validateRequiredWithoutNumberOrRental() {
        assertThatThrownBy(() -> SiPolicy.of(SiUsage.REQUIRED, true).validate(null, false))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SI card number is required");
        assertThatThrownBy(() -> SiPolicy.of(SiUsage.REQUIRED, false).validate(null, false))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SI card number is required");
    }

    @Test
    @DisplayName("validate rejects rental when the event does not allow it")
    void validateRentalNotAllowed() {
        assertThatThrownBy(() -> SiPolicy.of(SiUsage.REQUIRED, false).validate(SI, true))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("rental");
        assertThatThrownBy(() -> SiPolicy.of(SiUsage.OPTIONAL, false).validate(null, true))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("rental");
    }
}
