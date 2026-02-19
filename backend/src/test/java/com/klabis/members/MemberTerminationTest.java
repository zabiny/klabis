package com.klabis.members;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Member termination functionality.
 * <p>
 * Tests the termination business logic:
 * - Terminating an active member
 * - Preventing termination of already terminated member
 * - Recording termination reason and metadata
 * - Publishing MemberTerminatedEvent
 */
@DisplayName("Member Termination")
class MemberTerminationTest {

    private Member activeMember;
    private UserId adminUserId;

    @BeforeEach
    void setUp() {
        adminUserId = new UserId(UUID.randomUUID());

        LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
        EmailAddress email = EmailAddress.of("jan.novak@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");
        Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Jan",
                "Novák",
                dateOfBirth,
                "CZ",
                Gender.MALE
        );

        activeMember = Member.create(
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                null
        );
    }

    @Nested
    @DisplayName("Termination state")
    class TerminationState {

        @Test
        @DisplayName("active member should not have termination details")
        void activeMemberShouldNotHaveTerminationDetails() {
            // Assert
            assertThat(activeMember.isActive()).isTrue();
            assertThat(activeMember.getDeactivationReason()).isNull();
            assertThat(activeMember.getDeactivatedAt()).isNull();
            assertThat(activeMember.getDeactivationNote()).isNull();
            assertThat(activeMember.getDeactivatedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("handle(TerminateMembership) method")
    class HandleTerminateMembership {

        @Test
        @DisplayName("should terminate active member with reason")
        void shouldTerminateActiveMemberWithReason() {
            // Arrange
            Member.TerminateMembership command = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );

            // Act
            activeMember.handle(command);

            // Assert
            assertThat(activeMember.isActive()).isFalse();
            assertThat(activeMember.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(activeMember.getDeactivatedAt()).isNotNull();
            assertThat(activeMember.getDeactivationNote()).isEqualTo("Member requested termination");
            assertThat(activeMember.getDeactivatedBy()).isEqualTo(adminUserId);
        }

        @Test
        @DisplayName("should publish MemberTerminatedEvent when terminating")
        void shouldPublishEventWhenTerminating() {
            // Arrange
            Member.TerminateMembership command = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.PRESTUP,
                    null
            );

            // Act
            activeMember.handle(command);

            // Assert - should have MemberCreatedEvent (from creation) + MemberTerminatedEvent
            assertThat(activeMember.getDomainEvents())
                    .hasSize(2)
                    .anyMatch(event -> event instanceof MemberTerminatedEvent);
        }

        @Test
        @DisplayName("should reject termination of already terminated member")
        void shouldRejectTerminationOfAlreadyTerminatedMember() {
            // Arrange
            Member.TerminateMembership firstCommand = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "First termination"
            );
            activeMember.handle(firstCommand);

            UserId anotherAdmin = new UserId(UUID.randomUUID());
            Member.TerminateMembership secondCommand = new Member.TerminateMembership(
                    anotherAdmin,
                    DeactivationReason.OTHER,
                    "Second termination attempt"
            );

            // Act & Assert
            assertThatThrownBy(() -> activeMember.handle(secondCommand))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already terminated");
        }

        @Test
        @DisplayName("should allow termination with all reason types")
        void shouldAllowTerminationWithAllReasonTypes() {
            // Test ODHLASKA
            activeMember.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.ODHLASKA, "Note 1"));
            assertThat(activeMember.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);

            // Create new member for PRESTUP test
            Member member2 = createTestMember();
            member2.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.PRESTUP, "Note 2"));
            assertThat(member2.getDeactivationReason()).isEqualTo(DeactivationReason.PRESTUP);

            // Create new member for OTHER test
            Member member3 = createTestMember();
            member3.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.OTHER, "Note 3"));
            assertThat(member3.getDeactivationReason()).isEqualTo(DeactivationReason.OTHER);
        }

        @Test
        @DisplayName("should record termination timestamp")
        void shouldRecordTerminationTimestamp() {
            // Arrange
            var beforeTermination = System.currentTimeMillis();
            activeMember.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.ODHLASKA, null));
            var afterTermination = System.currentTimeMillis();

            // Assert
            assertThat(activeMember.getDeactivatedAt()).isNotNull();
            assertThat(activeMember.getDeactivatedAt().toEpochMilli())
                    .isGreaterThanOrEqualTo(beforeTermination)
                    .isLessThanOrEqualTo(afterTermination);
        }
    }

    private Member createTestMember() {
        LocalDate dateOfBirth = LocalDate.of(1992, 3, 20);
        EmailAddress email = EmailAddress.of("test@example.com");
        PhoneNumber phone = PhoneNumber.of("+420111111111");
        Address address = Address.of("Test 1", "Test", "11111", "CZ");
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9201");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Test",
                "Test",
                dateOfBirth,
                "CZ",
                Gender.MALE
        );

        return Member.create(
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                null
        );
    }
}
