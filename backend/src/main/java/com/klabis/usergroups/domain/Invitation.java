package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

@Entity
public class Invitation {

    @Identity
    private final InvitationId id;
    private final MemberId invitedMember;
    private final MemberId invitedBy;
    private InvitationStatus status;
    private final Instant createdAt;

    public Invitation(InvitationId id, MemberId invitedMember, MemberId invitedBy, InvitationStatus status, Instant createdAt) {
        Assert.notNull(id, "InvitationId is required");
        Assert.notNull(invitedMember, "invitedMember is required");
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(status, "status is required");
        Assert.notNull(createdAt, "createdAt is required");
        this.id = id;
        this.invitedMember = invitedMember;
        this.invitedBy = invitedBy;
        this.status = status;
        this.createdAt = createdAt;
    }

    static Invitation createPending(MemberId invitedBy, MemberId invitedMember) {
        return new Invitation(InvitationId.newId(), invitedMember, invitedBy, InvitationStatus.PENDING, Instant.now());
    }

    void accept() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Cannot accept invitation that is not in PENDING state: " + this.status);
        }
        this.status = InvitationStatus.ACCEPTED;
    }

    void reject() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Cannot reject invitation that is not in PENDING state: " + this.status);
        }
        this.status = InvitationStatus.REJECTED;
    }

    boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }

    boolean isForMember(MemberId memberId) {
        return this.invitedMember.equals(memberId);
    }

    public InvitationId getId() {
        return id;
    }

    public MemberId getInvitedMember() {
        return invitedMember;
    }

    public MemberId getInvitedBy() {
        return invitedBy;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invitation that = (Invitation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
