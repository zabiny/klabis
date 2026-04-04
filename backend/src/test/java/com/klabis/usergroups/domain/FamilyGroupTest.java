package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
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
        @DisplayName("should create group with correct name and parents as owners and members")
        void shouldCreateGroupWithNameAndParentsAsOwnersAndMembers() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getOwners()).containsExactly(PARENT_A);
            assertThat(group.getParents()).containsExactly(PARENT_A);
            assertThat(group.hasMember(PARENT_A)).isTrue();
        }

        @Test
        @DisplayName("should create group with multiple parents")
        void shouldCreateGroupWithMultipleParents() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A, PARENT_B), Set.of());

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getOwners()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(group.getParents()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(group.hasMember(PARENT_A)).isTrue();
            assertThat(group.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should create group with initial members")
        void shouldCreateGroupWithInitialMembers() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of(MEMBER_A, MEMBER_B));

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getMembers()).hasSize(3);
            assertThat(group.hasMember(PARENT_A)).isTrue();
            assertThat(group.hasMember(MEMBER_A)).isTrue();
            assertThat(group.hasMember(MEMBER_B)).isTrue();
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of());

            FamilyGroup group1 = FamilyGroup.create(command);
            FamilyGroup group2 = FamilyGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            Set<MemberId> parents = Set.of(PARENT_A);
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("", parents, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null parents")
        void shouldRejectNullParents() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", null, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject empty parents set")
        void shouldRejectEmptyParents() {
            Set<MemberId> emptyParents = Set.of();
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", emptyParents, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null initial members")
        void shouldRejectNullInitialMembers() {
            Set<MemberId> parents = Set.of(PARENT_A);
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", parents, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.reconstruct()")
    class ReconstructMethod {

        @Test
        @DisplayName("should reconstruct group with existing members and owners")
        void shouldReconstructWithMembersAndOwners() {
            UserGroupId id = new UserGroupId(UUID.randomUUID());
            Set<GroupMembership> memberships = Set.of(GroupMembership.of(MEMBER_A));

            FamilyGroup group = FamilyGroup.reconstruct(id, "Novákovi", Set.of(PARENT_A), memberships, null);

            assertThat(group.getId()).isEqualTo(id);
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getOwners()).containsExactly(PARENT_A);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
        }
    }

    @Nested
    @DisplayName("FamilyGroup.addParent()")
    class AddParentMethod {

        @Test
        @DisplayName("should add parent as both owner and member")
        void shouldAddParentAsOwnerAndMember() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of()));

            group.addParent(PARENT_B);

            assertThat(group.getOwners()).containsExactlyInAnyOrder(PARENT_A, PARENT_B);
            assertThat(group.hasMember(PARENT_B)).isTrue();
        }

        @Test
        @DisplayName("should throw when adding null parent")
        void shouldThrowWhenAddingNullParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of()));

            assertThatThrownBy(() -> group.addParent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when adding member already in group")
        void shouldThrowWhenAddingAlreadyPresentMember() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of(MEMBER_A)));
            assertThatThrownBy(() -> group.addParent(MEMBER_A))
                    .isInstanceOf(UserGroup.MemberAlreadyInGroupException.class);
        }
    }

    @Nested
    @DisplayName("FamilyGroup.removeParent()")
    class RemoveParentMethod {

        @Test
        @DisplayName("should remove parent from both owners and members")
        void shouldRemoveParentFromOwnersAndMembers() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A, PARENT_B), Set.of()));

            group.removeParent(PARENT_B);

            assertThat(group.getOwners()).containsExactly(PARENT_A);
            assertThat(group.hasMember(PARENT_B)).isFalse();
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing last parent")
        void shouldThrowWhenRemovingLastParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of()));

            assertThatThrownBy(() -> group.removeParent(PARENT_A))
                    .isInstanceOf(UserGroup.CannotRemoveLastOwnerException.class);
        }

        @Test
        @DisplayName("should throw when removing null parent")
        void shouldThrowWhenRemovingNullParent() {
            FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", Set.of(PARENT_A), Set.of()));

            assertThatThrownBy(() -> group.removeParent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("TYPE_DISCRIMINATOR")
    class TypeDiscriminator {

        @Test
        @DisplayName("should have FAMILY as type discriminator")
        void shouldHaveFamilyDiscriminator() {
            assertThat(FamilyGroup.TYPE_DISCRIMINATOR).isEqualTo("FAMILY");
        }
    }
}
