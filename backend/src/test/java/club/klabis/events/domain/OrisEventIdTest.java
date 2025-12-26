package club.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests for OrisId value object")
class OrisEventIdTest {

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should successfully create OrisId with positive value")
        void shouldCreateOrisIdWithPositiveValue() {
            // Act
            OrisEventId orisId = new OrisEventId(12345);

            // Assert
            assertThat(orisId).isNotNull();
            assertThat(orisId.value()).isEqualTo(12345);
        }

        @Test
        @DisplayName("Should throw exception when value is zero")
        void shouldThrowExceptionWhenValueIsZero() {
            // Act & Assert
            assertThatThrownBy(() -> new OrisEventId(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid orisId: 0")
                    .hasMessageContaining("must be higher than 0");
        }

        @Test
        @DisplayName("Should throw exception when value is negative")
        void shouldThrowExceptionWhenValueIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> new OrisEventId(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid orisId: -1")
                    .hasMessageContaining("must be higher than 0");
        }

        @Test
        @DisplayName("Should store correct value when created")
        void shouldStoreCorrectValue() {
            // Arrange
            int expectedValue = 54321;

            // Act
            OrisEventId orisId = new OrisEventId(expectedValue);

            // Assert
            assertThat(orisId.value()).isEqualTo(expectedValue);
        }
    }

    @Nested
    @DisplayName("createEventUrl method tests")
    class CreateEventUrlTests {

        @Test
        @DisplayName("Should create correct ORIS event URL")
        void shouldCreateCorrectOrisEventUrl() {
            // Arrange
            OrisEventId orisId = new OrisEventId(12345);

            // Act
            URL url = orisId.createEventUrl();

            // Assert
            assertThat(url).isNotNull();
            assertThat(url.toString()).isEqualTo("https://oris.orientacnisporty.cz/Zavod?id=12345");
        }

        @Test
        @DisplayName("Should include ORIS ID in URL")
        void shouldIncludeOrisIdInUrl() {
            // Arrange
            OrisEventId orisId = new OrisEventId(99999);

            // Act
            URL url = orisId.createEventUrl();

            // Assert
            assertThat(url.toString()).contains("id=99999");
        }

        @Test
        @DisplayName("Should use HTTPS protocol")
        void shouldUseHttpsProtocol() {
            // Arrange
            OrisEventId orisId = new OrisEventId(12345);

            // Act
            URL url = orisId.createEventUrl();

            // Assert
            assertThat(url.getProtocol()).isEqualTo("https");
        }

        @Test
        @DisplayName("Should point to correct ORIS domain")
        void shouldPointToCorrectOrisDomain() {
            // Arrange
            OrisEventId orisId = new OrisEventId(12345);

            // Act
            URL url = orisId.createEventUrl();

            // Assert
            assertThat(url.getHost()).isEqualTo("oris.orientacnisporty.cz");
        }

        @Test
        @DisplayName("Should handle URL creation for various ORIS IDs")
        void shouldHandleUrlCreationForVariousIds() {
            // Arrange & Act & Assert
            assertThatCode(() -> new OrisEventId(1).createEventUrl()).doesNotThrowAnyException();
            assertThatCode(() -> new OrisEventId(100).createEventUrl()).doesNotThrowAnyException();
            assertThatCode(() -> new OrisEventId(99999).createEventUrl()).doesNotThrowAnyException();
            assertThatCode(() -> new OrisEventId(Integer.MAX_VALUE).createEventUrl()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return valid URL object")
        void shouldReturnValidURL() {
            // Arrange
            OrisEventId orisId = new OrisEventId(12345);

            // Act
            URL url = orisId.createEventUrl();

            // Assert
            assertThat(url)
                    .isNotNull()
                    .isInstanceOf(URL.class);
        }
    }

    @Nested
    @DisplayName("Record tests (equals, hashCode, toString)")
    class RecordTests {

        @Test
        @DisplayName("Should implement value object equality (same value = equal)")
        void shouldImplementValueObjectEquality() {
            // Arrange
            OrisEventId orisEventId1 = new OrisEventId(12345);
            OrisEventId orisEventId2 = new OrisEventId(12345);
            OrisEventId orisEventId3 = new OrisEventId(54321);

            // Assert
            assertThat(orisEventId1).isEqualTo(orisEventId2);
            assertThat(orisEventId1).isNotEqualTo(orisEventId3);
        }
    }
}
