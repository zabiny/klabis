package com.klabis.common.users.passwordsetup;

import com.klabis.common.users.*;
import com.klabis.common.users.persistence.PasswordSetupTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
//@ActiveProfiles("test")
@TestPropertySource(properties = {"jasypt.encryptor.password=example"})
//@CleanupTestData
@DisplayName("Password setup token integration with User events")
@Sql(statements = {
        "INSERT INTO users (id, user_name, password_hash, account_status) VALUES ('11111111-1111-1111-1111-111111111111', 'test', 'hashedpw', 'PENDING_ACTIVATION')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserEventsListeningTests {

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    private final UserId PENDING_ACTIVATION_USER = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    @Test
    @DisplayName("should create password token for user without password")
    void shouldCreateCalendarItemWhenEventIsPublished(Scenario scenario, PublishedEvents events) {
        // Given: Event data
        final UserId userId = PENDING_ACTIVATION_USER;

        // When & Then: CalendarItem should be created automatically
        scenario.publish(new UserCreatedEvent(UUID.randomUUID(), userId.uuid(), "test", AccountStatus.PENDING_ACTIVATION, Instant.now(), "test@email.com"))
                .andWaitForStateChange(() -> !passwordSetupTokenRepository.findActiveTokensForUser(userId).isEmpty())
                .andVerify(isPresent -> {
                    PasswordSetupToken token = passwordSetupTokenRepository.findActiveTokensForUser(userId).get(0);

                    assertThat(token.getUserId()).isEqualTo(userId);
                    assertThat(token.getExpiresAt()).isInTheFuture();
                    assertThat(token.getTokenHash()).isNotNull().extracting(TokenHash::getValue).isNotNull();
                });
    }

}
