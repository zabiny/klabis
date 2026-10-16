package com.klabis.notifications.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record PushKeys(String p256dh, String auth) {

    public PushKeys {
        Assert.hasText(p256dh, "p256dh key is required");
        Assert.hasText(auth, "auth secret is required");
    }

    @Override
    public String toString() {
        return "PushKeys[***]";
    }
}
