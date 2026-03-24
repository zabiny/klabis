package com.klabis.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Email service implementation using Spring's JavaMailSender.
 *
 * <p>Handles failures gracefully - SMTP errors are logged but do not throw exceptions.
 * This ensures email failures don't break business operations (e.g., member registration).
 *
 * <p>Activated by the {@code email} Spring profile.
 */
public class JavaMailEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public JavaMailEmailService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = createMimeMessage(message);
            mailSender.send(mimeMessage);
            log.info("Email sent successfully: subject='{}', multipart={}",
                    message.subject(), message.isMultipart());
        } catch (MessagingException e) {
            log.error("Failed to create email message: subject='{}', error='{}'",
                    message.subject(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send email: subject='{}', error='{}'",
                    message.subject(), e.getMessage());
        }
    }

    private MimeMessage createMimeMessage(EmailMessage message) throws MessagingException {
        // Always use multipart=true to properly handle HTML content types
        // MimeMessageHelper needs multipart mode to set correct content-type for HTML emails
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(emailProperties.getFrom());
        helper.setTo(message.to());
        helper.setSubject(message.subject());

        if (message.isMultipart()) {
            // Multipart: both HTML and text
            helper.setText(message.textBody(), message.htmlBody());
        } else if (message.hasHtmlBody()) {
            // HTML only
            helper.setText(message.htmlBody(), true);
        } else {
            // Plain text only
            helper.setText(message.textBody());
        }

        return mimeMessage;
    }
}
