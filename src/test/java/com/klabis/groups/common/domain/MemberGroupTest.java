package com.klabis.groups.common.domain;

import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberGroup domain unit tests")
class MemberGroupTest {

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId SECOND_OWNER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    /**
     * Minimal concrete subclass used only for testing the abstract MemberGroup.
     */
    static class TestGroup extends MemberGroup<TestGroup, MemberId> {

        private final MemberId id;

        private TestGroup(MemberId id, String name, Set<MemberId> owners, Set<GroupMembership> members) {
            super(name, owners, members);
            this.id = id;
        }

        static TestGroup create(String name, MemberId owner) {
            return new TestGroup(
                    new MemberId(UUID.randomUUID()),
                    name,
                    Set.of(owner),
                    Set.of(GroupMembership.of(owner))
            );
        }

        static TestGroup reconstruct(String name, Set<MemberId> owners, Set<GroupMembership> members) {
            return new TestGroup(new MemberId(UUID.randomUUID()), name, owners, members);
        }

        @Override
        public MemberId getId() {
            return id;
        }
    }

    private TestGroup groupWithOwner(MemberId owner) {
        return TestGroup.create("Test Group", owner);
    }

    private TestGroup groupWithOwners(String name, Set<MemberId> owners, Set<GroupMembership> members) {
        return TestGroup.reconstruct(name, owners, members);
    }

    @Nested
    @DisplayName("create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with name and owner as member")
        void shouldCreateGroupWithNameAndOwnerAsMember() {
            TestGroup group = TestGroup.create("Training A", OWNER);

            assertThat(group.getName()).isEqualTo("Training A");
            assertThat(group.getOwners()).containsExactly(OWNER);
            assertThat(group.hasMember(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> TestGroup.create("", OWNER))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> TestGroup.create(null, OWNER))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("rename()")
    class RenameMethod {

        @Test
        @DisplayName("should rename the group")
        void shouldRenameGroup() {
            TestGroup group = groupWithOwner(OWNER);

            group.rename("New Name");

            assertThat(group.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.rename(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.rename(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addOwner()")
    class AddOwnerMethod {

        @Test
        @DisplayName("should add a new owner")
        void shouldAddNewOwner() {
            TestGroup group = groupWithOwner(OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactlyInAnyOrder(OWNER, SECOND_OWNER);
        }

        @Test
        @DisplayName("should be idempotent when adding existing owner")
        void shouldBeIdempotentForExistingOwner() {
            TestGroup group = groupWithOwner(OWNER);

            group.addOwner(OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should not affect membership — addOwner does not auto-add as member")
        void shouldNotAutoAddOwnerAsMember() {
            TestGroup group = groupWithOwner(OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.isOwner(SECOND_OWNER)).isTrue();
            assertThat(group.hasMember(SECOND_OWNER)).isFalse();
        }

        @Test
        @DisplayName("should throw for null memberId")
        void shouldRejectNullMemberId() {
            TestGroup group = groupWithOwner(OWNER);

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
            TestGroup group = groupWithOwners("Group",
                    Set.of(OWNER, SECOND_OWNER),
                    Set.of(GroupMembership.of(OWNER), GroupMembership.of(SECOND_OWNER)));

            group.removeOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing sole owner")
        void shouldThrowWhenRemovingLastOwner() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(OWNER))
                    .isInstanceOf(CannotRemoveLastOwnerException.class)
                    .hasMessageContaining("last owner");
        }

        @Test
        @DisplayName("should throw for null memberId")
        void shouldRejectNullMemberId() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isOwner() and isLastOwner()")
    class OwnerQueryMethods {

        @Test
        @DisplayName("should return true for owner")
        void shouldReturnTrueForOwner() {
            TestGroup group = groupWithOwner(OWNER);

            assertThat(group.isOwner(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            TestGroup group = groupWithOwner(OWNER);

            assertThat(group.isOwner(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("isLastOwner() should return true when user is sole owner")
        void shouldReturnTrueForSoleOwner() {
            TestGroup group = groupWithOwner(OWNER);

            assertThat(group.isLastOwner(OWNER)).isTrue();
        }

        @Test
        @DisplayName("isLastOwner() should return false when multiple owners exist")
        void shouldReturnFalseWhenMultipleOwners() {
            TestGroup group = groupWithOwners("Group",
                    Set.of(OWNER, SECOND_OWNER),
                    Set.of(GroupMembership.of(OWNER), GroupMembership.of(SECOND_OWNER)));

            assertThat(group.isLastOwner(OWNER)).isFalse();
            assertThat(group.isLastOwner(SECOND_OWNER)).isFalse();
        }

        @Test
        @DisplayName("isLastOwner() should return false for non-owner member")
        void shouldReturnFalseForNonOwnerMember() {
            TestGroup group = groupWithOwner(OWNER);

            assertThat(group.isLastOwner(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("getOwners() should return unmodifiable set")
        void shouldReturnUnmodifiableOwners() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.getOwners().add(MEMBER))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMemberMethod {

        @Test
        @DisplayName("should add a new member")
        void shouldAddNewMember() {
            TestGroup group = groupWithOwner(OWNER);

            group.addMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should throw MemberAlreadyInGroupException when member already in group")
        void shouldThrowWhenMemberAlreadyInGroup() {
            TestGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            assertThatThrownBy(() -> group.addMember(MEMBER))
                    .isInstanceOf(MemberAlreadyInGroupException.class)
                    .hasMessageContaining(MEMBER.toString());
        }

        @Test
        @DisplayName("should throw for null memberId")
        void shouldRejectNullMemberId() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.addMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("added member should have joinedAt timestamp")
        void shouldSetJoinedAtTimestamp() {
            TestGroup group = groupWithOwner(OWNER);

            group.addMember(MEMBER);

            GroupMembership membership = group.getMembers().stream()
                    .filter(m -> m.memberId().equals(MEMBER))
                    .findFirst()
                    .orElseThrow();
            assertThat(membership.joinedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberMethod {

        @Test
        @DisplayName("should remove a non-owner member")
        void shouldRemoveNonOwnerMember() {
            TestGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            group.removeMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("should throw MemberNotInGroupException when member not in group")
        void shouldThrowWhenMemberNotInGroup() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(MEMBER))
                    .isInstanceOf(MemberNotInGroupException.class)
                    .hasMessageContaining(MEMBER.toString());
        }

        @Test
        @DisplayName("should throw OwnerCannotBeRemovedFromGroupException when removing owner")
        void shouldThrowWhenRemovingOwner() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(OWNER))
                    .isInstanceOf(OwnerCannotBeRemovedFromGroupException.class)
                    .hasMessageContaining(OWNER.toString());
        }

        @Test
        @DisplayName("should throw for null memberId")
        void shouldRejectNullMemberId() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeMember(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("hasMember() and getMembers()")
    class MemberQueryMethods {

        @Test
        @DisplayName("should return true for existing member")
        void shouldReturnTrueForMember() {
            TestGroup group = groupWithOwner(OWNER);
            group.addMember(MEMBER);

            assertThat(group.hasMember(MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-member")
        void shouldReturnFalseForNonMember() {
            TestGroup group = groupWithOwner(OWNER);

            assertThat(group.hasMember(MEMBER)).isFalse();
        }

        @Test
        @DisplayName("getMembers() should return unmodifiable set")
        void shouldReturnUnmodifiableMembers() {
            TestGroup group = groupWithOwner(OWNER);

            assertThatThrownBy(() -> group.getMembers().add(GroupMembership.of(MEMBER)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
