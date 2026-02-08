package com.klabis.common.email;

import java.util.Objects;

/**
 * Data transfer object representing an email message.
 *
 * <p>Supports multipart emails with both HTML and plain-text content.
 * At least one of htmlBody or textBody must be provided.
 */
public record EmailMessage(String to, String subject, String htmlBody, String textBody) {

    /**
     * Creates a new email message.
     *
     * @param to       recipient email address
     * @param subject  email subject line
     * @param htmlBody HTML version of the email body (may be null if textBody provided)
     * @param textBody plain-text version of the email body (may be null if htmlBody provided)
     */
    public EmailMessage(String to, String subject, String htmlBody, String textBody) {
        this.to = Objects.requireNonNull(to, "Recipient (to) is required");
        this.subject = Objects.requireNonNull(subject, "Subject is required");

        if (htmlBody == null && textBody == null) {
            throw new IllegalArgumentException("At least one of htmlBody or textBody must be provided");
        }

        this.htmlBody = htmlBody;
        this.textBody = textBody;
    }

    /**
     * Creates an HTML-only email message.
     *
     * @param to       recipient email address
     * @param subject  email subject line
     * @param htmlBody HTML version of the email body
     * @return new EmailMessage
     */
    public static EmailMessage html(String to, String subject, String htmlBody) {
        return new EmailMessage(to, subject, htmlBody, null);
    }

    /**
     * Creates a plain-text-only email message.
     *
     * @param to       recipient email address
     * @param subject  email subject line
     * @param textBody plain-text version of the email body
     * @return new EmailMessage
     */
    public static EmailMessage text(String to, String subject, String textBody) {
        return new EmailMessage(to, subject, null, textBody);
    }

    /**
     * Creates a multipart email message with both HTML and plain-text versions.
     *
     * @param to       recipient email address
     * @param subject  email subject line
     * @param htmlBody HTML version of the email body
     * @param textBody plain-text version of the email body
     * @return new EmailMessage
     */
    public static EmailMessage multipart(String to, String subject, String htmlBody, String textBody) {
        return new EmailMessage(to, subject, htmlBody, textBody);
    }

    public boolean hasHtmlBody() {
        return htmlBody != null;
    }

    public boolean hasTextBody() {
        return textBody != null;
    }

    public boolean isMultipart() {
        return hasHtmlBody() && hasTextBody();
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
               "to='[REDACTED]', " +
               "subject='" + subject + "', " +
               "hasHtml=" + hasHtmlBody() +
               ", hasText=" + hasTextBody() +
               '}';
    }
}
