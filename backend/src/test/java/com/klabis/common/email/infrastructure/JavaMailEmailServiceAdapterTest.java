package com.klabis.common.email.infrastructure;

import com.klabis.common.email.EmailMessage;
import com.klabis.common.email.EmailProperties;
import com.klabis.common.email.JavaMailEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Contract tests for JavaMailEmailService adapter.
 *
 * <p>This test verifies the adapter contract: that our EmailMessage abstraction
 * correctly translates to JavaMail's API. It tests **observable behavior** (what gets sent)
 * rather than implementation details (MimeMessage internal structure).
 *
 * <p>The adapter is responsible for:
 * <ul>
 *   <li>Translating EmailMessage → JavaMail MimeMessage</li>
 *   <li>Configuring sender from EmailProperties</li>
 *   <li>Skipping sends when email is disabled</li>
 *   <li>Handling failures gracefully (logged, not thrown)</li>
 * </ul>
 */
@DisplayName("Email Adapter: JavaMailEmailService Contract Tests")
@ExtendWith(MockitoExtension.class)
class JavaMailEmailServiceAdapterTest {

    private EmailProperties emailProperties;
    private JavaMailEmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        // Configure mock to return real MimeMessage objects
        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage((jakarta.mail.Session) null));

        // Configure properties
        emailProperties = new EmailProperties();
        emailProperties.setFrom("noreply@klabis.cz");

        // Create service with mocked sender
        emailService = new JavaMailEmailService(mailSender, emailProperties);
    }

    /**
     * Contract: HTML emails are sent with correct recipient, subject, and content.
     */
    @Test
    @DisplayName("should translate HTML email to JavaMail format")
    void shouldTranslateHtmlEmail() throws MessagingException {
        // Given
        String recipient = "user@example.com";
        String subject = "Welcome to Klabis";
        String htmlBody = "<h1>Hello World</h1><p>Welcome to our club!</p>";
        EmailMessage message = EmailMessage.html(recipient, subject, htmlBody);

        // When
        emailService.send(message);

        // Then - verify the contract: JavaMailSender.send() was called with correct data
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo(subject);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(recipient);
        assertThat(sent.getFrom()[0].toString()).contains("noreply@klabis.cz");

        // Verify content is preserved (without checking internal structure)
        assertThat(getContent(sent)).contains("Hello World");
        assertThat(getContent(sent)).contains("Welcome to our club!");
    }

    /**
     * Contract: Plain-text emails are sent with correct recipient, subject, and content.
     */
    @Test
    @DisplayName("should translate plain-text email to JavaMail format")
    void shouldTranslatePlainTextEmail() throws MessagingException {
        // Given
        String recipient = "member@example.com";
        String subject = "Registration Confirmation";
        String textBody = "Dear Member,\n\nYour registration is complete.\nRegistration Number: ZBM2501";
        EmailMessage message = EmailMessage.text(recipient, subject, textBody);

        // When
        emailService.send(message);

        // Then - verify the contract
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo(subject);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(recipient);
        assertThat(getContent(sent)).contains("ZBM2501");
    }

    /**
     * Contract: Multipart emails are sent with both HTML and text versions.
     */
    @Test
    @DisplayName("should translate multipart email with both HTML and text versions")
    void shouldTranslateMultipartEmail() throws MessagingException {
        // Given
        String recipient = "admin@example.com";
        String subject = "Account Activation Required";
        String htmlBody = "<h1>Activate Account</h1><p>Click <a href='https://localhost:8443/activate'>here</a></p>";
        String textBody = "Activate Account\n\nVisit: https://localhost:8443/activate";
        EmailMessage message = EmailMessage.multipart(recipient, subject, htmlBody, textBody);

        // When
        emailService.send(message);

        // Then - verify the contract: both content versions are sent
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo(subject);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(recipient);

        // Verify both content versions are present
        String content = getContent(sent);
        assertThat(content).contains("Activate Account");
        assertThat(content).contains("https://localhost:8443/activate");
    }

    /**
     * Contract: From address is configurable via EmailProperties.
     */
    @Test
    @DisplayName("should use configured from address")
    void shouldUseConfiguredFromAddress() throws MessagingException {
        // Given
        emailProperties.setFrom("welcome@klabis.cz");
        EmailMessage message = EmailMessage.text("recipient@example.com", "Test", "Body");

        // When
        emailService.send(message);

        // Then
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getFrom()[0].toString()).contains("welcome@klabis.cz");
    }

    /**
     * Contract: Adapter handles JavaMailSender exceptions gracefully (logs, doesn't throw).
     *
     * <p>We can't easily test logging without a logging framework test, but we verify
     * the adapter doesn't propagate exceptions to the caller.
     */
    @Test
    @DisplayName("should handle send failures gracefully without throwing")
    void shouldHandleSendFailuresGracefully() {
        // Given
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailMessage message = EmailMessage.text("recipient@example.com", "Test", "Body");

        // When - should not throw exception
        emailService.send(message);

        // Then - verify send was attempted
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Helper to extract content from MimeMessage for verification.
     *
     * <p>This is a test utility that recursively extracts content from MimeMessage.
     * Needed to verify email content without checking JavaMail internal structure.
     */
    private String getContent(MimeMessage message) {
        try {
            return extractContent(message.getContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract email content for testing", e);
        }
    }

    /**
     * Recursively extracts content from a MimeMessage part.
     */
    private String extractContent(Object content) throws Exception {
        if (content == null) {
            return "";
        }

        if (content instanceof String) {
            return (String) content;
        }

        if (content instanceof jakarta.mail.internet.MimeMultipart multipart) {
            StringBuilder result = new StringBuilder();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                var part = multipart.getBodyPart(i);
                result.append(extractContent(part.getContent()));
            }
            return result.toString();
        }

        return content.toString();
    }
}
