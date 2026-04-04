package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserGroup domain unit tests")
class UserGroupTest {

    private static final UserId OWNER = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId SECOND_OWNER = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final UserId MEMBER = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final UserId ANOTHER_MEMBER = new UserId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

    private UserGroup groupWithOwner(UserId owner) {
        return UserGroup.create("Test Group", owner);
    }

    private UserGroup groupWithOwners(String name, Set<UserId> owners, Set<GroupMembership> members) {
        return UserGroup.reconstruct(name, owners, members);
    }

    @Nested
    @DisplayName("create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with name and owner")
        void shouldCreateGroupWithNameAndOwner() {
            UserGroup group = UserGroup.create("Training A", OWNER);

            assertThat(group.getName()).isEqualTo("Training A");
            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should add owner as initial member")
        void shouldAddOwnerAsInitialMember() {
            UserGroup group = UserGroup.create("Training A", OWNER);

            assertThat(group.hasMember(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> UserGroup.create("", OWNER))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null owner")
        void shouldRejectNullOwner() {
            assertThatThrownBy(() -> UserGroup.create("Valid Name", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructMethod {

        @Test
        @DisplayName("should reconstruct group with provided state")
        void shouldReconstructGroupWithProvidedState() {
            GroupMembership membership = GroupMembership.of(OWNER);
            UserGroup group = UserGroup.reconstruct("My Group", Set.of(OWNER), Set.of(membership));

            assertThat(group.getName()).isEqualTo("My Group");
            assertThat(group.getOwners()).containsExactly(OWNER);
            assertThat(group.hasMember(OWNER)).isTrue();
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMemberMethod {

        @Test
        @DisplayName("should add a new member")
        void shouldAddNewMember() {
            UserGroup group = groupWithOwner(OWNER);

            group.addMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should throw when member already in group")
        void shouldThrowWhenMemberAlreadyInGroup() {
            UserGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            assertThatThrownBy(() -> group.addMember(MEMBER))
                    .isInstanceOf(MemberAlreadyInGroupException.class)
                    .hasMessageContaining(MEMBER.toString());
        }

        @Test
        @DisplayName("should throw for null userId")
        void shouldRejectNullUserId() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.addMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        @Test
        @DisplayName("should remove a non-owner member")
        void shouldRemoveNonOwnerMember() {
            UserGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            group.removeMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should throw when member not in group")
        void shouldThrowWhenMemberNotInGroup() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(MEMBER))
                    .isInstanceOf(MemberNotInGroupException.class)
                    .hasMessageContaining(MEMBER.toString());
        }

        @Test
        @DisplayName("should throw when trying to remove an owner")
        void shouldThrowWhenRemovingOwner() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(OWNER))
                    .isInstanceOf(OwnerCannotBeRemovedFromGroupException.class)
                    .hasMessageContaining(OWNER.toString());
        }

        @Test
        @DisplayName("should throw for null userId")
        void shouldRejectNullUserId() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addOwner()")
    class AddOwnerMethod {

        @Test
        @DisplayName("should add a new owner")
        void shouldAddNewOwner() {
            UserGroup group = groupWithOwner(OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactlyInAnyOrder(OWNER, SECOND_OWNER);
        }

        @Test
        @DisplayName("should also make promoted user a member when they are not already one")
        void shouldAddNonMemberToMembersWhenPromotedToOwner() {
            UserGroup group = groupWithOwner(OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.isOwner(SECOND_OWNER)).isTrue();
            assertThat(group.hasMember(SECOND_OWNER)).isTrue();
        }

        @Test
        @DisplayName("should not duplicate membership when promoting existing member to owner")
        void shouldNotDuplicateMembershipWhenPromotingExistingMember() {
            UserGroup group = groupWithOwner(OWNER);
            group.addMember(SECOND_OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.isOwner(SECOND_OWNER)).isTrue();
            assertThat(group.hasMember(SECOND_OWNER)).isTrue();
            assertThat(group.getMembers().stream().filter(m -> m.userId().equals(SECOND_OWNER)).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should be idempotent when adding existing owner")
        void shouldBeIdempotentForExistingOwner() {
            UserGroup group = groupWithOwner(OWNER);

            group.addOwner(OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should throw for null userId")
        void shouldRejectNullUserId() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.addOwner(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("removeOwner()")
    class RemoveOwnerMethod {

        @Test
        @DisplayName("should remove owner when multiple owners exist")
        void shouldRemoveOwnerWhenMultipleExist() {
            UserGroup group = groupWithOwners("Group", Set.of(OWNER, SECOND_OWNER),
                    Set.of(GroupMembership.of(OWNER), GroupMembership.of(SECOND_OWNER)));

            group.removeOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing sole owner")
        void shouldThrowWhenRemovingLastOwner() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(OWNER))
                    .isInstanceOf(CannotRemoveLastOwnerException.class)
                    .hasMessageContaining("last owner");
        }

        @Test
        @DisplayName("should throw for null userId")
        void shouldRejectNullUserId() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("rename()")
    class RenameMethod {

        @Test
        @DisplayName("should rename the group")
        void shouldRenameGroup() {
            UserGroup group = groupWithOwner(OWNER);

            group.rename("New Name");

            assertThat(group.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank new name")
        void shouldRejectBlankName() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.rename(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.rename(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isLastOwner()")
    class IsLastOwnerMethod {

        @Test
        @DisplayName("should return true when user is sole owner")
        void shouldReturnTrueForSoleOwner() {
            UserGroup group = groupWithOwner(OWNER);

            assertThat(group.isLastOwner(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should return false when multiple owners exist")
        void shouldReturnFalseWhenMultipleOwners() {
            UserGroup group = groupWithOwners("Group", Set.of(OWNER, SECOND_OWNER),
                    Set.of(GroupMembership.of(OWNER), GroupMembership.of(SECOND_OWNER)));

            assertThat(group.isLastOwner(OWNER)).isFalse();
            assertThat(group.isLastOwner(SECOND_OWNER)).isFalse();
        }

        @Test
        @DisplayName("should return false for non-owner user")
        void shouldReturnFalseForNonOwner() {
            UserGroup group = groupWithOwner(OWNER);

            assertThat(group.isLastOwner(MEMBER)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOwner() and hasMember()")
    class QueryMethods {

        @Test
        @DisplayName("should return true for owner")
        void shouldReturnTrueForOwner() {
            UserGroup group = groupWithOwner(OWNER);

            assertThat(group.isOwner(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            UserGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            assertThat(group.isOwner(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should return true for member")
        void shouldReturnTrueForMember() {
            UserGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-member")
        void shouldReturnFalseForNonMember() {
            UserGroup group = groupWithOwner(OWNER);

            assertThat(group.hasMember(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("getMembers() should return unmodifiable set")
        void shouldReturnUnmodifiableMembers() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.getMembers().add(GroupMembership.of(MEMBER)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getOwners() should return unmodifiable set")
        void shouldReturnUnmodifiableOwners() {
            UserGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.getOwners().add(MEMBER))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
