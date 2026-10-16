package com.klabis.notifications.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "klabis.notifications.vapid")
public record VapidProperties(
        @DefaultValue("") String publicKey,
        @DefaultValue("") String privateKey,
        @DefaultValue("mailto:admin@example.org") String subject
) {

    public boolean isConfigured() {
        return StringUtils.hasText(publicKey) && StringUtils.hasText(privateKey);
    }
}
