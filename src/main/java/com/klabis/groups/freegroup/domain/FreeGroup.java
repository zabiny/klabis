package com.klabis.groups.freegroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.usergroup.CannotPromoteNonMemberToOwnerException;
import com.klabis.groups.common.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.common.domain.MemberGroup;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.FreeGroupInvitationCancelledEvent;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.*;

@AggregateRoot
public class FreeGroup extends MemberGroup<FreeGroup, FreeGroupId> implements WithInvitations {

    public static final String TYPE_DISCRIMINATOR = "FREE";

    @Identity
    private final FreeGroupId id;
    private final Set<Invitation> invitations;

    private FreeGroup(FreeGroupId id, String name, Set<MemberId> owners,
                      Set<GroupMembership> members, Set<Invitation> invitations) {
        super(name, owners, members);
        Assert.notNull(id, "FreeGroupId is required");
        Assert.notNull(invitations, "Invitations set is required");
        this.id = id;
        this.invitations = new HashSet<>(invitations);
    }

    @RecordBuilder
    public record CreateFreeGroup(String name, MemberId creator) {
        public CreateFreeGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(creator, "Creator is required");
        }
    }

    public static FreeGroup create(CreateFreeGroup command) {
        FreeGroupId id = new FreeGroupId(UUID.randomUUID());
        return new FreeGroup(id, command.name(), Set.of(command.creator()),
                Set.of(GroupMembership.of(command.creator())), Set.of());
    }

    public static FreeGroup reconstruct(FreeGroupId id, String name, Set<MemberId> owners,
                                        Set<GroupMembership> members, Set<Invitation> invitations,
                                        AuditMetadata auditMetadata) {
        FreeGroup group = new FreeGroup(id, name, owners, members, invitations);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public FreeGroupId getId() {
        return id;
    }

    public void rename(String newName, MemberId actingMember) {
        requireOwner(actingMember);
        rename(newName);
    }

    public void addOwner(MemberId memberId, MemberId actingMember) {
        Assert.notNull(memberId, "MemberId is required");
        requireOwner(actingMember);
        if (!hasMember(memberId)) {
            throw new CannotPromoteNonMemberToOwnerException(memberId.toUserId());
        }
        addOwner(memberId);
    }

    public void removeOwner(MemberId memberId, MemberId actingMember) {
        Assert.notNull(memberId, "MemberId is required");
        requireOwner(actingMember);
        removeOwner(memberId);
    }

    public void removeMember(MemberId memberId, MemberId actingMember) {
        Assert.notNull(memberId, "MemberId is required");
        requireOwner(actingMember);
        removeMember(memberId);
    }

    public void requireOwner(MemberId actingMember) {
        Assert.notNull(actingMember, "actingMember is required");
        if (!isOwner(actingMember)) {
            throw new GroupOwnershipRequiredException(actingMember, id);
        }
    }

    // Direct member addition is not allowed — members must go through the invitation flow
    @Override
    public void addMember(MemberId memberId) {
        throw new DirectMemberAdditionNotAllowedException();
    }

    @Override
    public void invite(MemberId invitedBy, MemberId target) {
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(target, "target is required");
        if (hasMember(target) || isOwner(target)) {
            throw new CannotInviteExistingMemberException(target);
        }
        boolean pendingAlreadyExists = invitations.stream()
                .anyMatch(inv -> inv.isForMember(target) && inv.isPending());
        if (pendingAlreadyExists) {
            throw new DuplicatePendingInvitationException(target);
        }
        invitations.add(Invitation.createPending(invitedBy, target));
    }

    @Override
    public void acceptInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        invitation.accept();
        super.addMember(invitation.getInvitedMember());
    }

    public void acceptInvitation(InvitationId invitationId, MemberId acceptingMember) {
        Assert.notNull(acceptingMember, "acceptingMember is required");
        if (!isInvitedMember(acceptingMember, invitationId)) {
            throw new NotInvitedMemberException(acceptingMember, invitationId);
        }
        acceptInvitation(invitationId);
    }

    @Override
    public void rejectInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        invitation.reject();
    }

    public void rejectInvitation(InvitationId invitationId, MemberId rejectingMember) {
        Assert.notNull(rejectingMember, "rejectingMember is required");
        if (!isInvitedMember(rejectingMember, invitationId)) {
            throw new NotInvitedMemberException(rejectingMember, invitationId);
        }
        rejectInvitation(invitationId);
    }

    public void cancelInvitation(InvitationId invitationId, Optional<MemberId> actor, String reason) {
        Assert.notNull(invitationId, "invitationId is required");
        actor.ifPresent(this::requireOwner);
        Invitation invitation = invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId))
                .findFirst()
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        invitation.cancel(actor, reason);

        Set<MemberId> recipientOwnerIds = new HashSet<>(getOwners());
        actor.ifPresent(recipientOwnerIds::remove);
        registerEvent(new FreeGroupInvitationCancelledEvent(
                UUID.randomUUID(),
                id,
                invitationId,
                invitation.getInvitedMember(),
                actor,
                Optional.ofNullable(reason),
                Collections.unmodifiableSet(recipientOwnerIds),
                Instant.now()));
    }

    @Override
    public List<Invitation> getPendingInvitations() {
        return invitations.stream()
                .filter(Invitation::isPending)
                .toList();
    }

    @Override
    public Set<Invitation> getInvitations() {
        return Collections.unmodifiableSet(invitations);
    }

    @Override
    public boolean isInvitedMember(MemberId memberId) {
        return invitations.stream().anyMatch(inv -> inv.isForMember(memberId));
    }

    public boolean isInvitedMember(MemberId memberId, InvitationId invitationId) {
        return invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId))
                .anyMatch(inv -> inv.isForMember(memberId));
    }

    public boolean isInvitedMember(InvitationId invitationId, MemberId memberId) {
        return isInvitedMember(memberId, invitationId);
    }

    private Invitation findPendingInvitation(InvitationId invitationId) {
        return invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId) && inv.isPending())
                .findFirst()
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
    }
}
