package com.klabis.common.email;

/**
 * Service for sending emails.
 *
 * <p>This is shared infrastructure with no domain knowledge.
 * Domain-specific email logic (what to send, when to send) belongs in domain event handlers.
 *
 * <p>Implementations should handle failures gracefully and log errors without throwing exceptions,
 * to ensure email failures don't break business operations.
 */
public interface EmailService {

    /**
     * Sends an email message.
     *
     * <p>This method handles failures gracefully:
     * <ul>
     *   <li>SMTP failures are logged but don't throw exceptions</li>
     *   <li>Invalid addresses are logged and skipped</li>
     *   <li>Template errors fall back to plain-text if available</li>
     * </ul>
     *
     * @param message the email message to send
     */
    void send(EmailMessage message);
}
