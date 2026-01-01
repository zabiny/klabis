package club.klabis.events.preferences;

import club.klabis.members.MemberId;
import club.klabis.members.domain.RegistrationNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static club.klabis.events.domain.EventTestFixtures.MEMBER_1;
import static club.klabis.events.domain.EventTestFixtures.MEMBER_2;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests for Preferences aggregate root")
class PreferencesTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create Preferences with member ID")
        void shouldCreatePreferencesWithMemberId() {
            // Arrange
            MemberId memberId = MEMBER_1;

            // Act
            Preferences preferences = new Preferences(memberId);

            // Assert
            assertThat(preferences).isNotNull();
            assertThat(preferences.getMemberId()).isEqualTo(memberId);
        }

        @Test
        @DisplayName("Should create default preferences with member ID via factory method")
        void shouldCreateDefaultPreferencesWithMemberId() {
            // Arrange
            MemberId memberId = MEMBER_2;

            // Act
            Preferences preferences = Preferences.defaultPreferences(memberId);

            // Assert
            assertThat(preferences).isNotNull();
            assertThat(preferences.getMemberId()).isEqualTo(memberId);
            assertThat(preferences.getSiCardNumber()).isEmpty();
            assertThat(preferences.getRegistrationNumber()).isNull();
        }

        @Test
        @DisplayName("Should initialize with no SI card number by default")
        void shouldInitializeWithNoSiCardNumber() {
            // Act
            Preferences preferences = new Preferences(MEMBER_1);

            // Assert
            assertThat(preferences.getSiCardNumber()).isEmpty();
        }

        @Test
        @DisplayName("Should initialize with null registration number by default")
        void shouldInitializeWithNullRegistrationNumber() {
            // Act
            Preferences preferences = new Preferences(MEMBER_1);

            // Assert
            assertThat(preferences.getRegistrationNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("setSiCardNumber tests")
    class SetSiCardNumberTests {

        @Test
        @DisplayName("Should successfully set SI card number")
        void shouldSetSiCardNumber() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            String siCardNumber = "123456";

            // Act
            preferences.setSiCardNumber(siCardNumber);

            // Assert
            assertThat(preferences.getSiCardNumber())
                    .isPresent()
                    .contains(siCardNumber);
        }

        @Test
        @DisplayName("Should return empty Optional when SI card number is not set")
        void shouldReturnEmptyWhenNotSet() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);

            // Act & Assert
            assertThat(preferences.getSiCardNumber()).isEmpty();
        }

        @Test
        @DisplayName("Should update existing SI card number")
        void shouldUpdateSiCardNumber() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            preferences.setSiCardNumber("111111");

            // Act
            String newSiCardNumber = "222222";
            preferences.setSiCardNumber(newSiCardNumber);

            // Assert
            assertThat(preferences.getSiCardNumber())
                    .isPresent()
                    .contains(newSiCardNumber);
        }

        @Test
        @DisplayName("Should handle setting SI card number to null")
        void shouldHandleSettingSiCardNumberToNull() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            preferences.setSiCardNumber("123456");

            // Act
            preferences.setSiCardNumber(null);

            // Assert
            assertThat(preferences.getSiCardNumber()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setRegistrationNumber tests")
    class SetRegistrationNumberTests {

        @Test
        @DisplayName("Should successfully set registration number")
        void shouldSetRegistrationNumber() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            RegistrationNumber registrationNumber = RegistrationNumber.ofRegistrationId("ABC1234");

            // Act
            preferences.setRegistrationNumber(registrationNumber);

            // Assert
            assertThat(preferences.getRegistrationNumber()).isEqualTo(registrationNumber);
        }

        @Test
        @DisplayName("Should update existing registration number")
        void shouldUpdateRegistrationNumber() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            preferences.setRegistrationNumber(RegistrationNumber.ofRegistrationId("OLD1234"));

            // Act
            RegistrationNumber newRegistrationNumber = RegistrationNumber.ofRegistrationId("NEW4564");
            preferences.setRegistrationNumber(newRegistrationNumber);

            // Assert
            assertThat(preferences.getRegistrationNumber()).isEqualTo(newRegistrationNumber);
        }

        @Test
        @DisplayName("Should return null when registration number is not set")
        void shouldReturnNullWhenNotSet() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);

            // Act & Assert
            assertThat(preferences.getRegistrationNumber()).isNull();
        }

        @Test
        @DisplayName("Should handle setting registration number to null")
        void shouldHandleSettingRegistrationNumberToNull() {
            // Arrange
            Preferences preferences = new Preferences(MEMBER_1);
            preferences.setRegistrationNumber(RegistrationNumber.ofRegistrationId("ABC1234"));

            // Act
            preferences.setRegistrationNumber(null);

            // Assert
            assertThat(preferences.getRegistrationNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should maintain all properties independently")
        void shouldMaintainAllPropertiesIndependently() {
            // Arrange
            MemberId memberId = MEMBER_1;
            String siCardNumber = "123456";
            RegistrationNumber registrationNumber = RegistrationNumber.ofRegistrationId("REG1234");

            // Act
            Preferences preferences = new Preferences(memberId);
            preferences.setSiCardNumber(siCardNumber);
            preferences.setRegistrationNumber(registrationNumber);

            // Assert
            assertThat(preferences.getMemberId()).isEqualTo(memberId);
            assertThat(preferences.getSiCardNumber()).contains(siCardNumber);
            assertThat(preferences.getRegistrationNumber()).isEqualTo(registrationNumber);
        }
    }
}
