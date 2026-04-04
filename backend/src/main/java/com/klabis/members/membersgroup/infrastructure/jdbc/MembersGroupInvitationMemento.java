package com.klabis.members.membersgroup.infrastructure.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("members_group_invitations")
class MembersGroupInvitationMemento {

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

    protected MembersGroupInvitationMemento() {
    }

    MembersGroupInvitationMemento(UUID id, UUID invitedMemberId, UUID invitedByMemberId,
                                   String status, Instant createdAt) {
        this.id = id;
        this.invitedMemberId = invitedMemberId;
        this.invitedByMemberId = invitedByMemberId;
        this.status = status;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getInvitedMemberId() {
        return invitedMemberId;
    }

    UUID getInvitedByMemberId() {
        return invitedByMemberId;
    }

    String getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
