package com.klabis.usergroups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
