package com.klabis.members.membersgroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.usergroup.CannotInviteExistingMemberException;
import com.klabis.common.usergroup.DirectMemberAdditionNotAllowedException;
import com.klabis.common.usergroup.DuplicatePendingInvitationException;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationNotFoundException;
import com.klabis.common.usergroup.NotInvitedMemberException;
import com.klabis.common.usergroup.UserGroup;
import com.klabis.common.usergroup.WithInvitations;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AggregateRoot
public class MembersGroup extends KlabisAggregateRoot<MembersGroup, MembersGroupId> implements WithInvitations {

    public static final String TYPE_DISCRIMINATOR = "FREE";

    @Identity
    private final MembersGroupId id;
    private final UserGroup userGroup;
    private final Set<Invitation> invitations;

    private MembersGroup(MembersGroupId id, UserGroup userGroup, Set<Invitation> invitations) {
        Assert.notNull(id, "MembersGroupId is required");
        Assert.notNull(userGroup, "UserGroup is required");
        Assert.notNull(invitations, "Invitations set is required");
        this.id = id;
        this.userGroup = userGroup;
        this.invitations = new HashSet<>(invitations);
    }

    @RecordBuilder
    public record CreateMembersGroup(String name, MemberId creator) {
        public CreateMembersGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(creator, "Creator is required");
        }
    }

    public static MembersGroup create(CreateMembersGroup command) {
        MembersGroupId id = new MembersGroupId(UUID.randomUUID());
        UserGroup userGroup = UserGroup.create(command.name(), command.creator().toUserId());
        return new MembersGroup(id, userGroup, Set.of());
    }

    public static MembersGroup reconstruct(MembersGroupId id, String name, Set<MemberId> owners,
                                           Set<GroupMembership> members, Set<Invitation> invitations,
                                           AuditMetadata auditMetadata) {
        Set<UserId> ownerIds = new HashSet<>();
        owners.forEach(m -> ownerIds.add(m.toUserId()));
        UserGroup userGroup = UserGroup.reconstruct(name, ownerIds, members);
        MembersGroup group = new MembersGroup(id, userGroup, invitations);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public MembersGroupId getId() {
        return id;
    }

    public String getName() {
        return userGroup.getName();
    }

    public Set<MemberId> getOwners() {
        return userGroup.getOwners().stream()
                .map(MemberId::fromUserId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<GroupMembership> getMembers() {
        return userGroup.getMembers();
    }

    public void rename(String newName) {
        userGroup.rename(newName);
    }

    public void addOwner(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        userGroup.addOwner(memberId.toUserId());
    }

    public void removeOwner(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        userGroup.removeOwner(memberId.toUserId());
    }

    public void removeMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        userGroup.removeMember(memberId.toUserId());
    }

    // WithInvitations interface — controllers verify authorization before calling; no owner check here
    @Override
    public void invite(UserId invitedBy, UserId target) {
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(target, "target is required");
        if (userGroup.hasMember(target) || userGroup.isOwner(target)) {
            throw new CannotInviteExistingMemberException(target);
        }
        boolean pendingAlreadyExists = invitations.stream()
                .anyMatch(inv -> inv.isForUser(target) && inv.isPending());
        if (pendingAlreadyExists) {
            throw new DuplicatePendingInvitationException(target);
        }
        invitations.add(Invitation.createPending(invitedBy, target));
    }

    public void invite(MemberId invitedBy, MemberId target) {
        invite(invitedBy.toUserId(), target.toUserId());
    }

    @Override
    public void acceptInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        invitation.accept();
        userGroup.addMember(invitation.getInvitedUser());
    }

    public void acceptInvitation(InvitationId invitationId, MemberId acceptingMember) {
        Assert.notNull(acceptingMember, "acceptingMember is required");
        if (!isInvitedMember(acceptingMember.toUserId(), invitationId)) {
            throw new NotInvitedMemberException(acceptingMember.toUserId(), invitationId);
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
        if (!isInvitedMember(rejectingMember.toUserId(), invitationId)) {
            throw new NotInvitedMemberException(rejectingMember.toUserId(), invitationId);
        }
        rejectInvitation(invitationId);
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

    // WithInvitations.isInvitedMember(UserId) — checks if user has any invitation for this group
    @Override
    public boolean isInvitedMember(UserId userId) {
        return invitations.stream().anyMatch(inv -> inv.isForUser(userId));
    }

    public boolean isInvitedMember(UserId userId, InvitationId invitationId) {
        return invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId))
                .anyMatch(inv -> inv.isForUser(userId));
    }

    public boolean isInvitedMember(InvitationId invitationId, MemberId memberId) {
        return isInvitedMember(memberId.toUserId(), invitationId);
    }

    public boolean isOwner(MemberId memberId) {
        return userGroup.isOwner(memberId.toUserId());
    }

    public boolean hasMember(MemberId memberId) {
        return userGroup.hasMember(memberId.toUserId());
    }

    public boolean isLastOwner(MemberId memberId) {
        return userGroup.isLastOwner(memberId.toUserId());
    }

    // Direct member addition is not allowed — members must go through the invitation flow
    public void addMember(MemberId memberId) {
        throw new DirectMemberAdditionNotAllowedException();
    }

    private Invitation findPendingInvitation(InvitationId invitationId) {
        return invitations.stream()
                .filter(inv -> inv.getId().equals(invitationId) && inv.isPending())
                .findFirst()
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
    }
}
