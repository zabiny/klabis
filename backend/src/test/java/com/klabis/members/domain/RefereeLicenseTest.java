package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RefereeLicense value object")
class RefereeLicenseTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(1);

    @Nested
    @DisplayName("of() factory method")
    class FactoryMethod {

        @Test
        @DisplayName("should create RefereeLicense with R1 level")
        void shouldCreateWithR1Level() {
            RefereeLicense license = RefereeLicense.of(RefereeLevel.R1, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(RefereeLevel.R1);
            assertThat(license.validityDate()).isEqualTo(FUTURE_DATE);
        }

        @Test
        @DisplayName("should create RefereeLicense with R2 level")
        void shouldCreateWithR2Level() {
            RefereeLicense license = RefereeLicense.of(RefereeLevel.R2, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(RefereeLevel.R2);
        }

        @Test
        @DisplayName("should create RefereeLicense with R3 level")
        void shouldCreateWithR3Level() {
            RefereeLicense license = RefereeLicense.of(RefereeLevel.R3, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(RefereeLevel.R3);
        }

        @Test
        @DisplayName("should throw when level is null")
        void shouldThrowWhenLevelIsNull() {
            assertThatThrownBy(() -> RefereeLicense.of(null, FUTURE_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("level");
        }

        @Test
        @DisplayName("should throw when validityDate is null")
        void shouldThrowWhenValidityDateIsNull() {
            assertThatThrownBy(() -> RefereeLicense.of(RefereeLevel.R1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validity date");
        }
    }
}
