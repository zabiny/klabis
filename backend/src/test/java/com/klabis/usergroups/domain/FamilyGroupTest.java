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

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId MEMBER_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_B = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("FamilyGroup.create()")
    class CreateMethod {

        @Test
        @DisplayName("should create group with correct name and owner")
        void shouldCreateGroupWithNameAndOwner() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup("Novákovi", OWNER, Set.of());

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getOwners()).containsExactly(OWNER);
            assertThat(group.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("should create group with initial members")
        void shouldCreateGroupWithInitialMembers() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                    "Novákovi", OWNER, Set.of(MEMBER_A, MEMBER_B));

            FamilyGroup group = FamilyGroup.create(command);

            assertThat(group.getMembers()).hasSize(2);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
            assertThat(group.hasMember(MEMBER_B)).isTrue();
        }

        @Test
        @DisplayName("should generate unique IDs for different groups")
        void shouldGenerateUniqueIds() {
            FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup("Novákovi", OWNER, Set.of());

            FamilyGroup group1 = FamilyGroup.create(command);
            FamilyGroup group2 = FamilyGroup.create(command);

            assertThat(group1.getId()).isNotEqualTo(group2.getId());
        }

        @Test
        @DisplayName("should reject blank group name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("", OWNER, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null owner")
        void shouldRejectNullOwner() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", null, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject null initial members")
        void shouldRejectNullInitialMembers() {
            assertThatThrownBy(() -> new FamilyGroup.CreateFamilyGroup("Novákovi", OWNER, null))
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

            FamilyGroup group = FamilyGroup.reconstruct(id, "Novákovi", Set.of(OWNER), memberships, null);

            assertThat(group.getId()).isEqualTo(id);
            assertThat(group.getName()).isEqualTo("Novákovi");
            assertThat(group.getOwners()).containsExactly(OWNER);
            assertThat(group.hasMember(MEMBER_A)).isTrue();
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
