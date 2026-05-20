package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.calendar.domain.CalendarFeedToken;
import com.klabis.calendar.domain.CalendarFeedTokenRepository;
import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalendarFeedToken Persistence")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO users (id, user_name, password_hash, account_status, created_at, modified_at, version) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ZBM0001', 'hash', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        "INSERT INTO users (id, user_name, password_hash, account_status, created_at, modified_at, version) VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ZBM0002', 'hash', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)"
})
class CalendarFeedTokenRepositoryTest {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);
    private static final UserId USER_A = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final UserId USER_B = new UserId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Autowired
    private CalendarFeedTokenRepository tokenRepository;

    @Nested
    @DisplayName("save() and findByUserId()")
    class SaveAndFindTests {

        @Test
        @DisplayName("should save and retrieve token by userId")
        void shouldSaveAndRetrieveToken() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_A, PASSWORD_ENCODER);
            tokenRepository.save(result.token());

            Optional<CalendarFeedToken> found = tokenRepository.findByUserId(USER_A);

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(USER_A);
            assertThat(found.get().getTokenHash()).isEqualTo(result.token().getTokenHash());
            assertThat(found.get().getTokenLookup()).isEqualTo(result.token().getTokenLookup());
            assertThat(found.get().getLastSetAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when no token exists for user")
        void shouldReturnEmptyWhenNoToken() {
            Optional<CalendarFeedToken> found = tokenRepository.findByUserId(USER_A);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should update token on regenerate (upsert)")
        void shouldUpdateTokenOnRegenerate() {
            CalendarFeedToken.Result initial = CalendarFeedToken.generate(USER_A, PASSWORD_ENCODER);
            tokenRepository.save(initial.token());

            CalendarFeedToken loaded = tokenRepository.findByUserId(USER_A).orElseThrow();
            String newRaw = loaded.regenerate(PASSWORD_ENCODER);
            tokenRepository.save(loaded);

            Optional<CalendarFeedToken> updated = tokenRepository.findByUserId(USER_A);
            assertThat(updated).isPresent();
            assertThat(PASSWORD_ENCODER.matches(newRaw, updated.get().getTokenHash())).isTrue();
            assertThat(PASSWORD_ENCODER.matches(initial.rawToken(), updated.get().getTokenHash())).isFalse();
        }
    }

    @Nested
    @DisplayName("findByTokenLookup()")
    class FindByLookupTests {

        @Test
        @DisplayName("should find token by lookup prefix")
        void shouldFindTokenByLookup() {
            CalendarFeedToken.Result result = CalendarFeedToken.generate(USER_A, PASSWORD_ENCODER);
            tokenRepository.save(result.token());
            String lookup = result.token().getTokenLookup();

            List<CalendarFeedToken> found = tokenRepository.findByTokenLookup(lookup);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getUserId()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("should return empty list when lookup does not match")
        void shouldReturnEmptyWhenLookupNotFound() {
            List<CalendarFeedToken> found = tokenRepository.findByTokenLookup("XXXXXXXX");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should find tokens for different users independently by their own lookup")
        void shouldFindTokensForDifferentUsersIndependently() {
            CalendarFeedToken.Result resultA = CalendarFeedToken.generate(USER_A, PASSWORD_ENCODER);
            CalendarFeedToken.Result resultB = CalendarFeedToken.generate(USER_B, PASSWORD_ENCODER);
            tokenRepository.save(resultA.token());
            tokenRepository.save(resultB.token());

            List<CalendarFeedToken> foundA = tokenRepository.findByTokenLookup(resultA.token().getTokenLookup());
            List<CalendarFeedToken> foundB = tokenRepository.findByTokenLookup(resultB.token().getTokenLookup());

            assertThat(foundA).hasSize(1);
            assertThat(foundA.get(0).getUserId()).isEqualTo(USER_A);
            assertThat(foundB).hasSize(1);
            assertThat(foundB.get(0).getUserId()).isEqualTo(USER_B);
        }
    }
}
