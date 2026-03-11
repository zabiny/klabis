package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrainerLicense value object")
class TrainerLicenseTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(1);

    @Nested
    @DisplayName("of() factory method")
    class FactoryMethod {

        @Test
        @DisplayName("should create TrainerLicense with T1 level")
        void shouldCreateWithT1Level() {
            TrainerLicense license = TrainerLicense.of(TrainerLevel.T1, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(TrainerLevel.T1);
            assertThat(license.validityDate()).isEqualTo(FUTURE_DATE);
        }

        @Test
        @DisplayName("should create TrainerLicense with T2 level")
        void shouldCreateWithT2Level() {
            TrainerLicense license = TrainerLicense.of(TrainerLevel.T2, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(TrainerLevel.T2);
        }

        @Test
        @DisplayName("should create TrainerLicense with T3 level")
        void shouldCreateWithT3Level() {
            TrainerLicense license = TrainerLicense.of(TrainerLevel.T3, FUTURE_DATE);

            assertThat(license.level()).isEqualTo(TrainerLevel.T3);
        }

        @Test
        @DisplayName("should throw when level is null")
        void shouldThrowWhenLevelIsNull() {
            assertThatThrownBy(() -> TrainerLicense.of(null, FUTURE_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("level");
        }

        @Test
        @DisplayName("should throw when validityDate is null")
        void shouldThrowWhenValidityDateIsNull() {
            assertThatThrownBy(() -> TrainerLicense.of(TrainerLevel.T1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validity date");
        }
    }
}
