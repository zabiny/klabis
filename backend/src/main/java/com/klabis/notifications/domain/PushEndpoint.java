package com.klabis.notifications.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.net.URI;

@ValueObject
public record PushEndpoint(String url) {

    public PushEndpoint {
        Assert.hasText(url, "Push endpoint is required");
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Push endpoint must be a valid URL", e);
        }
        Assert.isTrue("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null,
                "Push endpoint must be an https URL");
    }
}
