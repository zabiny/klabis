package com.klabis.members.familygroup.domain;

import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.MemberAlreadyInGroupException;
import com.klabis.common.usergroup.MemberNotInGroupException;
import com.klabis.common.usergroup.OwnerCannotBeRemovedFromGroupException;
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

    private static final MemberId PARENT_A = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId PARENT_B = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_B = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

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
            Set<GroupMembership> memberships = Set.of(GroupMembership.of(MEMBER_A.toUserId()));

            FamilyGroup group = FamilyGroup.reconstruct(id, "Novákovi", Set.of(PARENT_A), memberships, null);

            assertThat(group.getId()).isEqualTo(id);
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }
    }

    @Nested
    @DisplayName("FamilyGroup.addParent()")
    class AddParentMethod {

        @Test
        @DisplayName("should add parent as both owner and member")
        void shouldAddParentAsOwnerAndMember() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            group.addParent(PARENT_B);

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(group.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should throw when adding null parent")
        void shouldThrowWhenAddingNullParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            assertThatThrownBy(() -> group.addParent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should grant owner privileges to existing child without throwing")
        void shouldGrantOwnerToExistingChildWithoutThrowing() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(MEMBER_A);

            group.addParent(MEMBER_A);

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, MEMBER_A);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }
    }

    @Nested
    @DisplayName("FamilyGroup.removeParent()")
    class RemoveParentMethod {

        @Test
        @DisplayName("should remove parent from both owners and members")
        void shouldRemoveParentFromOwnersAndMembers() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addParent(PARENT_B);

            group.removeParent(PARENT_B);

            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(PARENT_B)).isFalse();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last parent")
        void shouldThrowWhenRemovingLastParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            assertThatThrownBy(() -> group.removeParent(PARENT_A))
                    .isInstanceOf(CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw when removing null parent")
        void shouldThrowWhenRemovingNullParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

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
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            group.addChild(MEMBER_A);

            assertThat(group.hasMember(MEMBER_A)).isTrue();
            assertThat(group.getParents()).doesNotContain(MEMBER_A);
            assertThat(group.getMembers()).hasSize(2);
        }

        @Test
        @DisplayName("should reject adding a member who is already a parent of the same group")
        void shouldRejectChildWhoIsAlreadyParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            assertThatThrownBy(() -> group.addChild(PARENT_A))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("should reject adding a child who is already a child of the same group")
        void shouldRejectDuplicateChild() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(MEMBER_A);

            assertThatThrownBy(() -> group.addChild(MEMBER_A))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.removeChild()")
    class RemoveChildMethod {

        @Test
        @DisplayName("should remove an existing child")
        void shouldRemoveExistingChild() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(MEMBER_A);

            group.removeChild(MEMBER_A);

            assertThat(group.hasMember(MEMBER_A)).isFalse();
            assertThat(group.getMembers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw OwnerCannotBeRemovedFromGroupException when removing a parent via removeChild")
        void shouldThrowWhenRemovingParentViaRemoveChild() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            assertThatThrownBy(() -> group.removeChild(PARENT_A))
                    .isInstanceOf(OwnerCannotBeRemovedFromGroupException.class);
        }

        @Test
        @DisplayName("should throw MemberNotInGroupException when removing a non-member")
        void shouldThrowWhenRemovingNonMember() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

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
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));

            assertThatThrownBy(() -> group.addChild(PARENT_A))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("addParent on existing child promotes them to parent without duplicating membership")
        void shouldPromoteChildToParentWithoutDuplicatingMembership() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Novákovi", PARENT_A));
            group.addChild(MEMBER_A);
            int membersBefore = group.getMembers().size();

            group.addParent(MEMBER_A);

            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, MEMBER_A);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
            assertThat(group.getMembers()).hasSize(membersBefore);
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
