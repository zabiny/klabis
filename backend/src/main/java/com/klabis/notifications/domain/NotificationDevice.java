package com.klabis.notifications.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;

@AggregateRoot
public class NotificationDevice extends KlabisAggregateRoot<NotificationDevice, NotificationDeviceId> {

    @Identity
    private final NotificationDeviceId id;
    private UserId owner;
    private final PushEndpoint endpoint;
    private PushKeys keys;
    private final String description;
    private Instant enabledAt;

    private NotificationDevice(NotificationDeviceId id, UserId owner, PushEndpoint endpoint, PushKeys keys,
                               String description, Instant enabledAt, AuditMetadata auditMetadata) {
        this.id = id;
        this.owner = owner;
        this.endpoint = endpoint;
        this.keys = keys;
        this.description = description;
        this.enabledAt = enabledAt;
        updateAuditMetadata(auditMetadata);
    }

    public static NotificationDevice registerFor(UserId owner, PushEndpoint endpoint, PushKeys keys, String description) {
        Assert.notNull(owner, "Owner is required");
        Assert.notNull(endpoint, "Endpoint is required");
        Assert.notNull(keys, "Keys are required");
        Assert.hasText(description, "Description is required");
        return new NotificationDevice(NotificationDeviceId.generate(), owner, endpoint, keys, description,
                Instant.now(), null);
    }

    public static NotificationDevice reconstruct(NotificationDeviceId id, UserId owner, PushEndpoint endpoint,
                                                 PushKeys keys, String description, Instant enabledAt,
                                                 AuditMetadata auditMetadata) {
        return new NotificationDevice(id, owner, endpoint, keys, description, enabledAt, auditMetadata);
    }

    public void reassignTo(UserId newOwner) {
        Assert.notNull(newOwner, "Owner is required");
        if (newOwner.equals(this.owner)) {
            return;
        }
        this.owner = newOwner;
        this.enabledAt = Instant.now();
    }

    public void updateKeys(PushKeys newKeys) {
        Assert.notNull(newKeys, "Keys are required");
        this.keys = newKeys;
    }

    public boolean isOwnedBy(UserId user) {
        return owner.equals(user);
    }

    @Override
    public NotificationDeviceId getId() {
        return id;
    }

    public UserId getOwner() {
        return owner;
    }

    public PushEndpoint getEndpoint() {
        return endpoint;
    }

    public PushKeys getKeys() {
        return keys;
    }

    public String getDescription() {
        return description;
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }
}
