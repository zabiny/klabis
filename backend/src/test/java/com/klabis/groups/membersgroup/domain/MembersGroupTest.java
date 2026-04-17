package com.klabis.groups.membersgroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.usergroup.CannotInviteExistingMemberException;
import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.DirectMemberAdditionNotAllowedException;
import com.klabis.common.usergroup.DuplicatePendingInvitationException;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationNotFoundException;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.common.usergroup.NotInvitedMemberException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MembersGroup domain unit tests")
class MembersGroupTest {

    private static final MemberId CREATOR = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER_MEMBER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId ANOTHER_MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Nested
    @DisplayName("create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with name and creator as owner and first member")
        void shouldCreateGroupWithNameAndCreator() {
            MembersGroup.CreateMembersGroup command = new MembersGroup.CreateMembersGroup("Orienteering Friends", CREATOR);

            MembersGroup group = MembersGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Orienteering Friends");
            assertThat(group.getOwners()).containsExactly(CREATOR);
            assertThat(group.getMembers()).hasSize(1);
            assertThat(group.hasMember(CREATOR)).isTrue();
            assertThat(group.isOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            MembersGroup.CreateMembersGroup command = new MembersGroup.CreateMembersGroup("Group A", CREATOR);

            MembersGroup group1 = MembersGroup.create(command);
            MembersGroup group2 = MembersGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new MembersGroup.CreateMembersGroup("", CREATOR))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null creator")
        void shouldRejectNullCreator() {
            assertThatThrownBy(() -> new MembersGroup.CreateMembersGroup("Valid Name", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("rename()")
    class RenameMethod {

        @Test
        @DisplayName("should rename group")
        void shouldRenameGroup() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Old Name", CREATOR));

            group.rename("New Name");

            assertThat(group.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank new name")
        void shouldRejectBlankName() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Old Name", CREATOR));

            assertThatThrownBy(() -> group.rename(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addMember() — direct addition blocked")
    class AddMemberMethod {

        @Test
        @DisplayName("should throw DirectMemberAdditionNotAllowedException — MembersGroup requires invitation flow")
        void shouldThrowDirectMemberAdditionNotAllowedException() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.addMember(OTHER_MEMBER))
                    .isInstanceOf(DirectMemberAdditionNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        @Test
        @DisplayName("should remove non-owner member from group")
        void shouldRemoveNonOwnerMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            group.removeMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getMembers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw when removing owner from group")
        void shouldThrowWhenRemovingOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(CREATOR))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should throw when removing member not in group")
        void shouldThrowWhenRemovingMemberNotInGroup() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(OTHER_MEMBER))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should allow removing one of multiple non-owner members")
        void shouldRemoveOneOfMultipleMembers() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            addMemberViaInvitation(group, ANOTHER_MEMBER);

            group.removeMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(ANOTHER_MEMBER)).isTrue();
            assertThat(group.getMembers()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("addOwner() / removeOwner()")
    class OwnerManagement {

        @Test
        @DisplayName("should throw CannotPromoteNonMemberToOwnerException when promoting a non-member to owner")
        void shouldThrowWhenPromotingNonMemberToOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            int memberCountBefore = group.getMembers().size();

            assertThatThrownBy(() -> group.addOwner(OTHER_MEMBER))
                    .isInstanceOf(com.klabis.common.usergroup.CannotPromoteNonMemberToOwnerException.class);
            assertThat(group.getMembers()).hasSize(memberCountBefore);
        }

        @Test
        @DisplayName("should promote existing member to owner without duplicating membership")
        void shouldPromoteExistingMemberToOwnerWithoutDuplicatingMembership() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            int memberCountBefore = group.getMembers().size();

            group.addOwner(OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isTrue();
            assertThat(group.getMembers()).hasSize(memberCountBefore);
        }

        @Test
        @DisplayName("should add owner and make them a member")
        void shouldAddOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            group.addOwner(OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should remove owner without removing them from members")
        void shouldRemoveOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER);

            group.removeOwner(OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last owner")
        void shouldThrowWhenRemovingLastOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeOwner(CREATOR))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("isLastOwner should return true for sole owner")
        void shouldReturnTrueForLastOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThat(group.isLastOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("isLastOwner should return false when multiple owners exist")
        void shouldReturnFalseWhenMultipleOwners() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER);

            assertThat(group.isLastOwner(CREATOR)).isFalse();
        }
    }

    @Nested
    @DisplayName("invite()")
    class InviteMethod {

        @Test
        @DisplayName("should create pending invitation when inviting a member")
        void shouldCreatePendingInvitationForMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedUser()).isEqualTo(OTHER_MEMBER.toUserId());
            assertThat(pending.get(0).getInvitedBy()).isEqualTo(CREATOR.toUserId());
            assertThat(pending.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw when inviting an existing member")
        void shouldThrowWhenInvitingExistingMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.invite(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(CannotInviteExistingMemberException.class)
                    .hasMessageContaining(OTHER_MEMBER.toUserId().toString());
        }

        @Test
        @DisplayName("should throw when inviting an owner")
        void shouldThrowWhenInvitingOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.invite(CREATOR, CREATOR))
                    .isInstanceOf(CannotInviteExistingMemberException.class);
        }

        @Test
        @DisplayName("should throw when inviting a member who already has a pending invitation")
        void shouldThrowWhenDuplicatePendingInvitation() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);

            assertThatThrownBy(() -> group.invite(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(DuplicatePendingInvitationException.class);
        }

        @Test
        @DisplayName("should allow re-inviting a member whose previous invitation was rejected")
        void shouldAllowReInviteAfterRejection() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId firstInvitationId = group.getPendingInvitations().get(0).getId();
            group.rejectInvitation(firstInvitationId);

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedUser()).isEqualTo(OTHER_MEMBER.toUserId());
        }
    }

    @Nested
    @DisplayName("acceptInvitation(InvitationId)")
    class AcceptInvitationById {

        @Test
        @DisplayName("should add member to group and mark invitation as accepted")
        void shouldAcceptInvitationAndAddMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.acceptInvitation(invitationId);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
            assertThat(group.getPendingInvitations()).isEmpty();
            Invitation accepted = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should throw when accepting a non-existent invitation")
        void shouldThrowWhenInvitationNotFound() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.acceptInvitation(unknownId))
                    .isInstanceOf(InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("acceptInvitation(InvitationId, MemberId) — domain invariant check")
    class AcceptInvitationWithMember {

        @Test
        @DisplayName("should add member to group when the correct invited member accepts")
        void shouldAddMemberWhenInvitedMemberAccepts() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.acceptInvitation(invitationId, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
            assertThat(group.getPendingInvitations()).isEmpty();
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when a different member tries to accept")
        void shouldThrowWhenNonInvitedMemberAccepts() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThatThrownBy(() -> group.acceptInvitation(invitationId, ANOTHER_MEMBER))
                    .isInstanceOf(NotInvitedMemberException.class);
        }
    }

    @Nested
    @DisplayName("rejectInvitation(InvitationId)")
    class RejectInvitationById {

        @Test
        @DisplayName("should mark invitation as rejected and not add member to group")
        void shouldRejectInvitationWithoutAddingMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.rejectInvitation(invitationId);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getPendingInvitations()).isEmpty();
            Invitation rejected = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(rejected.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw when rejecting a non-existent invitation")
        void shouldThrowWhenInvitationNotFound() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.rejectInvitation(unknownId))
                    .isInstanceOf(InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("rejectInvitation(InvitationId, MemberId) — domain invariant check")
    class RejectInvitationWithMember {

        @Test
        @DisplayName("should reject invitation when the correct invited member rejects")
        void shouldRejectInvitationWhenInvitedMemberRejects() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.rejectInvitation(invitationId, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getPendingInvitations()).isEmpty();
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when a different member tries to reject")
        void shouldThrowWhenNonInvitedMemberRejects() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThatThrownBy(() -> group.rejectInvitation(invitationId, ANOTHER_MEMBER))
                    .isInstanceOf(NotInvitedMemberException.class);
        }
    }

    @Nested
    @DisplayName("getPendingInvitations()")
    class GetPendingInvitationsMethod {

        @Test
        @DisplayName("should return only pending invitations, excluding accepted and rejected")
        void shouldReturnOnlyPendingInvitations() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId pendingId = group.getPendingInvitations().stream()
                    .filter(inv -> inv.getInvitedUser().equals(OTHER_MEMBER.toUserId()))
                    .findFirst().orElseThrow().getId();
            group.acceptInvitation(pendingId);

            List<Invitation> pending = group.getPendingInvitations();

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedUser()).isEqualTo(ANOTHER_MEMBER.toUserId());
        }
    }

