package com.klabis.common.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email functionality.
 * Maps to klabis.email.* properties in application.yml
 */
@ConfigurationProperties(prefix = "klabis.email")
public class EmailProperties {

    private String from;

    public static EmailProperties withFrom(String from) {
        EmailProperties props = new EmailProperties();
        props.from = from;
        return props;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
