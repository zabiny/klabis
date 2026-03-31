package com.klabis.usergroups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FreeGroup domain unit tests")
class FreeGroupTest {

    private static final MemberId CREATOR = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER_MEMBER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId ANOTHER_MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Nested
    @DisplayName("create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with name and creator as owner and first member")
        void shouldCreateGroupWithNameAndCreator() {
            FreeGroup.CreateFreeGroup command = new FreeGroup.CreateFreeGroup("Orienteering Friends", CREATOR);

            FreeGroup group = FreeGroup.create(command);

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
            FreeGroup.CreateFreeGroup command = new FreeGroup.CreateFreeGroup("Group A", CREATOR);

            FreeGroup group1 = FreeGroup.create(command);
            FreeGroup group2 = FreeGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new FreeGroup.CreateFreeGroup("", CREATOR))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null creator")
        void shouldRejectNullCreator() {
            assertThatThrownBy(() -> new FreeGroup.CreateFreeGroup("Valid Name", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMemberMethod {

        @Test
        @DisplayName("should add new member to group")
        void shouldAddNewMemberToGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            group.addMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
            assertThat(group.getMembers()).hasSize(2);
        }

        @Test
        @DisplayName("should throw when adding member already in group")
        void shouldThrowWhenMemberAlreadyInGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.addMember(OTHER_MEMBER);

            assertThatThrownBy(() -> group.addMember(OTHER_MEMBER))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(OTHER_MEMBER.toString());
        }

        @Test
        @DisplayName("should throw when adding owner who is already a member")
        void shouldThrowWhenAddingOwnerWhoIsAlreadyMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.addMember(CREATOR))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        @Test
        @DisplayName("should remove non-owner member from group")
        void shouldRemoveNonOwnerMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.addMember(OTHER_MEMBER);

            group.removeMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getMembers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw when removing owner from group")
        void shouldThrowWhenRemovingOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(CREATOR))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(CREATOR.toString());
        }

        @Test
        @DisplayName("should throw when removing member not in group")
        void shouldThrowWhenRemovingMemberNotInGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(OTHER_MEMBER))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(OTHER_MEMBER.toString());
        }

        @Test
        @DisplayName("should allow removing one of multiple non-owner members")
        void shouldRemoveOneOfMultipleMembers() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.addMember(OTHER_MEMBER);
            group.addMember(ANOTHER_MEMBER);

            group.removeMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(ANOTHER_MEMBER)).isTrue();
            assertThat(group.getMembers()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("invite()")
    class InviteMethod {

        @Test
        @DisplayName("should create pending invitation when owner invites a member")
        void shouldCreatePendingInvitationForMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedMember()).isEqualTo(OTHER_MEMBER);
            assertThat(pending.get(0).getInvitedBy()).isEqualTo(CREATOR);
            assertThat(pending.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw when inviting a member who already has a pending invitation")
        void shouldThrowWhenDuplicatePendingInvitation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);

            assertThatThrownBy(() -> group.invite(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(FreeGroup.DuplicatePendingInvitationException.class)
                    .hasMessageContaining(OTHER_MEMBER.toString());
        }

        @Test
        @DisplayName("should allow re-inviting a member whose previous invitation was rejected")
        void shouldAllowReInviteAfterRejection() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId firstInvitationId = group.getPendingInvitations().get(0).getId();
            group.rejectInvitation(firstInvitationId);

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedMember()).isEqualTo(OTHER_MEMBER);
        }
    }

    @Nested
    @DisplayName("acceptInvitation()")
    class AcceptInvitationMethod {

        @Test
        @DisplayName("should add member to group and mark invitation as accepted")
        void shouldAcceptInvitationAndAddMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.acceptInvitation(unknownId))
                    .isInstanceOf(FreeGroup.InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("rejectInvitation()")
    class RejectInvitationMethod {

        @Test
        @DisplayName("should mark invitation as rejected and not add member to group")
        void shouldRejectInvitationWithoutAddingMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.rejectInvitation(unknownId))
                    .isInstanceOf(FreeGroup.InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("getPendingInvitations()")
    class GetPendingInvitationsMethod {

        @Test
        @DisplayName("should return only pending invitations, excluding accepted and rejected")
        void shouldReturnOnlyPendingInvitations() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId pendingId = group.getPendingInvitations().stream()
                    .filter(inv -> inv.getInvitedMember().equals(OTHER_MEMBER))
                    .findFirst().orElseThrow().getId();
            group.acceptInvitation(pendingId);

            List<Invitation> pending = group.getPendingInvitations();

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedMember()).isEqualTo(ANOTHER_MEMBER);
        }
    }

    @Nested
    @DisplayName("isInvitedMember()")
    class IsInvitedMemberMethod {

        @Test
        @DisplayName("should return true when invitation belongs to the given member")
        void shouldReturnTrueForCorrectMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(invitationId, OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false when invitation belongs to a different member")
        void shouldReturnFalseForWrongMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(invitationId, ANOTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return false for an unknown invitation ID")
        void shouldReturnFalseForUnknownInvitationId() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThat(group.isOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.addMember(OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return true for member")
        void shouldReturnTrueForMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.addMember(OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-member")
        void shouldReturnFalseForNonMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
        }
    }
}
