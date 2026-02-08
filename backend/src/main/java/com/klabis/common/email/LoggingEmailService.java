package com.klabis.common.email;

import org.apache.commons.lang3.StringUtils;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@SecondaryAdapter
public class LoggingEmailService implements EmailService {

    private final EmailProperties emailProperties;
    private static final Logger LOG = LoggerFactory.getLogger(LoggingEmailService.class);


    public LoggingEmailService(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    private EmailMessage lastEmailSent = null;

    public Optional<EmailMessage> getLastEmailSent() {
        return Optional.ofNullable(lastEmailSent);
    }

    @Override
    public void send(EmailMessage message) {
        String logMessage = """
                From: %s
                To: %s
                Subject: %s
                Message:
                %s
                """.formatted(emailProperties.getFrom(),
                message.to(),
                message.subject(),
                StringUtils.firstNonBlank(message.textBody(), message.htmlBody()));

        LOG.warn("Email sent: \n========\n{}\n=========", logMessage);
        this.lastEmailSent = message;
    }

}
