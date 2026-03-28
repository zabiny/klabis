package com.klabis.oris;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oris-integration")
public record OrisProperties(boolean enabled) {}
