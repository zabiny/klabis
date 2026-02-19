package com.klabis.config;

import com.klabis.common.email.EmailProperties;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.LoggingEmailService;
import com.klabis.common.email.infrastructure.JavaMailEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
class EmailServiceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceConfiguration.class);

    @Bean
    public EmailService emailService(EmailProperties emailProperties, ObjectProvider<JavaMailSender> javaMailSenderProvider) {
        if (!emailProperties.isEnabled()) {
            LOG.info("Email service disabled, returning LoggingEmailService");
            return new LoggingEmailService(emailProperties);
        }

        JavaMailSender sender = javaMailSenderProvider.getIfAvailable(() -> null);
        if (sender == null) {
            LOG.warn(
                    "JavaMailSender has not been initialized (are SMTP server properties configured?) - returning LoggingEmailService");
            return new LoggingEmailService(emailProperties);
        } else {
            LOG.info("JavaMailSender has been initialized, returning JavaMailEmailService");
            return new JavaMailEmailService(sender, emailProperties);
        }
    }

}
