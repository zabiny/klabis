package com.klabis.members;

import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberCreatedEvent Tests")
class MemberCreatedEventTest {

    @Test
    @DisplayName("should create event from Member aggregate")
    void shouldCreateEventFromMember() {
        // Given
        Member member = aMember()
                .withRegistrationNumber("ZBM0501")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail("jan@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then
        assertThat(event.memberId()).isEqualTo(member.getId());
        assertThat(event.registrationNumber()).isEqualTo(member.getRegistrationNumber());
        assertThat(event.firstName()).isEqualTo("Jan");
        assertThat(event.lastName()).isEqualTo("Novák");
        assertThat(event.dateOfBirth()).isEqualTo(LocalDate.of(2005, 6, 15));
        assertThat(event.nationality()).isEqualTo("CZ");
        assertThat(event.gender()).isEqualTo(Gender.MALE);
        assertThat(event.emailAsOptional()).isPresent();
        assertThat(event.emailAsOptional().get().value()).isEqualTo("jan@example.com");
        assertThat(event.phoneAsOptional()).isPresent();
        assertThat(event.phoneAsOptional().get().value()).isEqualTo("+420777888999");
        assertThat(event.guardian()).isNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("should include guardian information in event for minors")
    void shouldIncludeGuardianInformation() {
        // Given
        GuardianInformation guardian = new GuardianInformation(
                "Parent",
                "Name",
                "PARENT",
                EmailAddress.of("parent@example.com"),
                PhoneNumber.of("+420777111222")
        );

        Member member = aMember()
                .withRegistrationNumber("ZBM1001")
                .withName("Child", "Minor")
                .withDateOfBirth(LocalDate.of(2010, 1, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Dětská 1", "Brno", "60200", "CZ"))
                .withEmail("child@example.com")
                .withPhone("+420777333444")
                .withGuardian(guardian)
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then
        assertThat(event.guardian()).isNotNull();
        assertThat(event.guardian().getFirstName()).isEqualTo("Parent");
        assertThat(event.guardian().getLastName()).isEqualTo("Name");
        assertThat(event.isMinor()).isTrue();
    }

    @Test
    @DisplayName("should identify adult members")
    void shouldIdentifyAdultMembers() {
        // Given
        Member member = aMember()
                .withRegistrationNumber("ZBM9001")
                .withName("Adult", "Member")
                .withDateOfBirth(LocalDate.of(1990, 5, 10))
                .withNationality("CZ")
                .withGender(Gender.FEMALE)
                .withAddress(Address.of("Dospělá 10", "Plzeň", "30100", "CZ"))
                .withEmail("adult@example.com")
                .withPhone("+420777555666")
                .withNoGuardian()
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then
        assertThat(event.isMinor()).isFalse();
    }

    @Test
    @DisplayName("should return primary email from member emails")
    void shouldReturnPrimaryEmailFromMember() {
        // Given
        Member member = aMember()
                .withRegistrationNumber("ZBM0501")
                .withName("Test", "User")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Testovací 5", "Ústí nad Labem", "40001", "CZ"))
                .withEmail("test@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then
        assertThat(event.getPrimaryEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("should return guardian email as primary when member has no email")
    void shouldReturnGuardianEmailWhenMemberHasNoEmail() {
        // Given
        GuardianInformation guardian = new GuardianInformation(
                "Parent",
                "Name",
                "PARENT",
                EmailAddress.of("parent@example.com"),
                PhoneNumber.of("+420777111222")
        );

        Member member = aMember()
                .withRegistrationNumber("ZBM1001")
                .withName("Child", "Minor")
                .withDateOfBirth(LocalDate.of(2010, 1, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Dětská 2", "Olomouc", "77100", "CZ"))
                .withEmail((EmailAddress) null)  // No member email
                .withPhone("+420777333444")
                .withGuardian(guardian)
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then
        assertThat(event.getPrimaryEmail()).isEqualTo("parent@example.com");
    }

    @Test
    @DisplayName("should return optional email and phone")
    void shouldReturnOptionalEmailAndPhone() {
        // Given
        Member member = aMember()
                .withRegistrationNumber("ZBM0501")
                .withName("Test", "User")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Testovací 5", "Liberec", "46001", "CZ"))
                .withEmail("test@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();

        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // When/Then
        assertThat(event.emailAsOptional()).isPresent();
        assertThat(event.emailAsOptional().get().value()).isEqualTo("test@example.com");
        assertThat(event.phoneAsOptional()).isPresent();
        assertThat(event.phoneAsOptional().get().value()).isEqualTo("+420777888999");
    }

    @Test
    @DisplayName("should validate required fields")
    void shouldValidateRequiredFields() {
        Address address = Address.of(
                "Testovací 5",
                "Hradec Králové",
                "50002",
                "CZ"
        );
        assertThatThrownBy(() -> new MemberCreatedEvent(
                null,
                new RegistrationNumber("ZBM0501"),
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                address,
                new EmailAddress("test@example.com"),
                new PhoneNumber("+420777888999"),
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Member ID");
    }

    @Test
    @DisplayName("should have toString without PII for GDPR compliance")
    void shouldHaveToStringWithoutPII() {
        // Given
        Member member = aMember()
                .withRegistrationNumber("ZBM0501")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail("jan@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        // Then - toString should NOT contain PII for GDPR compliance
        String toString = event.toString();
        assertThat(toString).contains("MemberCreatedEvent");
        assertThat(toString).contains("ZBM0501"); // Non-PII identifier

        // Verify PII is NOT in toString (GDPR compliance)
        assertThat(toString).doesNotContain("Jan"); // First name
        assertThat(toString).doesNotContain("Novák"); // Last name
        assertThat(toString).doesNotContain("@example.com"); // Email
        assertThat(toString).doesNotContain("+420"); // Phone
        assertThat(toString).doesNotContain("Hlavní"); // Address
        assertThat(toString).doesNotContain("Praha"); // Address
        assertThat(toString).doesNotContain("2005-06-15"); // Date of birth
    }
}
