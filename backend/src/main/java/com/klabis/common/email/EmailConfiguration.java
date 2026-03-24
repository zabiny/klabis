package com.klabis.common.email;

import com.klabis.common.templating.ThymeleafTemplateRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@Configuration
public class EmailConfiguration {

    @Bean
    @Profile("email")
    public EmailService javaMailEmailService(JavaMailSender javaMailSender, EmailProperties emailProperties) {
        return new JavaMailEmailService(javaMailSender, emailProperties);
    }

    @Bean
    @Profile("!email")
    public EmailService loggingEmailService(EmailProperties emailProperties) {
        return new LoggingEmailService(emailProperties);
    }

    // TODO: migrate to TemplateRenderer interface only
    @Bean
    ThymeleafTemplateRenderer templateRenderer(TemplateEngine templateEngine) {
        return new ThymeleafTemplateRenderer(templateEngine);
    }

}
