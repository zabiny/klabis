package com.klabis.common.email;

import com.klabis.common.templating.ThymeleafTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@Configuration
public class EmailConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfiguration.class);

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

    // TODO: migrate to TemplateRenderer interface only
    @Bean
    ThymeleafTemplateRenderer templateRenderer(TemplateEngine templateEngine) {
        return new ThymeleafTemplateRenderer(templateEngine);
    }

}
