package com.klabis.common.users.infrastructure.listeners;

import com.klabis.common.users.UserService;
import com.klabis.common.users.application.PasswordSetupService;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.GeneratedTokenResult;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserCreatedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for UserCreatedEvent.
 *
 * <p>This handler manages password setup emails for newly created users.
 * It processes the UserCreatedEvent and sends password setup emails when:
 * <ul>
 *   <li>User account status is PENDING_ACTIVATION</li>
 *   <li>Email address is available in the event</li>
 * </ul>
 *
 * <p><b>Architecture Decision:</b> Password setup is now handled in the users module because:
 * <ul>
 *   <li>User domain owns password-related operations</li>
 *   <li>UserCreatedEvent carries email for cross-module coordination during member registration</li>
 *   <li>Keeps password setup logic co-located with user lifecycle</li>
 *   <li>Clean separation: Member module handles personal data registration, User module handles password setup</li>
 * </ul>
 *
 * <p><b>Event Flow:</b>
 * <ol>
 *   <li>UserCreatedEvent is published when a new user is created</li>
 *   <li>This handler checks if email and PENDING_ACTIVATION status are present</li>
 *   <li>Generates a password setup token for the user</li>
 *   <li>Sends password setup email with username in greeting (registration number)</li>
 * </ol>
 *
 * <p><b>Note:</b> During member registration, UserCreatedEvent is published before MemberCreatedEvent,
 * so this handler sends the password setup email during transaction processing.
 */
@Component
@PrimaryAdapter
public class UserCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventHandler.class);

    private final UserService userService;
    private final PasswordSetupService passwordSetupService;

    public UserCreatedEventHandler(
            UserService userService,
            PasswordSetupService passwordSetupService) {
        this.userService = userService;
        this.passwordSetupService = passwordSetupService;
    }

    /**
     * Handles UserCreatedEvent by sending password setup email if email is available.
     *
     * <p>The {@link ApplicationModuleListener} annotation provides:
     * <ul>
     *   <li>Event externalization via Spring Modulith's outbox pattern</li>
     *   <li>Automatic retry on failures</li>
     *   <li>Separate transaction for event processing</li>
     * </ul>
     *
     * <p><b>Important:</b> This handler only processes events for:
     * <ul>
     *   <li>Users with PENDING_ACTIVATION status (skips already active users)</li>
     *   <li>Events that include email (skips admin-created users without email)</li>
     * </ul>
     *
     * @param event the user created event containing optional email for password setup
     */
    @ApplicationModuleListener
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Processing UserCreatedEvent (eventId: {}) for user: {} (status: {})",
                event.getEventId(), event.getUserId(), event.getAccountStatus());

        // Only send password setup email for pending activation
        if (event.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
            log.debug("Skipping password setup for user {} - status is {}",
                    event.getUserId(), event.getAccountStatus());
            return;
        }

        // Only send if email is available
        if (event.getEmail().isEmpty()) {
            log.warn("No email address available in UserCreatedEvent for user {}. Skipping password setup.",
                    event.getUserId());
            return;
        }

        try {
            // Find the user account
            User user = userService.findUserByUsername(event.getUsername())
                    .orElseThrow(() -> new IllegalStateException(
                            "User not found for username: " + event.getUsername()));

            // Generate password setup token
            GeneratedTokenResult tokenResult = passwordSetupService.generateToken(user);

            // Send password setup email with username (registration number) greeting
            String email = event.getEmail().get();
            passwordSetupService.sendPasswordSetupEmailWithUsername(
                    event.getUsername(),  // Use username as greeting
                    email,
                    tokenResult.plainToken()
            );

            log.info("Password setup email sent successfully for event {} to user {} (email: {})",
                    event.getEventId(), event.getUserId(), email);
        } catch (Exception e) {
            // Log error and re-throw to trigger retry
            log.error("Failed to send password setup email for user {} (event: {})",
                    event.getUserId(), event.getEventId(), e);
            throw e;
        }
    }
}