    @Nested
    @DisplayName("isInvitedMember(InvitationId, MemberId)")
    class IsInvitedMemberMethod {

        @Test
        @DisplayName("should return true when invitation belongs to the given member")
        void shouldReturnTrueForCorrectMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(invitationId, OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false when invitation belongs to a different member")
        void shouldReturnFalseForWrongMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(invitationId, ANOTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return false for an unknown invitation ID")
        void shouldReturnFalseForUnknownInvitationId() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);

            assertThat(group.isInvitedMember(InvitationId.newId(), OTHER_MEMBER)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOwner() and hasMember()")
    class QueryMethods {

        @Test
        @DisplayName("should return true for owner")
        void shouldReturnTrueForOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThat(group.isOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return true for member added via invitation")
        void shouldReturnTrueForMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-member")
        void shouldReturnFalseForNonMember() {
            MembersGroup group = MembersGroup.create(new MembersGroup.CreateMembersGroup("Test Group", CREATOR));

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
        }
    }

    private static void addMemberViaInvitation(MembersGroup group, MemberId member) {
        group.invite(CREATOR, member);
        InvitationId invitationId = group.getPendingInvitations().stream()
                .filter(inv -> inv.getInvitedUser().equals(member.toUserId()))
                .findFirst().orElseThrow().getId();
        group.acceptInvitation(invitationId);
    }
}
