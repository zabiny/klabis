package com.klabis.notifications.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.encryption.EncryptedString;
import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceId;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "notifications", value = "notification_device")
class NotificationDeviceMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("endpoint")
    private String endpoint;

    @Column("p256dh")
    private EncryptedString p256dh;

    @Column("auth")
    private EncryptedString auth;

    @Column("description")
    private String description;

    @Column("enabled_at")
    private Instant enabledAt;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    @Transient
    private boolean isNew = true;

    protected NotificationDeviceMemento() {
    }

    static NotificationDeviceMemento from(NotificationDevice device) {
        NotificationDeviceMemento memento = new NotificationDeviceMemento();
        memento.id = device.getId().value();
        memento.userId = device.getOwner().uuid();
        memento.endpoint = device.getEndpoint().url();
        memento.p256dh = EncryptedString.of(device.getKeys().p256dh());
        memento.auth = EncryptedString.of(device.getKeys().auth());
        memento.description = device.getDescription();
        memento.enabledAt = device.getEnabledAt();

        memento.createdAt = device.getCreatedAt();
        memento.createdBy = device.getCreatedBy();
        memento.lastModifiedAt = device.getLastModifiedAt();
        memento.lastModifiedBy = device.getLastModifiedBy();
        memento.version = device.getVersion();

        memento.isNew = (device.getAuditMetadata() == null);
        return memento;
    }

    NotificationDevice toNotificationDevice() {
        return NotificationDevice.reconstruct(
                new NotificationDeviceId(id),
                new UserId(userId),
                new PushEndpoint(endpoint),
                new PushKeys(p256dh.value(), auth.value()),
                description,
                enabledAt,
                new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version)
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
