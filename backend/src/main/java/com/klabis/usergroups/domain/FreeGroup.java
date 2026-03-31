package com.klabis.usergroups.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FreeGroup extends UserGroup {

    public static final String TYPE_DISCRIMINATOR = "FREE";

    private final Set<Invitation> invitations;

    private FreeGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members,
                      Set<Invitation> invitations) {
        super(id, name, owners, members);
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
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        GroupMembership creatorMembership = GroupMembership.of(command.creator());
        return new FreeGroup(id, command.name(), Set.of(command.creator()), Set.of(creatorMembership),
                Set.of());
    }

    public static FreeGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                        Set<GroupMembership> members, AuditMetadata auditMetadata) {
        return reconstruct(id, name, owners, members, Set.of(), auditMetadata);
    }

    public static FreeGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                        Set<GroupMembership> members, Set<Invitation> invitations,
                                        AuditMetadata auditMetadata) {
        FreeGroup group = new FreeGroup(id, name, owners, members, invitations);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    public void invite(MemberId invitedBy, MemberId target) {
        Assert.notNull(invitedBy, "invitedBy is required");
        Assert.notNull(target, "target is required");
        boolean pendingAlreadyExists = invitations.stream()
                .anyMatch(inv -> inv.isForMember(target) && inv.isPending());
        if (pendingAlreadyExists) {
            throw new DuplicatePendingInvitationException(target);
        }
        invitations.add(Invitation.createPending(invitedBy, target));
    }

    public void acceptInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        invitation.accept();
        addMember(invitation.getInvitedMember());
    }

    public void rejectInvitation(InvitationId invitationId) {
        Assert.notNull(invitationId, "invitationId is required");
        Invitation invitation = findPendingInvitation(invitationId);
        invitation.reject();
    }

    public List<Invitation> getPendingInvitations() {
        return invitations.stream()
                .filter(Invitation::isPending)
                .toList();
    }

    public Set<Invitation> getInvitations() {
        return Collections.unmodifiableSet(invitations);
    }

    public boolean isInvitedMember(InvitationId invitationId, MemberId memberId) {
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

    static final class DuplicatePendingInvitationException extends BusinessRuleViolationException {
        DuplicatePendingInvitationException(MemberId memberId) {
            super("Member %s already has a pending invitation to this group".formatted(memberId));
        }
    }

    static final class InvitationNotFoundException extends BusinessRuleViolationException {
        InvitationNotFoundException(InvitationId invitationId) {
            super("Pending invitation not found: %s".formatted(invitationId));
        }
    }
}
