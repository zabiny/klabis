package com.klabis.members.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MemberMemento class.
 * <p>
 * Tests the conversion between Member domain entity and MemberMemento persistence object:
 * - from(Member) - creates memento from domain entity
 * - toMember() - reconstructs domain entity from memento
 * - Round-trip: Member → Memento → Member preserves all data
 */
@DisplayName("MemberMemento Tests")
class MemberMementoTest {

    private Member createTestMember() {
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

        Member member = Member.create(
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                null // no guardian for adult
        );

        // Set audit metadata manually for testing
        AuditMetadata auditMetadata = new AuditMetadata(
                Instant.now(),
                "test-user",
                Instant.now(),
                "test-user",
                0L
        );
        member.updateAuditMetadata(auditMetadata);

        return member;
    }

    private Member createMemberWithAllFields() {
        LocalDate dateOfBirth = LocalDate.of(2005, 3, 15);
        EmailAddress email = EmailAddress.of("petra.novakova@example.com");
        PhoneNumber phone = PhoneNumber.of("+420111222333");
        Address address = Address.of("Dětská 1", "Brno", "60200", "CZ");
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Petra",
                "Nováková",
                dateOfBirth,
                "CZ",
                Gender.FEMALE
        );
        GuardianInformation guardian = new GuardianInformation(
                "Pavel",
                "Novák",
                "PARENT",
                EmailAddress.of("pavel.novak@example.com"),
                PhoneNumber.of("+420987654321")
        );
        IdentityCard identityCard = IdentityCard.of("AB123456", LocalDate.now().plusYears(5));
        MedicalCourse medicalCourse = MedicalCourse.of(
                LocalDate.of(2023, 1, 15),
                Optional.of(LocalDate.of(2025, 1, 15))
        );
        TrainerLicense trainerLicense = TrainerLicense.of("TRAINER001", LocalDate.now().plusYears(3));

        Member member = Member.create(
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                guardian
        );

        member.updateDocuments(identityCard, medicalCourse, trainerLicense);
        member.updateMemberDetails(
                null, null, null, null, null,
                "CHIP123", DrivingLicenseGroup.A, "Vegetarian", null
        );

        // Set audit metadata
        AuditMetadata auditMetadata = new AuditMetadata(
                Instant.now(),
                "test-user",
                Instant.now(),
                "test-user",
                1L
        );
        member.updateAuditMetadata(auditMetadata);

