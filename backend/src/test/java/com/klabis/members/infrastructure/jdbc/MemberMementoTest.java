package com.klabis.members.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    private MemberTestDataBuilder buildTestMember() {
        LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
        EmailAddress email = EmailAddress.of("jan.novak@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");
        Address address = Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ"));
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");

        return MemberTestDataBuilder.aMember()
                .withRegistrationNumber(registrationNumber)
                .withAddress(address)
                .withName("Jan", "Novák")
                .withDateOfBirth(dateOfBirth)
                .withGender(Gender.MALE)
                .withNationality("CZ")
                .withBirthNumber("900515/1234")
                .withEmail(email)
                .withPhone(phone)
                .withAuditMetadata(new AuditMetadata(
                        Instant.now(),
                        "test-user",
                        Instant.now(),
                        "test-user",
                        0L
                ));
    }

    private Member createTestMember() {
        return buildTestMember().build();
    }

    private Member createMemberWithAllFields() {
        LocalDate dateOfBirth = LocalDate.of(2005, 3, 15);
        EmailAddress email = EmailAddress.of("petra.novakova@example.com");
        PhoneNumber phone = PhoneNumber.of("+420111222333");
        Address address = Address.of("Dětská 1", "Brno", "60200", Country.of("CZ"));
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        IdentityCard identityCard = IdentityCard.of("AB123456", LocalDate.now().plusYears(5));
        MedicalCourse medicalCourse = MedicalCourse.of(
                LocalDate.of(2023, 1, 15),
                Optional.of(LocalDate.of(2025, 1, 15))
        );
        TrainerLicense trainerLicense = TrainerLicense.of(TrainerLevel.T2, LocalDate.now().plusYears(3));
        BirthNumber birthNumber = BirthNumber.of("950301/1234");
        BankAccountNumber bankAccountNumber = BankAccountNumber.of("123456/0300");

        AuditMetadata auditMetadata = new AuditMetadata(
                Instant.now(),
                "test-user",
                Instant.now(),
                "test-user",
                1L
        );

        return MemberTestDataBuilder.aMember()
                .withRegistrationNumber(registrationNumber)
                .withAddress(address)
                .withEmail(email)
                .withPhone(phone)
                .withAuditMetadata(auditMetadata)
                .withName("Petra", "Nováková")
                .withDateOfBirth(dateOfBirth)
                .withGender(Gender.FEMALE)
                .withIdentityCard(identityCard)
                .withMedicalCourse(medicalCourse)
                .withTrainerLicense(trainerLicense)
                .withChipNumber("CHIP123")
                .withDrivingLicenseGroup(DrivingLicenseGroup.A)
                .withDietaryRestrictions("Vegetarian")
                .withBirthNumber(birthNumber)
                .withBankAccountNumber(bankAccountNumber)
                .withRefereeLicense(RefereeLicense.of(RefereeLevel.R2, LocalDate.now().plusYears(2)))
                .withDataIncomplete(true)
                .suspended(DeactivationReason.PRESTUP, "Transferred to another club")
                .build();

    }

    @Nested
    @DisplayName("Round-trip conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("should preserve every field in round-trip conversion")
        void shouldPreserveEveryFieldInRoundTrip() {
            Member member = createMemberWithAllFields();

            Member reconstructed = MemberMemento.from(member).toMember();

            assertThat(reconstructed).usingRecursiveComparison().isEqualTo(member);
        }

        @Test
        @DisplayName("should preserve a null address and unset optional fields in round-trip conversion")
        void shouldPreserveNullAddressAndUnsetOptionalFields() {
            // Arrange - an ORIS-imported member whose address was incomplete has no address at all
            Member original = MemberTestDataBuilder.aMember()
                    .withAddress(null)
                    .withDataIncomplete(true)
                    .build();

            // Act - Convert to memento and back
            MemberMemento memento = MemberMemento.from(original);
            Member reconstructed = memento.toMember();

            // Assert
            assertThat(reconstructed.getAddress()).isNull();
            assertThat(reconstructed).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Domain event delegation")
    class DomainEventDelegation {

        @Test
        @DisplayName("should delegate getDomainEvents to member")
        void shouldDelegateGetDomainEventsToMember() {
            // Arrange
            Member member = Member.register(buildTestMember().toRegisterMemberCommand());  // creates also event
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
            Member member = Member.register(buildTestMember().toRegisterMemberCommand());  // creates also event
            MemberMemento memento = MemberMemento.from(member);

            // Act
            memento.clearDomainEvents();

            // Assert
            assertThat(member.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should handle null member reference gracefully")
        void shouldHandleNullMemberReferenceGracefully() {
            MemberMemento memento = new MemberMemento();

            assertThat(memento.getDomainEvents()).isEmpty();
            assertThatCode(memento::clearDomainEvents).doesNotThrowAnyException();
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
            Member member = buildTestMember().withAuditMetadata(null).build();
            MemberMemento memento = MemberMemento.from(member);

            // Act
            boolean isNew = memento.isNew();

            // Assert - New mementos are marked as new
            assertThat(isNew).isTrue();
        }
    }
}
