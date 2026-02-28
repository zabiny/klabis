package com.klabis.members;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberReactivatedEvent Tests")
class MemberReactivatedEventTest {

    @Test
    @DisplayName("should create event from Member aggregate and command")
    void shouldCreateEventFromMember() {
        // Given - create and terminate a member
        Member member = aMember()
                .withRegistrationNumber("ZBM0501")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail("jan@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();

        UserId adminUserId = new UserId(UUID.randomUUID());
        member.handle(new Member.TerminateMembership(
                adminUserId,
                DeactivationReason.ODHLASKA,
                "Termination note"
        ));

        UserId reactivatedBy = new UserId(UUID.randomUUID());
        Member.ReactivateMembership command = new Member.ReactivateMembership(reactivatedBy);

        // When
        MemberReactivatedEvent event = MemberReactivatedEvent.fromMember(member, command);

        // Then
        assertThat(event.getMemberId()).isEqualTo(member.getId());
        assertThat(event.getRegistrationNumber()).isEqualTo(member.getRegistrationNumber());
        assertThat(event.getReactivatedBy()).isEqualTo(reactivatedBy);
        assertThat(event.getReactivatedAt()).isNotNull();
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("should validate required fields")
    void shouldValidateRequiredFields() {
        // Given
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId reactivatedBy = new UserId(UUID.randomUUID());

        // When & Then - missing eventId
        assertThatThrownBy(() -> new MemberReactivatedEvent(
                null,
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Event ID");

        // When & Then - missing memberId
        assertThatThrownBy(() -> new MemberReactivatedEvent(
                UUID.randomUUID(),
                null,
                registrationNumber,
                now,
                reactivatedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Member ID");

        // When & Then - missing registrationNumber
        assertThatThrownBy(() -> new MemberReactivatedEvent(
                UUID.randomUUID(),
                memberId,
                null,
                now,
                reactivatedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Registration number");

        // When & Then - missing reactivatedAt
        assertThatThrownBy(() -> new MemberReactivatedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                null,
                reactivatedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Reactivated at");

        // When & Then - missing reactivatedBy
        assertThatThrownBy(() -> new MemberReactivatedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                now,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Reactivated by");
    }

    @Test
    @DisplayName("should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        // Given
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId reactivatedBy = new UserId(UUID.randomUUID());

        MemberReactivatedEvent event = new MemberReactivatedEvent(
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        );

        // When
        String toString = event.toString();

        // Then
        assertThat(toString).contains("MemberReactivatedEvent");
        assertThat(toString).contains("eventId=");
        assertThat(toString).contains("memberId=");
        assertThat(toString).contains("ZBM0501");
        assertThat(toString).contains("reactivatedAt=");
        assertThat(toString).contains("reactivatedBy=");
    }

    @Test
    @DisplayName("should implement equals and hashCode based on eventId")
    void shouldImplementEqualsAndHashCodeBasedOnEventId() {
        // Given
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId reactivatedBy = new UserId(UUID.randomUUID());
        UUID eventId = UUID.randomUUID();

        MemberReactivatedEvent event1 = new MemberReactivatedEvent(
                eventId,
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        );

        MemberReactivatedEvent event2 = new MemberReactivatedEvent(
                eventId,
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        );

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    @DisplayName("should not be equal with different eventId")
    void shouldNotBeEqualWithDifferentEventId() {
        // Given
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId reactivatedBy = new UserId(UUID.randomUUID());

        MemberReactivatedEvent event1 = new MemberReactivatedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        );

        MemberReactivatedEvent event2 = new MemberReactivatedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                now,
                reactivatedBy
        );

        // Then
        assertThat(event1).isNotEqualTo(event2);
    }
}