        return member;
    }

    @Nested
    @DisplayName("from(Member) method")
    class FromMemberMethod {

        @Test
        @DisplayName("should create memento from member with basic fields")
        void shouldCreateMementoFromMemberWithBasicFields() {
            // Arrange
            Member member = createTestMember();

            // Act
            MemberMemento memento = MemberMemento.from(member);

            // Assert - convert back to domain to verify
            Member reconstructed = memento.toMember();
            MemberAssert.assertThat(reconstructed)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(LocalDate.of(1990, 5, 15))
                    .hasNationality("CZ")
                    .hasGender(Gender.MALE);
            assertThat(memento.getId()).isEqualTo(member.getId().uuid());
            assertThat(reconstructed.getRegistrationNumber().getValue()).isEqualTo("ZBM9001");
        }

        @Test
        @DisplayName("should create memento from member with all fields")
        void shouldCreateMementoFromMemberWithAllFields() {
            // Arrange
            Member member = createMemberWithAllFields();

            // Act
            MemberMemento memento = MemberMemento.from(member);

            // Assert - convert back to domain to verify
            Member reconstructed = memento.toMember();

            // Assert - Personal info
            MemberAssert.assertThat(reconstructed)
                    .hasFirstName("Petra")
                    .hasLastName("Nováková")
                    .hasGuardianNotNull();

            // Assert - Address
            assertThat(reconstructed.getAddress().street()).isEqualTo("Dětská 1");
            assertThat(reconstructed.getAddress().city()).isEqualTo("Brno");
            assertThat(reconstructed.getAddress().postalCode()).isEqualTo("60200");
            assertThat(reconstructed.getAddress().country()).isEqualTo("CZ");

            // Assert - Contact
            assertThat(reconstructed.getEmail().value()).isEqualTo("petra.novakova@example.com");
            assertThat(reconstructed.getPhone().value()).isEqualTo("+420111222333");

            // Assert - Guardian details
            assertThat(reconstructed.getGuardian().getFirstName()).isEqualTo("Pavel");
            assertThat(reconstructed.getGuardian().getLastName()).isEqualTo("Novák");
            assertThat(reconstructed.getGuardian().getRelationship()).isEqualTo("PARENT");
            assertThat(reconstructed.getGuardian().getEmailValue()).isEqualTo("pavel.novak@example.com");
            assertThat(reconstructed.getGuardian().getPhoneValue()).isEqualTo("+420987654321");

            // Assert - Documents
            assertThat(reconstructed.getIdentityCard().cardNumber()).isEqualTo("AB123456");
            assertThat(reconstructed.getMedicalCourse().completionDate()).isEqualTo(LocalDate.of(2023, 1, 15));
            assertThat(reconstructed.getMedicalCourse().validityDate()).hasValue(LocalDate.of(2025, 1, 15));
            assertThat(reconstructed.getTrainerLicense().licenseNumber()).isEqualTo("TRAINER001");

            // Assert - Other fields
            MemberAssert.assertThat(reconstructed).isActive();
            assertThat(reconstructed.getChipNumber()).isEqualTo("CHIP123");
            assertThat(reconstructed.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.A);
            assertThat(reconstructed.getDietaryRestrictions()).isEqualTo("Vegetarian");

            // Assert - Audit fields
            assertThat(reconstructed.getCreatedAt()).isNotNull();
            assertThat(reconstructed.getCreatedBy()).isEqualTo("test-user");
            assertThat(reconstructed.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should store transient reference to member for domain events")
        void shouldStoreTransientReferenceToMember() {
            // Arrange
            Member member = createTestMember();

            // Act
            MemberMemento memento = MemberMemento.from(member);

            // Assert - member reference is stored for domain event delegation
            // We can verify this indirectly by checking that domain events are accessible
            assertThat(memento.getDomainEvents()).hasSize(1);
            assertThat(memento.getDomainEvents().get(0)).isInstanceOf(MemberCreatedEvent.class);
        }
    }

    @Nested
    @DisplayName("toMember() method")
    class ToMemberMethod {

        @Test
        @DisplayName("should reconstruct member from memento with basic fields")
        void shouldReconstructMemberFromMementoWithBasicFields() {
            // Arrange
            Member originalMember = createTestMember();
            MemberMemento memento = MemberMemento.from(originalMember);

            // Act
            Member reconstructed = memento.toMember();

            // Assert
            assertThat(reconstructed.getId()).isEqualTo(originalMember.getId());
            assertThat(reconstructed.getRegistrationNumber()).isEqualTo(originalMember.getRegistrationNumber());
            assertThat(reconstructed.getFirstName()).isEqualTo("Jan");
            assertThat(reconstructed.getLastName()).isEqualTo("Novák");
            assertThat(reconstructed.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(reconstructed.getNationality()).isEqualTo("CZ");
            assertThat(reconstructed.getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("should reconstruct member from memento with all fields")
        void shouldReconstructMemberFromMementoWithAllFields() {
            // Arrange
            Member originalMember = createMemberWithAllFields();
            MemberMemento memento = MemberMemento.from(originalMember);

            // Act
            Member reconstructed = memento.toMember();

            // Assert - Personal info
            assertThat(reconstructed.getFirstName()).isEqualTo("Petra");
            assertThat(reconstructed.getLastName()).isEqualTo("Nováková");

            // Assert - Address
            assertThat(reconstructed.getAddress().street()).isEqualTo("Dětská 1");
            assertThat(reconstructed.getAddress().city()).isEqualTo("Brno");
            assertThat(reconstructed.getAddress().postalCode()).isEqualTo("60200");
            assertThat(reconstructed.getAddress().country()).isEqualTo("CZ");

            // Assert - Contact
            assertThat(reconstructed.getEmail().value()).isEqualTo("petra.novakova@example.com");
            assertThat(reconstructed.getPhone().value()).isEqualTo("+420111222333");

            // Assert - Guardian
            assertThat(reconstructed.getGuardian()).isNotNull();
            assertThat(reconstructed.getGuardian().getFirstName()).isEqualTo("Pavel");
            assertThat(reconstructed.getGuardian().getLastName()).isEqualTo("Novák");
            assertThat(reconstructed.getGuardian().getRelationship()).isEqualTo("PARENT");

            // Assert - Documents
            assertThat(reconstructed.getIdentityCard()).isNotNull();
            assertThat(reconstructed.getIdentityCard().cardNumber()).isEqualTo("AB123456");
            assertThat(reconstructed.getMedicalCourse()).isNotNull();
            assertThat(reconstructed.getMedicalCourse().completionDate()).isEqualTo(LocalDate.of(2023, 1, 15));
            assertThat(reconstructed.getTrainerLicense()).isNotNull();
            assertThat(reconstructed.getTrainerLicense().licenseNumber()).isEqualTo("TRAINER001");

            // Assert - Other fields
            assertThat(reconstructed.getChipNumber()).isEqualTo("CHIP123");
            assertThat(reconstructed.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.A);
            assertThat(reconstructed.getDietaryRestrictions()).isEqualTo("Vegetarian");
            assertThat(reconstructed.isActive()).isTrue();

            // Assert - Audit metadata
            assertThat(reconstructed.getCreatedAt()).isNotNull();
            assertThat(reconstructed.getCreatedBy()).isEqualTo("test-user");
            assertThat(reconstructed.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should reconstruct member with null optional fields")
        void shouldReconstructMemberWithNullOptionalFields() {
            // Arrange
            Member member = createTestMember(); // No guardian, no documents
            MemberMemento memento = MemberMemento.from(member);

            // Act
            Member reconstructed = memento.toMember();

            // Assert - Null fields are preserved
            assertThat(reconstructed.getGuardian()).isNull();
            assertThat(reconstructed.getIdentityCard()).isNull();
            assertThat(reconstructed.getMedicalCourse()).isNull();
            assertThat(reconstructed.getTrainerLicense()).isNull();
            assertThat(reconstructed.getChipNumber()).isNull();
            assertThat(reconstructed.getDrivingLicenseGroup()).isNull();
            assertThat(reconstructed.getDietaryRestrictions()).isNull();
        }
    }

    @Nested
    @DisplayName("Round-trip conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("should preserve all data in round-trip conversion")
        void shouldPreserveAllDataInRoundTripConversion() {
            // Arrange
            Member original = createMemberWithAllFields();

            // Act - Convert to memento and back
            MemberMemento memento = MemberMemento.from(original);
            Member reconstructed = memento.toMember();

            // Assert - All fields preserved
            assertThat(reconstructed.getId()).isEqualTo(original.getId());
            assertThat(reconstructed.getRegistrationNumber()).isEqualTo(original.getRegistrationNumber());
            assertThat(reconstructed.getFirstName()).isEqualTo(original.getFirstName());
            assertThat(reconstructed.getLastName()).isEqualTo(original.getLastName());
            assertThat(reconstructed.getDateOfBirth()).isEqualTo(original.getDateOfBirth());
            assertThat(reconstructed.getNationality()).isEqualTo(original.getNationality());
            assertThat(reconstructed.getGender()).isEqualTo(original.getGender());
            assertThat(reconstructed.getEmail()).isEqualTo(original.getEmail());
            assertThat(reconstructed.getPhone()).isEqualTo(original.getPhone());
            assertThat(reconstructed.getAddress()).isEqualTo(original.getAddress());
            assertThat(reconstructed.getGuardian()).isEqualTo(original.getGuardian());
            assertThat(reconstructed.isActive()).isEqualTo(original.isActive());
            assertThat(reconstructed.getChipNumber()).isEqualTo(original.getChipNumber());
            assertThat(reconstructed.getDrivingLicenseGroup()).isEqualTo(original.getDrivingLicenseGroup());
            assertThat(reconstructed.getDietaryRestrictions()).isEqualTo(original.getDietaryRestrictions());
        }

        @Test
        @DisplayName("should preserve audit metadata in round-trip conversion")
        void shouldPreserveAuditMetadataInRoundTripConversion() {
            // Arrange
            Member original = createTestMember();

            // Act - Convert to memento and back
            MemberMemento memento = MemberMemento.from(original);
            Member reconstructed = memento.toMember();

            // Assert - Audit metadata preserved
            assertThat(reconstructed.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(reconstructed.getCreatedBy()).isEqualTo(original.getCreatedBy());
            assertThat(reconstructed.getLastModifiedAt()).isEqualTo(original.getLastModifiedAt());
            assertThat(reconstructed.getLastModifiedBy()).isEqualTo(original.getLastModifiedBy());
            assertThat(reconstructed.getVersion()).isEqualTo(original.getVersion());
        }
    }

    @Nested
    @DisplayName("getAuditMetadata() method")
    class GetAuditMetadataMethod {

        @Test
        @DisplayName("should return audit metadata value object")
        void shouldReturnAuditMetadataValueObject() {
            // Arrange
            Member member = createTestMember();
            MemberMemento memento = MemberMemento.from(member);

            // Act
            AuditMetadata auditMetadata = memento.getAuditMetadata();

            // Assert
            assertThat(auditMetadata).isNotNull();
            assertThat(auditMetadata.createdAt()).isEqualTo(member.getCreatedAt());
            assertThat(auditMetadata.createdBy()).isEqualTo(member.getCreatedBy());
            assertThat(auditMetadata.lastModifiedAt()).isEqualTo(member.getLastModifiedAt());
            assertThat(auditMetadata.lastModifiedBy()).isEqualTo(member.getLastModifiedBy());
            assertThat(auditMetadata.version()).isEqualTo(member.getVersion());
        }
    }

    @Nested
    @DisplayName("Domain event delegation")
    class DomainEventDelegation {

        @Test
        @DisplayName("should delegate getDomainEvents to member")
        void shouldDelegateGetDomainEventsToMember() {
            // Arrange
            Member member = createTestMember();
            MemberMemento memento = MemberMemento.from(member);

            // Act
            var events = memento.getDomainEvents();

            // Assert - Member has a MemberCreatedEvent
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(MemberCreatedEvent.class);
        }

        @Test
        @DisplayName("should delegate clearDomainEvents to member")
        void shouldDelegateClearDomainEventsToMember() {
            // Arrange
            Member member = createTestMember();
            MemberMemento memento = MemberMemento.from(member);

            // Act
            memento.clearDomainEvents();

            // Assert
            assertThat(member.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should handle null member reference gracefully")
        void shouldHandleNullMemberReferenceGracefully() {
            // Arrange - Create memento without member reference
            MemberMemento memento = new MemberMemento();

            // Act
            var events = memento.getDomainEvents();

            // Assert - Returns empty list when member is null
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("should handle clearDomainEvents with null member reference")
        void shouldHandleClearDomainEventsWithNullMemberReference() {
            // Arrange - Create memento without member reference
            MemberMemento memento = new MemberMemento();

            // Act - Should not throw exception
            memento.clearDomainEvents();

            // Assert - No exception thrown
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DisplayName("Persistable implementation")
    class PersistableImplementation {

        @Test
        @DisplayName("should return correct ID")
        void shouldReturnCorrectId() {
            // Arrange
            Member member = createTestMember();
            MemberMemento memento = MemberMemento.from(member);

            // Act
            UUID id = memento.getId();

            // Assert
            assertThat(id).isEqualTo(member.getId().uuid());
        }

        @Test
        @DisplayName("should return isNew=true for new memento")
        void shouldReturnIsNewTrueForNewMemento() {
            // Arrange
            Member member = createTestMember();
            member.updateAuditMetadata(null);
            MemberMemento memento = MemberMemento.from(member);

            // Act
            boolean isNew = memento.isNew();

            // Assert - New mementos are marked as new
            assertThat(isNew).isTrue();
        }
    }
}
