package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

@Entity
public class Invitation {

    @Identity
    private final InvitationId id;
    private final UserId invitedUser;
    private final UserId invitedBy;
    private InvitationStatus status;
    private final Instant createdAt;

    private Invitation(InvitationId id, UserId invitedUser, UserId invitedBy, InvitationStatus status, Instant createdAt) {
        Assert.notNull(id, "InvitationId is required");
        Assert.notNull(invitedUser, "invitedUser is required");
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(status, "status is required");
        Assert.notNull(createdAt, "createdAt is required");
        this.id = id;
        this.invitedUser = invitedUser;
        this.invitedBy = invitedBy;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Invitation createPending(UserId invitedBy, UserId invitedUser) {
        return new Invitation(InvitationId.newId(), invitedUser, invitedBy, InvitationStatus.PENDING, Instant.now());
    }

    public static Invitation reconstruct(InvitationId id, UserId invitedUser, UserId invitedBy,
                                         InvitationStatus status, Instant createdAt) {
        return new Invitation(id, invitedUser, invitedBy, status, createdAt);
    }

    public void accept() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Cannot accept invitation that is not in PENDING state: " + this.status);
        }
        this.status = InvitationStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Cannot reject invitation that is not in PENDING state: " + this.status);
        }
        this.status = InvitationStatus.REJECTED;
    }

    public boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }

    public boolean isForUser(UserId userId) {
        return this.invitedUser.equals(userId);
    }

    public InvitationId getId() {
        return id;
    }

    public UserId getInvitedUser() {
        return invitedUser;
    }

    public UserId getInvitedBy() {
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
