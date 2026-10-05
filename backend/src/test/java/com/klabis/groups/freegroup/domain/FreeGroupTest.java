package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.groups.domain.CannotRemoveLastOwnerException;
import com.klabis.common.groups.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.common.groups.domain.MemberNotInGroupException;
import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.FreeGroupInvitationCancelledEvent;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        @DisplayName("should create group with name and creator as its only owner")
        void shouldCreateGroupWithNameAndCreator() {
            FreeGroup.CreateFreeGroup command = new FreeGroup.CreateFreeGroup("Orienteering Friends", CREATOR);

            FreeGroup group = FreeGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Orienteering Friends");
            assertThat(group.getOwners()).containsExactly(CREATOR);
            assertThat(group.getMembers()).isEmpty();
            assertThat(group.hasMember(CREATOR)).isFalse();
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

        @Test
        @DisplayName("should delegate nothing when created without delegated authorities")
        void shouldDelegateNothingByDefault() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("No Delegation", CREATOR));

            assertThat(group.delegatedAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should delegate nothing when created with an empty set")
        void shouldDelegateNothingWithEmptySet() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("No Delegation", CREATOR, Set.of()));

            assertThat(group.delegatedAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should delegate MEMBERS_EDIT_PROFILE when chosen at creation")
        void shouldDelegateChosenAuthority() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup(
                    "Delegating", CREATOR, Set.of(Authority.MEMBERS_EDIT_PROFILE)));

            assertThat(group.delegatedAuthorities()).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
        }

        @Test
        @DisplayName("should reject an administrator authority that cannot be held over specific members")
        void shouldRejectAdministratorAuthority() {
            assertThatThrownBy(() -> FreeGroup.create(new FreeGroup.CreateFreeGroup(
                    "Escalating", CREATOR, Set.of(Authority.MEMBERS_MANAGE))))
                    .isInstanceOf(InvalidDelegatedAuthorityException.class)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(Authority.MEMBERS_MANAGE.getValue());
        }

        @Test
        @DisplayName("should reject an authority mixed with a delegatable one")
        void shouldRejectAdministratorAuthorityAmongDelegatable() {
            Set<Authority> chosen = EnumSet.of(Authority.MEMBERS_EDIT_PROFILE, Authority.MEMBERS_PERMISSIONS);

            assertThatThrownBy(() -> FreeGroup.create(new FreeGroup.CreateFreeGroup("Mixed", CREATOR, chosen)))
                    .isInstanceOf(InvalidDelegatedAuthorityException.class);
        }

        @Test
        @DisplayName("should reject null delegated authorities")
        void shouldRejectNullDelegatedAuthorities() {
            assertThatThrownBy(() -> new FreeGroup.CreateFreeGroup("Valid Name", CREATOR, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("delegatedAuthorities immutability")
    class DelegatedAuthoritiesImmutability {

        @Test
        @DisplayName("should expose a read-only set")
        void shouldExposeReadOnlySet() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup(
                    "Delegating", CREATOR, Set.of(Authority.MEMBERS_EDIT_PROFILE)));

            assertThatThrownBy(() -> group.delegatedAuthorities().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> group.delegatedAuthorities().add(Authority.MEMBERS_MANAGE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not change when the set passed at creation is modified afterwards")
        void shouldNotShareSetWithCaller() {
            Set<Authority> chosen = EnumSet.of(Authority.MEMBERS_EDIT_PROFILE);
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Delegating", CREATOR, chosen));

            chosen.clear();

            assertThat(group.delegatedAuthorities()).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
        }

        @Test
        @DisplayName("should be kept when other aspects of the group change")
        void shouldSurviveOtherChanges() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup(
                    "Delegating", CREATOR, Set.of(Authority.MEMBERS_EDIT_PROFILE)));

            group.rename("Renamed", CREATOR);
            group.invite(CREATOR, OTHER_MEMBER);
            group.acceptInvitation(group.getPendingInvitations().getFirst().getId());

            assertThat(group.delegatedAuthorities()).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
        }

        @Test
        @DisplayName("should be restored by reconstruct")
        void shouldBeRestoredByReconstruct() {
            FreeGroup group = FreeGroup.reconstruct(new FreeGroupId(UUID.randomUUID()), "Restored",
                    Set.of(CREATOR), Set.of(), Set.of(), Set.of(Authority.MEMBERS_EDIT_PROFILE), null);

            assertThat(group.delegatedAuthorities()).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
        }

        @Test
        @DisplayName("should reject an administrator authority when reconstructing")
        void shouldRejectAdministratorAuthorityOnReconstruct() {
            assertThatThrownBy(() -> FreeGroup.reconstruct(new FreeGroupId(UUID.randomUUID()), "Restored",
                    Set.of(CREATOR), Set.of(), Set.of(), Set.of(Authority.MEMBERS_MANAGE), null))
                    .isInstanceOf(InvalidDelegatedAuthorityException.class);
        }
    }

    @Nested
    @DisplayName("rename()")
    class RenameMethod {

        @Test
        @DisplayName("should rename group")
        void shouldRenameGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Old Name", CREATOR));

            group.rename("New Name", CREATOR);

            assertThat(group.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank new name")
        void shouldRejectBlankName() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Old Name", CREATOR));

            assertThatThrownBy(() -> group.rename("", CREATOR))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when non-owner renames")
        void shouldThrowWhenNonOwnerRenames() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Old Name", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.rename("New Name", OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }
    }

    @Nested
    @DisplayName("addMember() — direct addition blocked")
    class AddMemberMethod {

        @Test
        @DisplayName("should throw DirectMemberAdditionNotAllowedException — FreeGroup requires invitation flow")
        void shouldThrowDirectMemberAdditionNotAllowedException() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            group.removeMember(OTHER_MEMBER, CREATOR);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should throw when removing owner from group")
        void shouldThrowWhenRemovingOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(CREATOR, CREATOR))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should throw when removing member not in group")
        void shouldThrowWhenRemovingMemberNotInGroup() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeMember(OTHER_MEMBER, CREATOR))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when non-owner removes member")
        void shouldThrowWhenNonOwnerRemovesMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            addMemberViaInvitation(group, ANOTHER_MEMBER);

            assertThatThrownBy(() -> group.removeMember(ANOTHER_MEMBER, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should let a member remove themselves to leave the group")
        void shouldLetMemberLeaveThemselves() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            addMemberViaInvitation(group, ANOTHER_MEMBER);

            group.removeMember(OTHER_MEMBER, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(ANOTHER_MEMBER)).isTrue();
            assertThat(group.getOwners()).containsExactly(CREATOR);
        }

        @Test
        @DisplayName("should throw MemberNotInGroupException when the caller was never a member")
        void shouldRejectLeavingWhenNotAMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.removeMember(ANOTHER_MEMBER, ANOTHER_MEMBER))
                    .isInstanceOf(MemberNotInGroupException.class)
                    .hasMessageContaining(ANOTHER_MEMBER.toString());
        }

        @Test
        @DisplayName("should not let an owner use the leave path — owners leave via removeOwner")
        void shouldNotLetOwnerUseLeavePath() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.removeMember(CREATOR, CREATOR))
                    .isInstanceOf(MemberNotInGroupException.class);
            assertThat(group.isOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("should allow re-inviting a member who left the group")
        void shouldAllowReInviteAfterLeaving() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.removeMember(OTHER_MEMBER, OTHER_MEMBER);

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedMember()).isEqualTo(OTHER_MEMBER);
            assertThat(pending.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should allow removing one of multiple non-owner members")
        void shouldRemoveOneOfMultipleMembers() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            addMemberViaInvitation(group, ANOTHER_MEMBER);

            group.removeMember(OTHER_MEMBER, CREATOR);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(ANOTHER_MEMBER)).isTrue();
            assertThat(group.getMembers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("addOwner() / removeOwner()")
    class OwnerManagement {

        @Test
        @DisplayName("should throw CannotPromoteNonMemberToOwnerException when promoting a non-member to owner")
        void shouldThrowWhenPromotingNonMemberToOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            int memberCountBefore = group.getMembers().size();

            assertThatThrownBy(() -> group.addOwner(OTHER_MEMBER, CREATOR))
                    .isInstanceOf(com.klabis.groups.freegroup.domain.CannotPromoteNonMemberToOwnerException.class);
            assertThat(group.getMembers()).hasSize(memberCountBefore);
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when non-owner promotes a member")
        void shouldThrowWhenNonOwnerPromotesMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            addMemberViaInvitation(group, ANOTHER_MEMBER);

            assertThatThrownBy(() -> group.addOwner(ANOTHER_MEMBER, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should move the promoted member out of the member list")
        void shouldMovePromotedMemberOutOfMembers() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            group.addOwner(OTHER_MEMBER, CREATOR);

            assertThat(group.isOwner(OTHER_MEMBER)).isTrue();
            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should remove owner from the group entirely")
        void shouldRemoveOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER, CREATOR);

            group.removeOwner(OTHER_MEMBER, CREATOR);

            assertThat(group.isOwner(OTHER_MEMBER)).isFalse();
            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when non-owner removes an owner")
        void shouldThrowWhenNonOwnerRemovesOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.removeOwner(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last owner")
        void shouldThrowWhenRemovingLastOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.removeOwner(CREATOR, CREATOR))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("isLastOwner should return true for sole owner")
        void shouldReturnTrueForLastOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThat(group.isLastOwner(CREATOR)).isTrue();
        }

        @Test
        @DisplayName("isLastOwner should return false when multiple owners exist")
        void shouldReturnFalseWhenMultipleOwners() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER, CREATOR);

            assertThat(group.isLastOwner(CREATOR)).isFalse();
        }
    }

    @Nested
    @DisplayName("invite()")
    class InviteMethod {

        @Test
        @DisplayName("should create pending invitation when inviting a member")
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
        @DisplayName("should throw when inviting an existing member")
        void shouldThrowWhenInvitingExistingMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThatThrownBy(() -> group.invite(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(CannotInviteExistingMemberException.class)
                    .hasMessageContaining(OTHER_MEMBER.toString());
        }

        @Test
        @DisplayName("should throw when inviting an owner")
        void shouldThrowWhenInvitingOwner() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThatThrownBy(() -> group.invite(CREATOR, CREATOR))
                    .isInstanceOf(CannotInviteExistingMemberException.class);
        }

        @Test
        @DisplayName("should throw when inviting a member who already has a pending invitation")
        void shouldThrowWhenDuplicatePendingInvitation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);

            assertThatThrownBy(() -> group.invite(CREATOR, OTHER_MEMBER))
                    .isInstanceOf(DuplicatePendingInvitationException.class);
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
    @DisplayName("acceptInvitation(InvitationId)")
    class AcceptInvitationById {

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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.acceptInvitation(invitationId, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
            assertThat(group.getPendingInvitations()).isEmpty();
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when a different member tries to accept")
        void shouldThrowWhenNonInvitedMemberAccepts() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
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
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.rejectInvitation(invitationId, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
            assertThat(group.getPendingInvitations()).isEmpty();
        }

        @Test
        @DisplayName("should throw NotInvitedMemberException when a different member tries to reject")
        void shouldThrowWhenNonInvitedMemberRejects() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
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
    @DisplayName("isInvitedMember(MemberId, InvitationId)")
    class IsInvitedMemberMethod {

        @Test
        @DisplayName("should return true when invitation belongs to the given member")
        void shouldReturnTrueForCorrectMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(OTHER_MEMBER, invitationId)).isTrue();
        }

        @Test
        @DisplayName("should return false when invitation belongs to a different member")
        void shouldReturnFalseForWrongMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThat(group.isInvitedMember(ANOTHER_MEMBER, invitationId)).isFalse();
        }

        @Test
        @DisplayName("should return false for an unknown invitation ID")
        void shouldReturnFalseForUnknownInvitationId() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);

            assertThat(group.isInvitedMember(OTHER_MEMBER, InvitationId.newId())).isFalse();
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
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThat(group.isOwner(OTHER_MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return true for member added via invitation")
        void shouldReturnTrueForMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);

            assertThat(group.hasMember(OTHER_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-member")
        void shouldReturnFalseForNonMember() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));

            assertThat(group.hasMember(OTHER_MEMBER)).isFalse();
        }
    }

    @Nested
    @DisplayName("cancelInvitation()")
    class CancelInvitationMethod {

        @Test
        @DisplayName("should cancel pending invitation with reason and populate audit fields")
        void shouldCancelPendingInvitationWithReason() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.cancelInvitation(invitationId, Optional.of(CREATOR), "Changed plans");

            Invitation invitation = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            assertThat(invitation.getCancelledAt()).isPresent();
            assertThat(invitation.getCancelledBy()).contains(CREATOR);
            assertThat(invitation.getCancellationReason()).contains("Changed plans");
            assertThat(group.getPendingInvitations()).isEmpty();
        }

        @Test
        @DisplayName("should cancel pending invitation without reason — reason stays null")
        void shouldCancelPendingInvitationWithoutReason() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.cancelInvitation(invitationId, Optional.of(CREATOR), null);

            Invitation invitation = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            assertThat(invitation.getCancelledAt()).isPresent();
            assertThat(invitation.getCancelledBy()).contains(CREATOR);
            assertThat(invitation.getCancellationReason()).isEmpty();
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when former owner attempts to cancel")
        void shouldThrowWhenFormerOwnerAttemptsToCancel() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER, CREATOR);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.removeOwner(OTHER_MEMBER, CREATOR);

            assertThatThrownBy(() -> group.cancelInvitation(invitationId, Optional.of(OTHER_MEMBER), null))
                    .isInstanceOf(GroupOwnershipRequiredException.class);

            Invitation invitation = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw GroupOwnershipRequiredException when non-owner member attempts to cancel")
        void shouldThrowWhenNonOwnerMemberAttemptsToCancel() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            assertThatThrownBy(() -> group.cancelInvitation(invitationId, Optional.of(OTHER_MEMBER), null))
                    .isInstanceOf(GroupOwnershipRequiredException.class);
        }

        @Test
        @DisplayName("should throw InvitationNotCancellableException when cancelling an ACCEPTED invitation")
        void shouldThrowWhenCancellingAcceptedInvitation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.acceptInvitation(invitationId);

            assertThatThrownBy(() -> group.cancelInvitation(invitationId, Optional.of(CREATOR), null))
                    .isInstanceOf(InvitationNotCancellableException.class);
        }

        @Test
        @DisplayName("should throw InvitationNotCancellableException when cancelling a REJECTED invitation")
        void shouldThrowWhenCancellingRejectedInvitation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.rejectInvitation(invitationId);

            assertThatThrownBy(() -> group.cancelInvitation(invitationId, Optional.of(CREATOR), null))
                    .isInstanceOf(InvitationNotCancellableException.class);
        }

        @Test
        @DisplayName("should throw InvitationNotCancellableException when cancelling an already CANCELLED invitation")
        void shouldThrowWhenCancellingAlreadyCancelledInvitation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();
            group.cancelInvitation(invitationId, Optional.of(CREATOR), null);

            assertThatThrownBy(() -> group.cancelInvitation(invitationId, Optional.of(CREATOR), null))
                    .isInstanceOf(InvitationNotCancellableException.class);
        }

        @Test
        @DisplayName("should allow re-inviting a member after their previous invitation was cancelled")
        void shouldAllowReInviteAfterCancellation() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            group.invite(CREATOR, OTHER_MEMBER);
            InvitationId firstId = group.getPendingInvitations().get(0).getId();
            group.cancelInvitation(firstId, Optional.of(CREATOR), null);

            group.invite(CREATOR, OTHER_MEMBER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedMember()).isEqualTo(OTHER_MEMBER);
        }
    }

    @Nested
    @DisplayName("cancelInvitation() — domain event")
    class CancelInvitationDomainEvent {

        @Test
        @DisplayName("should emit FreeGroupInvitationCancelledEvent with recipientOwnerIds excluding the actor")
        void shouldEmitEventExcludingActor() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER, CREATOR);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.cancelInvitation(invitationId, Optional.of(CREATOR), "No longer needed");

            List<Object> events = group.getDomainEvents();
            assertThat(events).hasSize(1);
            FreeGroupInvitationCancelledEvent event = (FreeGroupInvitationCancelledEvent) events.get(0);
            assertThat(event.groupId()).isEqualTo(group.getId());
            assertThat(event.invitationId()).isEqualTo(invitationId);
            assertThat(event.inviteeMemberId()).isEqualTo(ANOTHER_MEMBER);
            assertThat(event.actor()).contains(CREATOR);
            assertThat(event.reason()).contains("No longer needed");
            assertThat(event.recipientOwnerIds()).containsExactly(OTHER_MEMBER);
            assertThat(event.cancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("should emit FreeGroupInvitationCancelledEvent with all owners as recipients for SYSTEM actor")
        void shouldEmitEventWithAllOwnersForSystemActor() {
            FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Test Group", CREATOR));
            addMemberViaInvitation(group, OTHER_MEMBER);
            group.addOwner(OTHER_MEMBER, CREATOR);
            group.invite(CREATOR, ANOTHER_MEMBER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.cancelInvitation(invitationId, Optional.empty(), "Member deactivated");

            List<Object> events = group.getDomainEvents();
            assertThat(events).hasSize(1);
            FreeGroupInvitationCancelledEvent event = (FreeGroupInvitationCancelledEvent) events.get(0);
            assertThat(event.actor()).isEmpty();
            assertThat(event.recipientOwnerIds()).containsExactlyInAnyOrder(CREATOR, OTHER_MEMBER);
        }
    }

    private static void addMemberViaInvitation(FreeGroup group, MemberId member) {
        group.invite(CREATOR, member);
        InvitationId invitationId = group.getPendingInvitations().stream()
                .filter(inv -> inv.getInvitedMember().equals(member))
                .findFirst().orElseThrow().getId();
        group.acceptInvitation(invitationId);
    }
}
