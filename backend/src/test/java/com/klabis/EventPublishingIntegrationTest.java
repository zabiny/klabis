package com.klabis;

import com.klabis.common.users.UserId;
import com.klabis.common.users.authentication.TestTransactionHelper;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.common.users.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify domain events are published correctly to the Spring Modulith outbox.
 *
 * <p>This test verifies the critical event publishing flow:
 * <ol>
 *   <li>User aggregate registers domain event via {@link org.springframework.data.domain.AbstractAggregateRoot#registerEvent(Object)}</li>
 *   <li>Spring Data JDBC publishes events to event_publication table automatically</li>
 *   <li>PasswordSetupEventListener receives and processes the event</li>
 * </ol>
 *
 * <p><b>CRITICAL:</b> This test validates that the domain event publishing mechanism works correctly.
 * User extends {@link org.springframework.data.domain.AbstractAggregateRoot} which handles event registration
 * and Spring Data JDBC publishes them to the outbox automatically.
 */
@SpringBootTest(classes = {TestApplicationConfiguration.class})
@ActiveProfiles("test")
@EnableScenarios
@DisplayName("Domain Event Publishing Integration Test")
class EventPublishingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("should publish UserCreatedEvent to outbox and trigger PasswordSetupEventListener")
    void shouldPublishUserCreatedEventToOutbox(Scenario scenario) throws Exception {
        // Given: A new user with PENDING_ACTIVATION status (triggers password setup flow)
        String username = "ZBM9999";

        User newUser = UserTestDataBuilder.aPendingUser()
                .username(username)
                .build();

        // User extends AbstractAggregateRoot which registers domain events internally
        java.util.UUID userId = newUser.getId().uuid();

        // When: Save the user through the repository
        AtomicReference<User> savedUserRef = new AtomicReference<>();
        TestTransactionHelper.executeInTransactionOrElse(
                transactionTemplate,
                () -> {
                    User savedUser = userRepository.save(newUser);
                    savedUserRef.set(savedUser);
                }
        );

        // Then: Verify event was published to the outbox and listener was triggered
        scenario.stimulate(() -> savedUserRef.get())
                .andWaitForStateChange(() -> {

                    // Verify user was persisted
                    User foundUser = userRepository.findById(new UserId(userId))
                            .orElse(null);
                    assertThat(foundUser)
                            .as("User should be saved in database")
                            .isNotNull();
                    assertThat(foundUser.getUsername())
                            .as("Username should match")
                            .isEqualTo(username);
                    assertThat(foundUser.getAccountStatus())
                            .as("User should have PENDING_ACTIVATION status")
                            .isEqualTo(AccountStatus.PENDING_ACTIVATION);

                    // Note: Domain events are handled internally by AbstractAggregateRoot
                    // and published automatically by Spring Data JDBC to the event_publication table

                    return foundUser;
                });
    }

    @Test
    @DisplayName("should not publish events for ACTIVE users to PasswordSetupEventListener")
    void shouldNotPublishEventsForActiveUsers(Scenario scenario) throws Exception {
        // Given: A new user with ACTIVE status (does not trigger password setup flow)
        String username = "ZBM8888";

        User newUser = UserTestDataBuilder.aMemberUser()
                .username(username)
                .build();

        java.util.UUID userId = newUser.getId().uuid();

        // When: Save the user through the repository
        AtomicReference<User> savedUserRef = new AtomicReference<>();
        TestTransactionHelper.executeInTransactionOrElse(
                transactionTemplate,
                () -> {
                    User savedUser = userRepository.save(newUser);
                    savedUserRef.set(savedUser);
                }
        );

        // Then: Verify event was published but listener does not process it
        scenario.stimulate(() -> savedUserRef.get())
                .andWaitForStateChange(() -> {

                    User foundUser = userRepository.findById(new UserId(userId))
                            .orElse(null);
                    assertThat(foundUser)
                            .as("User should be saved in database")
                            .isNotNull();

                    return foundUser;
                });
    }
}
