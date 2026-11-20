package com.klabis.groups.freegroup.domain;

import org.jspecify.annotations.Nullable;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.groups.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberGroup;
import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.*;

@AggregateRoot
public class FreeGroup extends MemberGroup<FreeGroup, FreeGroupId, MemberId> implements WithInvitations {

    public static final String TYPE_DISCRIMINATOR = "FREE";

    @Identity
    private final FreeGroupId id;
    private final Set<Invitation> invitations;
    private final Set<Authority> delegatedAuthorities;

    private FreeGroup(FreeGroupId id, String name, Set<MemberId> owners,
                      Set<GroupMembership<MemberId>> members, Set<Invitation> invitations,
                      Set<Authority> delegatedAuthorities) {
        super(name, owners, members);
        Assert.notNull(id, "FreeGroupId is required");
        Assert.notNull(invitations, "Invitations set is required");
        Assert.notNull(delegatedAuthorities, "Delegated authorities are required");
        // Guards the aggregate itself, so a client bypassing the create form cannot delegate an administrator authority.
        delegatedAuthorities.stream()
                .filter(authority -> !Authority.delegatableInFreeGroups().contains(authority))
                .findFirst()
                .ifPresent(authority -> {
                    throw new InvalidDelegatedAuthorityException(authority);
                });
        this.id = id;
        this.invitations = new HashSet<>(invitations);
        this.delegatedAuthorities = delegatedAuthorities.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(delegatedAuthorities));
    }

    @RecordBuilder
    public record CreateFreeGroup(String name, MemberId creator, Set<Authority> delegatedAuthorities) {
        public CreateFreeGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(creator, "Creator is required");
            Assert.notNull(delegatedAuthorities, "Delegated authorities are required");
            delegatedAuthorities = Set.copyOf(delegatedAuthorities);
        }
    }

    public static FreeGroup create(CreateFreeGroup command) {
        FreeGroupId id = new FreeGroupId(UUID.randomUUID());
        return new FreeGroup(id, command.name(), Set.of(command.creator()), Set.of(), Set.of(),
                command.delegatedAuthorities());
    }

    public static FreeGroup reconstruct(FreeGroupId id, String name, Set<MemberId> owners,
                                        Set<GroupMembership<MemberId>> members, Set<Invitation> invitations,
                                        Set<Authority> delegatedAuthorities, AuditMetadata auditMetadata) {
        FreeGroup group = new FreeGroup(id, name, owners, members, invitations, delegatedAuthorities);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    /**
     * Fixed at creation: members agree to what the owners may do with their data when they join, so the set
     * must not widen afterwards.
     */
    @Override
    public Set<Authority> delegatedAuthorities() {
        return Set.copyOf(delegatedAuthorities);
    }

    @Override
    public FreeGroupId getId() {
        return id;
    }

    /**
     * Only a current member may be promoted: owners are chosen among the people already in the group.
     */
    @Override
    public void addOwner(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        if (!hasMember(memberId)) {
            throw new CannotPromoteNonMemberToOwnerException(memberId, getName());
        }
        super.addOwner(memberId);
    }

    @Override
    public void removeMember(MemberId memberId) {
        super.removeMember(memberId);
    }

    // Direct member addition is not allowed — members must go through the invitation flow
    @Override
    public void addMember(MemberId memberId) {
        throw new DirectMemberAdditionNotAllowedException(getName());
    }

    @Override
    public void invite(MemberId invitedBy, MemberId target) {
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(target, "target is required");
        if (hasMember(target) || isOwner(target)) {
            throw new CannotInviteExistingMemberException(target, getName());
        }
        boolean pendingAlreadyExists = invitations.stream()
                .anyMatch(inv -> inv.isForMember(target) && inv.isPending());
        if (pendingAlreadyExists) {
            throw new DuplicatePendingInvitationException(target, getName());
        }
        invitations.add(Invitation.createPending(invitedBy, target));
    }

    @Override
    public void acceptInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        // Membership first: addMember rejects an owner, and a half-accepted invitation is not a state to persist.
        super.addMember(invitation.getInvitedMember());
        invitation.accept();
    }

    public void acceptInvitation(InvitationId invitationId, MemberId acceptingMember) {
        Assert.notNull(acceptingMember, "acceptingMember is required");
        if (!isInvitedMember(acceptingMember, invitationId)) {
            throw new NotInvitedMemberException(acceptingMember, invitationId, getName());
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
            throw new NotInvitedMemberException(rejectingMember, invitationId, getName());
        }
        rejectInvitation(invitationId);
    }

    public void cancelInvitation(InvitationId invitationId, @Nullable MemberId actor, String reason) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId))
                .findFirst()
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        invitation.cancel(actor, reason);
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

    private Invitation findPendingInvitation(InvitationId invitationId) {
        return invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId) && inv.isPending())
                .findFirst()
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
    }
}
