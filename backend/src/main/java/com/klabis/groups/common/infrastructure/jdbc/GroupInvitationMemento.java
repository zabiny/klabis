package com.klabis.groups.common.infrastructure.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "groups", value = "user_group_invitations")
public class GroupInvitationMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("invited_member_id")
    private UUID invitedMemberId;

    @Column("invited_by_member_id")
    private UUID invitedByMemberId;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("cancelled_at")
    private Instant cancelledAt;

    @Column("cancelled_by")
    private UUID cancelledBy;

    @Column("cancellation_reason")
    private String cancellationReason;

    protected GroupInvitationMemento() {
    }

    public GroupInvitationMemento(UUID id, UUID invitedMemberId, UUID invitedByMemberId,
                           String status, Instant createdAt,
                           Instant cancelledAt, UUID cancelledBy, String cancellationReason) {
        this.id = id;
        this.invitedMemberId = invitedMemberId;
        this.invitedByMemberId = invitedByMemberId;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInvitedMemberId() {
        return invitedMemberId;
    }

    public UUID getInvitedByMemberId() {
        return invitedByMemberId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}
