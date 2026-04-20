package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Entity
public class Invitation {

    @Identity
    private final InvitationId id;
    private final UserId invitedUser;
    private final UserId invitedBy;
    private InvitationStatus status;
    private final Instant createdAt;

    private Instant cancelledAt;
    private MemberId cancelledBy;
    private String cancellationReason;

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

    public static Invitation reconstruct(InvitationId id, UserId invitedUser, UserId invitedBy,
                                         InvitationStatus status, Instant createdAt,
                                         Instant cancelledAt, MemberId cancelledBy, String cancellationReason) {
        Invitation invitation = new Invitation(id, invitedUser, invitedBy, status, createdAt);
        invitation.cancelledAt = cancelledAt;
        invitation.cancelledBy = cancelledBy;
        invitation.cancellationReason = cancellationReason;
        return invitation;
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

    /**
     * Cancels this invitation. Only PENDING invitations can be cancelled.
     *
     * @param actor  the member performing the cancellation, or empty for SYSTEM-initiated cancel
     * @param reason optional free-text reason (max 500 chars)
     */
    public void cancel(Optional<MemberId> actor, String reason) {
        Assert.notNull(actor, "actor is required");
        if (reason != null) {
            Assert.isTrue(reason.length() <= 500, "Cancellation reason must not exceed 500 characters");
        }
        if (this.status != InvitationStatus.PENDING) {
            throw new InvitationNotCancellableException(this.id, this.status);
        }
        this.status = InvitationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor.orElse(null);
        this.cancellationReason = reason;
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

    public Optional<Instant> getCancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    public Optional<MemberId> getCancelledBy() {
        return Optional.ofNullable(cancelledBy);
    }

    public Optional<String> getCancellationReason() {
        return Optional.ofNullable(cancellationReason);
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
