package com.klabis.members;

import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member Domain Event Registration Tests")
class MemberDomainEventTest {

    @Test
    @DisplayName("should register MemberCreatedEvent when Member is created")
    void shouldRegisterEventWhenMemberCreated() {
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

        // When
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("jan@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        // Then
        List<Object> events = member.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MemberCreatedEvent.class);

        MemberCreatedEvent event = (MemberCreatedEvent) events.get(0);
        assertThat(event.getMemberId()).isEqualTo(member.getId());
        assertThat(event.getFirstName()).isEqualTo("Jan");
        assertThat(event.getLastName()).isEqualTo("Novák");
    }

    @Test
    @DisplayName("should clear domain events after clearDomainEvents is called")
    void shouldClearDomainEvents() {
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

        assertThat(member.getDomainEvents()).isNotEmpty();

        // When
        member.clearDomainEvents();

        // Then
        assertThat(member.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("should return unmodifiable list of domain events")
    void shouldReturnUnmodifiableEventList() {
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

        // When/Then
        List<Object> events = member.getDomainEvents();
        assertThat(events).isUnmodifiable();
    }

    @Test
    @DisplayName("should register event with guardian information for minors")
    void shouldRegisterEventWithGuardianForMinors() {
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

        // When
        Member member = Member.create(
                new RegistrationNumber("ZBM1001"),
                personalInformation,
                address,
                new EmailAddress("child@example.com"),
                new PhoneNumber("+420777333444"),
                guardian
        );

        // Then
        List<Object> events = member.getDomainEvents();
        assertThat(events).hasSize(1);

        MemberCreatedEvent event = (MemberCreatedEvent) events.get(0);
        assertThat(event.getGuardian()).isNotNull();
        assertThat(event.getGuardian().getFirstName()).isEqualTo("Parent");
    }

    @Test
    @DisplayName("should include all member data in registered event")
    void shouldIncludeAllMemberDataInEvent() {
        // Given
        PersonalInformation personalInformation = PersonalInformation.of(
                "Test",
                "User",
                LocalDate.of(2005, 6, 15),
                "SK",
                Gender.FEMALE
        );
        Address address = Address.of(
                "Testovací 5",
                "Bratislava",
                "81101",
                "SK"
        );

        // When
        Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInformation,
                address,
                new EmailAddress("test1@example.com"),
                new PhoneNumber("+420777888999"),
                null
        );

        // Then
        MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
        assertThat(event.getEmail()).isPresent().hasValueSatisfying(email ->
                assertThat(email.value()).isEqualTo("test1@example.com")
        );
        assertThat(event.getPhone()).isPresent().hasValueSatisfying(phone ->
                assertThat(phone.value()).isEqualTo("+420777888999")
        );
        assertThat(event.getNationality()).isEqualTo("SK");
        assertThat(event.getGender()).isEqualTo(Gender.FEMALE);
    }
}
