package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for invitation flow on a UserGroup that implements WithInvitations.
 * Uses a simple test double (anonymous class) to avoid creating a full aggregate in common.
 */
@DisplayName("WithInvitations contract tests")
class UserGroupWithInvitationsTest {

    private static final UserId OWNER = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId USER_A = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final UserId USER_B = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    /**
     * Minimal concrete UserGroup that also implements WithInvitations, for testing the contract.
     */
    static class InvitationGroup extends UserGroup implements WithInvitations {

        private final java.util.Set<Invitation> invitations = new java.util.HashSet<>();

        InvitationGroup(String name, UserId owner) {
            super(name, Set.of(owner), Set.of(GroupMembership.of(owner)));
        }

        @Override
        public void invite(UserId invitedUser, UserId invitedBy) {
            if (hasMember(invitedUser) || isOwner(invitedUser)) {
                throw new CannotInviteExistingMemberException(invitedUser);
            }
            boolean pendingExists = invitations.stream()
                    .anyMatch(inv -> inv.isForUser(invitedUser) && inv.isPending());
            if (pendingExists) {
                throw new DuplicatePendingInvitationException(invitedUser);
            }
            invitations.add(Invitation.createPending(invitedBy, invitedUser));
        }

        @Override
        public void acceptInvitation(InvitationId invitationId) {
            Invitation invitation = findPendingInvitation(invitationId);
            invitation.accept();
            addMember(invitation.getInvitedUser());
        }

        @Override
        public void rejectInvitation(InvitationId invitationId) {
            Invitation invitation = findPendingInvitation(invitationId);
            invitation.reject();
        }

        @Override
        public List<Invitation> getPendingInvitations() {
            return invitations.stream().filter(Invitation::isPending).toList();
        }

        @Override
        public Set<Invitation> getInvitations() {
            return java.util.Collections.unmodifiableSet(invitations);
        }

        @Override
        public boolean isInvitedMember(UserId userId) {
            return invitations.stream().anyMatch(inv -> inv.isForUser(userId) && inv.isPending());
        }

        private Invitation findPendingInvitation(InvitationId invitationId) {
            return invitations.stream()
                    .filter(inv -> inv.getId().equals(invitationId) && inv.isPending())
                    .findFirst()
                    .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        }
    }

    private InvitationGroup groupWithOwner() {
        return new InvitationGroup("Test Group", OWNER);
    }

    @Nested
    @DisplayName("invite()")
    class InviteMethod {

        @Test
        @DisplayName("should create pending invitation")
        void shouldCreatePendingInvitation() {
            InvitationGroup group = groupWithOwner();

            group.invite(USER_A, OWNER);

            List<Invitation> pending = group.getPendingInvitations();
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedUser()).isEqualTo(USER_A);
            assertThat(pending.get(0).getInvitedBy()).isEqualTo(OWNER);
            assertThat(pending.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw when inviting existing member")
        void shouldThrowWhenInvitingExistingMember() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());

            assertThatThrownBy(() -> group.invite(USER_A, OWNER))
                    .isInstanceOf(CannotInviteExistingMemberException.class)
                    .hasMessageContaining(USER_A.toString());
        }

        @Test
        @DisplayName("should throw when inviting an owner")
        void shouldThrowWhenInvitingOwner() {
            InvitationGroup group = groupWithOwner();

            assertThatThrownBy(() -> group.invite(OWNER, OWNER))
                    .isInstanceOf(CannotInviteExistingMemberException.class)
                    .hasMessageContaining(OWNER.toString());
        }

        @Test
        @DisplayName("should throw when duplicate pending invitation exists")
        void shouldThrowWhenDuplicatePendingInvitation() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);

            assertThatThrownBy(() -> group.invite(USER_A, OWNER))
                    .isInstanceOf(DuplicatePendingInvitationException.class)
                    .hasMessageContaining(USER_A.toString());
        }

        @Test
        @DisplayName("should allow re-invite after rejection")
        void shouldAllowReInviteAfterRejection() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            InvitationId firstId = group.getPendingInvitations().get(0).getId();
            group.rejectInvitation(firstId);

            group.invite(USER_A, OWNER);

            assertThat(group.getPendingInvitations()).hasSize(1);
            assertThat(group.getPendingInvitations().get(0).getInvitedUser()).isEqualTo(USER_A);
        }
    }

    @Nested
    @DisplayName("acceptInvitation()")
    class AcceptInvitationMethod {

        @Test
        @DisplayName("should add user to group and mark invitation accepted")
        void shouldAcceptInvitationAndAddMember() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.acceptInvitation(invitationId);

            assertThat(group.hasMember(USER_A)).isTrue();
            assertThat(group.getPendingInvitations()).isEmpty();
            Invitation accepted = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should throw when invitation not found")
        void shouldThrowWhenInvitationNotFound() {
            InvitationGroup group = groupWithOwner();
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.acceptInvitation(unknownId))
                    .isInstanceOf(InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("rejectInvitation()")
    class RejectInvitationMethod {

        @Test
        @DisplayName("should mark invitation rejected without adding member")
        void shouldRejectInvitationWithoutAddingMember() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            InvitationId invitationId = group.getPendingInvitations().get(0).getId();

            group.rejectInvitation(invitationId);

            assertThat(group.hasMember(USER_A)).isFalse();
            assertThat(group.getPendingInvitations()).isEmpty();
            Invitation rejected = group.getInvitations().stream()
                    .filter(inv -> inv.getId().equals(invitationId))
                    .findFirst().orElseThrow();
            assertThat(rejected.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw when invitation not found")
        void shouldThrowWhenInvitationNotFound() {
            InvitationGroup group = groupWithOwner();
            InvitationId unknownId = InvitationId.newId();

            assertThatThrownBy(() -> group.rejectInvitation(unknownId))
                    .isInstanceOf(InvitationNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("isInvitedMember()")
    class IsInvitedMemberMethod {

        @Test
        @DisplayName("should return true when user has pending invitation")
        void shouldReturnTrueWhenPendingInvitation() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);

            assertThat(group.isInvitedMember(USER_A)).isTrue();
        }

        @Test
        @DisplayName("should return false when user has no pending invitation")
        void shouldReturnFalseWhenNoPendingInvitation() {
            InvitationGroup group = groupWithOwner();

            assertThat(group.isInvitedMember(USER_A)).isFalse();
        }

        @Test
        @DisplayName("should return false when invitation was accepted")
        void shouldReturnFalseWhenInvitationAccepted() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            group.acceptInvitation(group.getPendingInvitations().get(0).getId());

            assertThat(group.isInvitedMember(USER_A)).isFalse();
        }
    }

    @Nested
    @DisplayName("getPendingInvitations()")
    class GetPendingInvitationsMethod {

        @Test
        @DisplayName("should return only pending invitations")
        void shouldReturnOnlyPendingInvitations() {
            InvitationGroup group = groupWithOwner();
            group.invite(USER_A, OWNER);
            group.invite(USER_B, OWNER);
            InvitationId acceptedId = group.getPendingInvitations().stream()
                    .filter(inv -> inv.getInvitedUser().equals(USER_A))
                    .findFirst().orElseThrow().getId();
            group.acceptInvitation(acceptedId);

            List<Invitation> pending = group.getPendingInvitations();

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getInvitedUser()).isEqualTo(USER_B);
        }
    }
}
