package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarFeedToken;
import com.klabis.calendar.domain.CalendarFeedTokenRepository;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("IcalTokenService")
@ExtendWith(MockitoExtension.class)
class IcalTokenServiceTest {

    private static final PasswordEncoder REAL_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private CalendarFeedTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private IcalTokenService service;

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        service = new IcalTokenService(tokenRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("generateOrRotate()")
    class GenerateOrRotateTests {

        @Test
        @DisplayName("should create and save a new token when none exists")
        void shouldCreateTokenWhenNoneExists() {
            when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IcalTokenPort.GenerateResult result = service.generateOrRotate(USER_ID);

            assertThat(result.rawToken()).isNotBlank();
            assertThat(result.lastSetAt()).isNotNull();
            ArgumentCaptor<CalendarFeedToken> captor = ArgumentCaptor.forClass(CalendarFeedToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().isNew()).isTrue();
        }

        @Test
        @DisplayName("should rotate token when one already exists")
        void shouldRotateExistingToken() {
            CalendarFeedToken existing = CalendarFeedToken.reconstruct(USER_ID, "oldhash", "oldlook", Instant.now());
            when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(anyString())).thenReturn("newhash");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IcalTokenPort.GenerateResult result = service.generateOrRotate(USER_ID);

            assertThat(result.rawToken()).isNotBlank();
            assertThat(result.lastSetAt()).isNotNull();
            verify(tokenRepository).save(existing);
        }

        @Test
        @DisplayName("should reject null userId")
        void shouldRejectNullUserId() {
            assertThatThrownBy(() -> service.generateOrRotate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        private final PasswordEncoder realEncoder = REAL_ENCODER;
        private IcalTokenService serviceWithRealEncoder;

        @BeforeEach
        void setUpRealEncoder() {
            serviceWithRealEncoder = new IcalTokenService(tokenRepository, realEncoder);
        }

        @Test
        @DisplayName("should return userId for a valid token")
        void shouldReturnUserIdForValidToken() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, realEncoder);
            String raw = result.rawToken();
            String lookup = raw.substring(0, 8);

            when(tokenRepository.findByTokenLookup(lookup)).thenReturn(List.of(result.token()));

            Optional<UserId> validated = serviceWithRealEncoder.validate(raw);

            assertThat(validated).isPresent().contains(USER_ID);
        }

        @Test
        @DisplayName("should return empty for a token with wrong suffix")
        void shouldReturnEmptyForWrongSuffix() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_ID, realEncoder);
            String raw = result.rawToken();
            String lookup = raw.substring(0, 8);
            String tampered = lookup + "XXXXXXXXXXXXXXXXXXXXXXXXXXXX";

            when(tokenRepository.findByTokenLookup(lookup)).thenReturn(List.of(result.token()));

            Optional<UserId> validated = serviceWithRealEncoder.validate(tampered);

            assertThat(validated).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no token matches lookup")
        void shouldReturnEmptyWhenNoTokenMatchesLookup() {
            when(tokenRepository.findByTokenLookup(anyString())).thenReturn(List.of());

            Optional<UserId> validated = serviceWithRealEncoder.validate("abcdefghXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

            assertThat(validated).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null token")
        void shouldReturnEmptyForNullToken() {
            Optional<UserId> validated = serviceWithRealEncoder.validate(null);

            assertThat(validated).isEmpty();
            verifyNoInteractions(tokenRepository);
        }

        @Test
        @DisplayName("should return empty for token shorter than 8 chars")
        void shouldReturnEmptyForShortToken() {
            Optional<UserId> validated = serviceWithRealEncoder.validate("short");

            assertThat(validated).isEmpty();
            verifyNoInteractions(tokenRepository);
        }

        @Test
        @DisplayName("should not match an invalidated token after regeneration")
        void shouldNotMatchInvalidatedTokenAfterRegeneration() {
            CalendarFeedToken.Result original = CalendarFeedToken.generate(USER_ID, realEncoder);
            String oldRaw = original.rawToken();
            CalendarFeedToken token = original.token();

            token.regenerate(realEncoder);

            String lookup = oldRaw.substring(0, 8);
            when(tokenRepository.findByTokenLookup(lookup)).thenReturn(List.of(token));

            Optional<UserId> validated = serviceWithRealEncoder.validate(oldRaw);

            assertThat(validated).isEmpty();
        }
    }
}
