package com.klabis.common.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email functionality.
 * Maps to klabis.email.* properties in application.yml
 */
@ConfigurationProperties(prefix = "klabis.email")
public class EmailProperties {

    private boolean enabled = true;
    private String from;

    public static EmailProperties enabledEmail(String emailFrom) {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.enabled = true;
        emailProperties.from = emailFrom;
        return emailProperties;
    }

    public static EmailProperties disabledEmail() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.enabled = false;
        emailProperties.from = "disabled";
        return emailProperties;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
