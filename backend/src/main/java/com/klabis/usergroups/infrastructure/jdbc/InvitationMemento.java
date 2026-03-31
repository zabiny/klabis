package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.usergroups.domain.Invitation;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.InvitationStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("invitations")
class InvitationMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("invited_member_id")
    private UUID invitedMemberId;

    @Column("invited_by_member_id")
    private UUID invitedByMemberId;

    @Column("status")
    private InvitationStatus status;

    @Column("created_at")
    private Instant createdAt;

    protected InvitationMemento() {
    }

    static InvitationMemento from(Invitation invitation) {
        InvitationMemento memento = new InvitationMemento();
        memento.id = invitation.getId().value();
        memento.invitedMemberId = invitation.getInvitedMember().uuid();
        memento.invitedByMemberId = invitation.getInvitedBy().uuid();
        memento.status = invitation.getStatus();
        memento.createdAt = invitation.getCreatedAt();
        return memento;
    }

    Invitation toInvitation() {
        return new Invitation(
                new InvitationId(this.id),
                new MemberId(this.invitedMemberId),
                new MemberId(this.invitedByMemberId),
                this.status,
                this.createdAt
        );
    }

    UUID getId() {
        return id;
    }

    UUID getInvitedMemberId() {
        return invitedMemberId;
    }
}
