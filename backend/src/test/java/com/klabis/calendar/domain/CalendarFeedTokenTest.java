package com.klabis.calendar.domain;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarFeedToken domain")
class CalendarFeedTokenTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);
    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("should create token with non-null fields")
        void shouldCreateTokenWithNonNullFields() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);

            assertThat(result.rawToken()).isNotBlank();
            assertThat(result.token().getUserId()).isEqualTo(USER_ID);
            assertThat(result.token().getTokenHash()).isNotBlank();
            assertThat(result.token().getTokenLookup()).isNotBlank();
            assertThat(result.token().getLastSetAt()).isNotNull();
        }

        @Test
        @DisplayName("should produce a raw token that verifies against stored hash")
        void shouldProduceRawTokenVerifiableAgainstHash() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);

            assertThat(PASSWORD_ENCODER.matches(result.rawToken(), result.token().getTokenHash())).isTrue();
        }

        @Test
        @DisplayName("should use first 8 chars of raw token as lookup prefix")
        void shouldUseFirst8CharsAsLookup() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);

            assertThat(result.token().getTokenLookup()).isEqualTo(result.rawToken().substring(0, 8));
        }

        @Test
        @DisplayName("should mark token as new")
        void shouldMarkTokenAsNew() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);

            assertThat(result.token().isNew()).isTrue();
        }

        @Test
        @DisplayName("should generate two different tokens on subsequent calls")
        void shouldGenerateDifferentTokensOnSubsequentCalls() {
            CalendarFeedToken.Result first = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);
            CalendarFeedToken.Result second = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);

            assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        }

        @Test
        @DisplayName("should reject null userId")
        void shouldRejectNullUserId() {
            assertThatThrownBy(() -> CalendarFeedToken.generate(null, PASSWORD_ENCODER))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null passwordEncoder")
        void shouldRejectNullPasswordEncoder() {
            assertThatThrownBy(() -> CalendarFeedToken.generate(USER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("regenerate()")
    class RegenerateTests {

        private CalendarFeedToken token;
        private String originalRaw;

        @BeforeEach
        void setUp() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, PASSWORD_ENCODER);
            token = result.token();
            originalRaw = result.rawToken();
        }

        @Test
        @DisplayName("should return a new raw token different from the original")
        void shouldReturnDifferentToken() {
            String newRaw = token.regenerate(PASSWORD_ENCODER);

            assertThat(newRaw).isNotEqualTo(originalRaw);
        }

        @Test
        @DisplayName("should update hash so old token no longer matches")
        void shouldInvalidateOldToken() {
            token.regenerate(PASSWORD_ENCODER);

            assertThat(PASSWORD_ENCODER.matches(originalRaw, token.getTokenHash())).isFalse();
        }

        @Test
        @DisplayName("should update hash so new token matches")
        void shouldNewTokenMatchHash() {
            String newRaw = token.regenerate(PASSWORD_ENCODER);

            assertThat(PASSWORD_ENCODER.matches(newRaw, token.getTokenHash())).isTrue();
        }

        @Test
        @DisplayName("should update lookup prefix to match new token")
        void shouldUpdateLookupPrefix() {
            String newRaw = token.regenerate(PASSWORD_ENCODER);

            assertThat(token.getTokenLookup()).isEqualTo(newRaw.substring(0, 8));
        }

        @Test
        @DisplayName("should update lastSetAt to a recent timestamp")
        void shouldUpdateLastSetAt() {
            Instant before = Instant.now();
            token.regenerate(PASSWORD_ENCODER);

            assertThat(token.getLastSetAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should mark token as not new")
        void shouldMarkTokenAsNotNew() {
            CalendarFeedToken token = CalendarFeedToken.reconstruct(
                    USER_ID, "hash", "lookup12", Instant.now());

            assertThat(token.isNew()).isFalse();
        }

        @Test
        @DisplayName("should preserve all fields")
        void shouldPreserveAllFields() {
            Instant lastSetAt = Instant.parse("2026-01-01T10:00:00Z");
            CalendarFeedToken token = CalendarFeedToken.reconstruct(USER_ID, "hash", "lookup123", lastSetAt);

            assertThat(token.getUserId()).isEqualTo(USER_ID);
            assertThat(token.getTokenHash()).isEqualTo("hash");
            assertThat(token.getTokenLookup()).isEqualTo("lookup123");
            assertThat(token.getLastSetAt()).isEqualTo(lastSetAt);
        }
    }
}
