package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invitation domain unit tests")
class InvitationTest {

    private static final UserId INVITER = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId INVITED = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Nested
    @DisplayName("createPending()")
    class CreatePendingMethod {

        @Test
        @DisplayName("should create invitation with PENDING status")
        void shouldCreatePendingInvitation() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            assertThat(invitation.getId()).isNotNull();
            assertThat(invitation.getInvitedUser()).isEqualTo(INVITED);
            assertThat(invitation.getInvitedBy()).isEqualTo(INVITER);
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
            assertThat(invitation.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate unique invitation IDs")
        void shouldGenerateUniqueIds() {
            Invitation inv1 = Invitation.createPending(INVITER, INVITED);
            Invitation inv2 = Invitation.createPending(INVITER, INVITED);

            assertThat(inv1.getId()).isNotEqualTo(inv2.getId());
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructMethod {

        @Test
        @DisplayName("should reconstruct invitation from persistence data")
        void shouldReconstructFromPersistenceData() {
            InvitationId id = InvitationId.newId();
            Instant createdAt = Instant.now();

            Invitation invitation = Invitation.reconstruct(id, INVITED, INVITER, InvitationStatus.ACCEPTED, createdAt);

            assertThat(invitation.getId()).isEqualTo(id);
            assertThat(invitation.getInvitedUser()).isEqualTo(INVITED);
            assertThat(invitation.getInvitedBy()).isEqualTo(INVITER);
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
            assertThat(invitation.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("accept()")
    class AcceptMethod {

        @Test
        @DisplayName("should change status to ACCEPTED")
        void shouldChangeStatusToAccepted() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            invitation.accept();

            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should throw when accepting a non-PENDING invitation")
        void shouldThrowWhenNotPending() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);
            invitation.accept();

            assertThatThrownBy(invitation::accept)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("reject()")
    class RejectMethod {

        @Test
        @DisplayName("should change status to REJECTED")
        void shouldChangeStatusToRejected() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            invitation.reject();

            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw when rejecting a non-PENDING invitation")
        void shouldThrowWhenNotPending() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);
            invitation.reject();

            assertThatThrownBy(invitation::reject)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("isPending() and isForUser()")
    class QueryMethods {

        @Test
        @DisplayName("should return true for pending invitation")
        void shouldReturnTrueForPendingStatus() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            assertThat(invitation.isPending()).isTrue();
        }

        @Test
        @DisplayName("should return false after acceptance")
        void shouldReturnFalseAfterAcceptance() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);
            invitation.accept();

            assertThat(invitation.isPending()).isFalse();
        }

        @Test
        @DisplayName("isForUser() should return true for invited user")
        void shouldReturnTrueForInvitedUser() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            assertThat(invitation.isForUser(INVITED)).isTrue();
        }

        @Test
        @DisplayName("isForUser() should return false for different user")
        void shouldReturnFalseForDifferentUser() {
            Invitation invitation = Invitation.createPending(INVITER, INVITED);

            assertThat(invitation.isForUser(INVITER)).isFalse();
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when same invitation ID")
        void shouldBeEqualForSameId() {
            InvitationId id = InvitationId.newId();
            Invitation inv1 = Invitation.reconstruct(id, INVITED, INVITER, InvitationStatus.PENDING, Instant.now());
            Invitation inv2 = Invitation.reconstruct(id, INVITED, INVITER, InvitationStatus.ACCEPTED, Instant.now());

            assertThat(inv1).isEqualTo(inv2);
            assertThat(inv1.hashCode()).isEqualTo(inv2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different invitation IDs")
        void shouldNotBeEqualForDifferentIds() {
            Invitation inv1 = Invitation.createPending(INVITER, INVITED);
            Invitation inv2 = Invitation.createPending(INVITER, INVITED);

            assertThat(inv1).isNotEqualTo(inv2);
        }
    }
}
