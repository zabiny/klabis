package com.klabis.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WebsiteUrl Value Object Tests")
class WebsiteUrlTest {

    @Nested
    @DisplayName("Valid URLs")
    class ValidUrls {

        @Test
        @DisplayName("Should accept valid http URL")
        void shouldAcceptValidHttpUrl() {
            // Given
            String url = "http://example.com";

            // When
            WebsiteUrl websiteUrl = WebsiteUrl.of(url);

            // Then
            assertThat(websiteUrl.value()).isEqualTo(url);
        }

        @Test
        @DisplayName("Should accept valid https URL")
        void shouldAcceptValidHttpsUrl() {
            // Given
            String url = "https://example.com";

            // When
            WebsiteUrl websiteUrl = WebsiteUrl.of(url);

            // Then
            assertThat(websiteUrl.value()).isEqualTo(url);
        }

        @Test
        @DisplayName("Should accept URL with path")
        void shouldAcceptUrlWithPath() {
            assertThatCode(() -> WebsiteUrl.of("https://example.com/events/2024"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept URL with query parameters")
        void shouldAcceptUrlWithQueryParameters() {
            assertThatCode(() -> WebsiteUrl.of("https://example.com/events?year=2024&type=race"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept URL with port")
        void shouldAcceptUrlWithPort() {
            assertThatCode(() -> WebsiteUrl.of("https://example.com:8080"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept URL with subdomain")
        void shouldAcceptUrlWithSubdomain() {
            assertThatCode(() -> WebsiteUrl.of("https://events.example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept URL with fragment")
        void shouldAcceptUrlWithFragment() {
            assertThatCode(() -> WebsiteUrl.of("https://example.com/page#section"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid Schemes")
    class InvalidSchemes {

        @Test
        @DisplayName("Should reject ftp URL")
        void shouldRejectFtpUrl() {
            assertThatThrownBy(() -> WebsiteUrl.of("ftp://example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL must use http or https protocol");
        }

        @Test
        @DisplayName("Should reject file URL")
        void shouldRejectFileUrl() {
            assertThatThrownBy(() -> WebsiteUrl.of("file:///path/to/file"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL must use http or https protocol");
        }

        @Test
        @DisplayName("Should reject mailto URL")
        void shouldRejectMailtoUrl() {
            assertThatThrownBy(() -> WebsiteUrl.of("mailto:test@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL must use http or https protocol");
        }

        @Test
        @DisplayName("Should reject URL without protocol")
        void shouldRejectUrlWithoutProtocol() {
            assertThatThrownBy(() -> WebsiteUrl.of("example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URL format");
        }
    }

    @Nested
    @DisplayName("Invalid Format")
    class InvalidFormat {

        @Test
        @DisplayName("Should reject malformed URL")
        void shouldRejectMalformedUrl() {
            assertThatThrownBy(() -> WebsiteUrl.of("not a url"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URL format");
        }

        @Test
        @DisplayName("Should reject URL with spaces")
        void shouldRejectUrlWithSpaces() {
            assertThatThrownBy(() -> WebsiteUrl.of("https://example .com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URL format");
        }

        @Test
        @DisplayName("Should reject invalid characters in URL")
        void shouldRejectInvalidCharactersInUrl() {
            assertThatThrownBy(() -> WebsiteUrl.of("https://example.com/<invalid>"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URL format");
        }
    }

    @Nested
    @DisplayName("Null and Blank Rejection")
    class NullAndBlankRejection {

        @Test
        @DisplayName("Should reject null")
        void shouldRejectNull() {
            assertThatThrownBy(() -> WebsiteUrl.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL is required");
        }

        @Test
        @DisplayName("Should reject blank string")
        void shouldRejectBlankString() {
            assertThatThrownBy(() -> WebsiteUrl.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL is required");
        }

        @Test
        @DisplayName("Should reject empty string")
        void shouldRejectEmptyString() {
            assertThatThrownBy(() -> WebsiteUrl.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Website URL is required");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("Should be equal when URLs are same")
        void shouldBeEqualWhenUrlsAreSame() {
            // Given
            WebsiteUrl url1 = WebsiteUrl.of("https://example.com");
            WebsiteUrl url2 = WebsiteUrl.of("https://example.com");

            // Then
            assertThat(url1)
                    .isEqualTo(url2)
                    .hasSameHashCodeAs(url2);
        }

        @Test
        @DisplayName("Should not be equal when URLs are different")
        void shouldNotBeEqualWhenUrlsAreDifferent() {
            // Given
            WebsiteUrl url1 = WebsiteUrl.of("https://example.com");
            WebsiteUrl url2 = WebsiteUrl.of("https://other.com");

            // Then
            assertThat(url1).isNotEqualTo(url2);
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("Should trim whitespace from URL")
        void shouldTrimWhitespaceFromUrl() {
            // Given
            String url = "  https://example.com  ";

            // When
            WebsiteUrl websiteUrl = WebsiteUrl.of(url);

            // Then
            assertThat(websiteUrl.value()).isEqualTo("https://example.com");
        }
    }
}
