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

@DisplayName("UserGroup owner management domain unit tests")
class UserGroupOwnerManagementTest {

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId SECOND_OWNER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MemberId NEW_MEMBER = new MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    private FreeGroup createGroupWithOwner(MemberId owner) {
        return FreeGroup.reconstruct(new UserGroupId(UUID.randomUUID()), "Test Group", Set.of(owner), Set.of(), null);
    }

    private FreeGroup createGroupWithOwners(Set<MemberId> owners) {
        return FreeGroup.reconstruct(new UserGroupId(UUID.randomUUID()), "Test Group", owners, Set.of(), null);
    }

    @Nested
    @DisplayName("addOwner()")
    class AddOwner {

        @Test
        @DisplayName("should add a new owner to the group")
        void shouldAddNewOwner() {
            FreeGroup group = createGroupWithOwner(OWNER);

            group.addOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactlyInAnyOrder(OWNER, SECOND_OWNER);
        }

        @Test
        @DisplayName("should be idempotent when adding an existing owner")
        void shouldBeIdempotentForExistingOwner() {
            FreeGroup group = createGroupWithOwner(OWNER);

            group.addOwner(OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should reject null memberId")
        void shouldRejectNullMemberId() {
            FreeGroup group = createGroupWithOwner(OWNER);

            assertThatThrownBy(() -> group.addOwner(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("removeOwner()")
    class RemoveOwner {

        @Test
        @DisplayName("should remove an owner when multiple owners exist")
        void shouldRemoveOwnerWhenMultipleExist() {
            FreeGroup group = createGroupWithOwners(Set.of(OWNER, SECOND_OWNER));

            group.removeOwner(SECOND_OWNER);

            assertThat(group.getOwners()).containsExactly(OWNER);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing sole owner")
        void shouldThrowWhenRemovingLastOwner() {
            FreeGroup group = createGroupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(OWNER))
                    .isInstanceOf(UserGroup.CannotRemoveLastOwnerException.class)
                    .hasMessageContaining("last owner");
        }

        @Test
        @DisplayName("should reject null memberId")
        void shouldRejectNullMemberId() {
            FreeGroup group = createGroupWithOwner(OWNER);

            assertThatThrownBy(() -> group.removeOwner(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isLastOwner()")
    class IsLastOwner {

        @Test
        @DisplayName("should return true when member is the only owner")
        void shouldReturnTrueForSoleOwner() {
            FreeGroup group = createGroupWithOwner(OWNER);

            assertThat(group.isLastOwner(OWNER)).isTrue();
        }

        @Test
        @DisplayName("should return false when there are multiple owners")
        void shouldReturnFalseWhenMultipleOwners() {
            FreeGroup group = createGroupWithOwners(Set.of(OWNER, SECOND_OWNER));

            assertThat(group.isLastOwner(OWNER)).isFalse();
            assertThat(group.isLastOwner(SECOND_OWNER)).isFalse();
        }

        @Test
        @DisplayName("should return false for a non-owner member")
        void shouldReturnFalseForNonOwner() {
            FreeGroup group = createGroupWithOwner(OWNER);

            assertThat(group.isLastOwner(NEW_MEMBER)).isFalse();
        }
    }
}
