package com.klabis.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberCreatedEvent Tests")
class MemberCreatedEventTest {

    @Test
    @DisplayName("should create event from Member aggregate")
    void shouldCreateEventFromMember() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE
        );
        Address address = Address.of(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("jan@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then
        assertThat(event.getMemberId()).isEqualTo(member.getId());
        assertThat(event.getRegistrationNumber()).isEqualTo(member.getRegistrationNumber());
        assertThat(event.getFirstName()).isEqualTo("Jan");
        assertThat(event.getLastName()).isEqualTo("Novák");
        assertThat(event.getDateOfBirth()).isEqualTo(LocalDate.of(2005, 6, 15));
        assertThat(event.getNationality()).isEqualTo("CZ");
        assertThat(event.getGender()).isEqualTo(Gender.MALE);
        assertThat(event.getEmail()).isPresent();
        assertThat(event.getEmail().get().value()).isEqualTo("jan@example.com");
        assertThat(event.getPhone()).isPresent();
        assertThat(event.getPhone().get().value()).isEqualTo("+420777888999");
        assertThat(event.getGuardian()).isNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("should include guardian information in event for minors")
    void shouldIncludeGuardianInformation() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Child",
                "Minor",
                LocalDate.of(2010, 1, 15),
                "CZ",
                Gender.MALE
        );
        GuardianInformation guardian = new GuardianInformation(
                "Parent",
                "Name",
                "PARENT",
                EmailAddress.of("parent@example.com"),
                PhoneNumber.of("+420777111222")
        );
        Address address = Address.of(
                "Dětská 1",
                "Brno",
                "60200",
                "CZ"
        );

        Member member = Member.create(
                new RegistrationNumber("ZBM1001"),
                personalInformation,
                address,
                new EmailAddress("child@example.com"),
                new PhoneNumber("+420777333444"),
                guardian
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then
        assertThat(event.getGuardian()).isNotNull();
        assertThat(event.getGuardian().getFirstName()).isEqualTo("Parent");
        assertThat(event.getGuardian().getLastName()).isEqualTo("Name");
        assertThat(event.isMinor()).isTrue();
    }

    @Test
    @DisplayName("should identify adult members")
    void shouldIdentifyAdultMembers() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Adult",
                "Member",
                LocalDate.of(1990, 5, 10),
                "CZ",
                Gender.FEMALE
        );
        Address address = Address.of(
                "Dospělá 10",
                "Plzeň",
                "30100",
                "CZ"
        );
        Member member = Member.create(
                new RegistrationNumber("ZBM9001"),
                personalInformation,
                address,
                new EmailAddress("adult@example.com"),
                new PhoneNumber("+420777555666"),
                null
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then
        assertThat(event.isMinor()).isFalse();
    }

    @Test
    @DisplayName("should return primary email from member emails")
    void shouldReturnPrimaryEmailFromMember() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Test",
                "User",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE
        );
        Address address = Address.of(
                "Testovací 5",
                "Ústí nad Labem",
                "40001",
                "CZ"
        );
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("test@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then
        assertThat(event.getPrimaryEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("should return guardian email as primary when member has no email")
    void shouldReturnGuardianEmailWhenMemberHasNoEmail() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Child",
                "Minor",
                LocalDate.of(2010, 1, 15),
                "CZ",
                Gender.MALE
        );
        GuardianInformation guardian = new GuardianInformation(
                "Parent",
                "Name",
                "PARENT",
                EmailAddress.of("parent@example.com"),
                PhoneNumber.of("+420777111222")
        );
        Address address = Address.of(
                "Dětská 2",
                "Olomouc",
                "77100",
                "CZ"
        );

        Member member = Member.create(
                new RegistrationNumber("ZBM1001"),
                personalInformation,
                address,
                null, // No member email
                new PhoneNumber("+420777333444"),
                guardian
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then
        assertThat(event.getPrimaryEmail()).isEqualTo("parent@example.com");
    }

    @Test
    @DisplayName("should return optional email and phone")
    void shouldReturnOptionalEmailAndPhone() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Test",
                "User",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE
        );
        Address address = Address.of(
                "Testovací 5",
                "Liberec",
                "46001",
                "CZ"
        );
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("test@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // When/Then
        assertThat(event.getEmail()).isPresent();
        assertThat(event.getEmail().get().value()).isEqualTo("test@example.com");
        assertThat(event.getPhone()).isPresent();
        assertThat(event.getPhone().get().value()).isEqualTo("+420777888999");
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
    @DisplayName("should have meaningful toString without PII")
    void shouldHaveMeaningfulToStringWithoutPII() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE
        );
        Address address = Address.of(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("jan@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        // When
        MemberCreatedEvent event = MemberCreatedEvent.fromMember(member);

        // Then - toString should NOT contain PII for GDPR compliance
        String toString = event.toString();
        assertThat(toString).contains("MemberCreatedEvent");
        assertThat(toString).contains("eventId=");
        assertThat(toString).contains("memberId=");
        assertThat(toString).contains("ZBM0501");
        assertThat(toString).contains("nationality='CZ'");
        assertThat(toString).contains("isMinor=false");
        assertThat(toString).contains("occurredAt=");

        // Verify PII is NOT in toString
        assertThat(toString).doesNotContain("Jan");
        assertThat(toString).doesNotContain("Novák");
        assertThat(toString).doesNotContain("@example.com");
        assertThat(toString).doesNotContain("+420");
        assertThat(toString).doesNotContain("Hlavní");
        assertThat(toString).doesNotContain("Praha");
    }
}
