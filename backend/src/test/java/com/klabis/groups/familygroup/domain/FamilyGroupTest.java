package com.klabis.groups.familygroup.domain;

import com.klabis.common.users.UserId;
import com.klabis.groups.common.domain.CannotRemoveLastOwnerException;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.common.domain.MemberAlreadyInGroupException;
import com.klabis.groups.common.domain.MemberNotInGroupException;
import com.klabis.groups.common.domain.OwnerCannotBeRemovedFromGroupException;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FamilyGroup domain unit tests")
class FamilyGroupTest {

    private static final UserId PARENT_A = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId PARENT_B = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_B = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    private static FamilyGroup groupWith(UserId parent) {
        return FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", parent));
    }

    @Nested
    @DisplayName("FamilyGroup.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with scalar parent as owner and member")
        void shouldCreateGroupWithScalarParentAsOwnerAndMember() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A);

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(PARENT_A)).isTrue();
            assertThat(group.getMembers()).hasSize(1);
        }

        @Test
        @DisplayName("should accept a parent without any member profile")
        void shouldAcceptParentWithoutMemberProfile() {
            UserId nonMemberParent = new UserId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

            FamilyGroup group = FamilyGroup.create(
                    new FamilyGroup.CreateFamilyGroup("Novákovi", nonMemberParent));

            assertThat(group.getParents()).containsExactly(nonMemberParent);
            assertThat(group.hasMember(nonMemberParent)).isTrue();
        }

        @Test
        @DisplayName("should reject null parent")
        void shouldRejectNullParent() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A);

            FamilyGroup group1 = FamilyGroup.create(command);
            FamilyGroup group2 = FamilyGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("", PARENT_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.reconstruct()")
    class ReconstructMethod {

        @Test
        @DisplayName("should reconstruct group with existing members and owners")
        void shouldReconstructWithMembersAndOwners() {
            FamilyGroupId id = new FamilyGroupId(UUID.randomUUID());
            Set<GroupMembership<UserId>> memberships = Set.of(GroupMembership.of(MEMBER_A.toUserId()));

            FamilyGroup group = FamilyGroup.reconstruct(id, "Novákovi", Set.of(PARENT_A), memberships, null);

            assertThat(group.getId()).isEqualTo(id);
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(MEMBER_A.toUserId())).isTrue();
        }
    }

    @Nested
    @DisplayName("FamilyGroup.addParent()")
    class AddParentMethod {

        @Test
        @DisplayName("should add parent as both owner and member")
        void shouldAddParentAsOwnerAndMember() {
            FamilyGroup group = groupWith(PARENT_A);

            group.addParent(PARENT_B);

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(group.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should throw when adding null parent")
        void shouldThrowWhenAddingNullParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.addParent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should grant owner privileges to existing child without throwing")
        void shouldGrantOwnerToExistingChildWithoutThrowing() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addChild(MEMBER_A);

            group.addParent(MEMBER_A.toUserId());

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, MEMBER_A.toUserId());
            assertThat(group.hasMember(MEMBER_A.toUserId())).isTrue();
        }
    }

    @Nested
    @DisplayName("FamilyGroup.removeParent()")
    class RemoveParentMethod {

        @Test
        @DisplayName("should remove parent from both owners and members")
        void shouldRemoveParentFromOwnersAndMembers() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addParent(PARENT_B);

            group.removeParent(PARENT_B);

            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(PARENT_B)).isFalse();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last parent")
        void shouldThrowWhenRemovingLastParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.removeParent(PARENT_A))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw when removing null parent")
        void shouldThrowWhenRemovingNullParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.removeParent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.addChild()")
    class AddChildMethod {

        @Test
        @DisplayName("should add child as non-owner member")
        void shouldAddChildAsNonOwnerMember() {
            FamilyGroup group = groupWith(PARENT_A);

            group.addChild(MEMBER_A);

            assertThat(group.hasMember(MEMBER_A.toUserId())).isTrue();
            assertThat(group.getParents()).doesNotContain(MEMBER_A.toUserId());
            assertThat(group.getMembers()).hasSize(2);
        }

        @Test
        @DisplayName("should reject adding a member who is already a parent of the same group")
        void shouldRejectChildWhoIsAlreadyParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.addChild(MemberId.fromUserId(PARENT_A)))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("should reject adding a child who is already a child of the same group")
        void shouldRejectDuplicateChild() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addChild(MEMBER_A);

            assertThatThrownBy(() -> group.addChild(MEMBER_A))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("should throw when adding null child")
        void shouldThrowWhenAddingNullChild() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.addChild(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.removeChild()")
    class RemoveChildMethod {

        @Test
        @DisplayName("should remove an existing child")
        void shouldRemoveExistingChild() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addChild(MEMBER_A);

            group.removeChild(MEMBER_A);

            assertThat(group.hasMember(MEMBER_A.toUserId())).isFalse();
            assertThat(group.getMembers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw OwnerCannotBeRemovedFromGroupException when removing a parent via removeChild")
        void shouldThrowWhenRemovingParentViaRemoveChild() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.removeChild(MemberId.fromUserId(PARENT_A)))
                    .isInstanceOf(OwnerCannotBeRemovedFromGroupException.class);
        }

        @Test
        @DisplayName("should throw MemberNotInGroupException when removing a non-member")
        void shouldThrowWhenRemovingNonMember() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.removeChild(MEMBER_A))
                    .isInstanceOf(MemberNotInGroupException.class);
        }
    }

    @Nested
    @DisplayName("Parent/child exclusivity invariant")
    class ParentChildExclusivityInvariant {

        @Test
        @DisplayName("addChild rejects the parent of the same group — cannot be both parent and child")
        void shouldRejectAddingParentAsChild() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThatThrownBy(() -> group.addChild(MemberId.fromUserId(PARENT_A)))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("addParent on existing child promotes them to parent without duplicating membership")
        void shouldPromoteChildToParentWithoutDuplicatingMembership() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addChild(MEMBER_A);
            int membersBefore = group.getMembers().size();

            group.addParent(MEMBER_A.toUserId());

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, MEMBER_A.toUserId());
            assertThat(group.hasMember(MEMBER_A.toUserId())).isTrue();
            assertThat(group.getMembers()).hasSize(membersBefore);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.getChildren()")
    class GetChildrenMethod {

        @Test
        @DisplayName("should return empty set when group has only parents and no children")
        void shouldReturnEmptyWhenOnlyParents() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThat(group.getChildren()).isEmpty();
        }

        @Test
        @DisplayName("should return children (non-parent members) as MemberId")
        void shouldReturnNonParentMembers() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addChild(MEMBER_A);
            group.addChild(MEMBER_B);

            assertThat(group.getChildren())
                    .extracting(GroupMembership::memberId)
                    .containsExactlyInAnyOrder(MEMBER_A, MEMBER_B);
        }

        @Test
        @DisplayName("should exclude parents from children result")
        void shouldExcludeParents() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addParent(PARENT_B);
            group.addChild(MEMBER_A);

            assertThat(group.getChildren())
                    .extracting(GroupMembership::memberId)
                    .containsExactly(MEMBER_A)
                    .doesNotContain(MemberId.fromUserId(PARENT_A), MemberId.fromUserId(PARENT_B));
        }
    }

    @Nested
    @DisplayName("FamilyGroup.isLastParent()")
    class IsLastParentMethod {

        @Test
        @DisplayName("should return true when user is the only parent")
        void shouldReturnTrueWhenSoleParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThat(group.isLastParent(PARENT_A)).isTrue();
        }

        @Test
        @DisplayName("should return false when there are multiple parents")
        void shouldReturnFalseWhenMultipleParents() {
            FamilyGroup group = groupWith(PARENT_A);
            group.addParent(PARENT_B);

            assertThat(group.isLastParent(PARENT_A)).isFalse();
        }

        @Test
        @DisplayName("should return false when user is not a parent")
        void shouldReturnFalseWhenNotAParent() {
            FamilyGroup group = groupWith(PARENT_A);

            assertThat(group.isLastParent(MEMBER_A.toUserId())).isFalse();
        }
    }

    @Nested
    @DisplayName("TYPE_DISCRIMINATOR")
    class TypeDiscriminatorTest {

        @Test
        @DisplayName("should have FAMILY as type discriminator")
        void shouldHaveFamilyDiscriminator() {
            assertThat(FamilyGroup.TYPE_DISCRIMINATOR).isEqualTo("FAMILY");
        }
    }
}
