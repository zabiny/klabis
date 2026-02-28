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

@DisplayName("MemberResumedEvent Tests")
class MemberResumedEventTest {

    @Test
    @DisplayName("should create event from Member aggregate and command")
    void shouldCreateEventFromMember() {
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
        member.handle(new Member.SuspendMembership(
                adminUserId,
                DeactivationReason.ODHLASKA,
                "Suspension note"
        ));

        UserId resumedBy = new UserId(UUID.randomUUID());
        Member.ResumeMembership command = new Member.ResumeMembership(resumedBy);

        MemberResumedEvent event = MemberResumedEvent.fromMember(member, command);

        assertThat(event.getMemberId()).isEqualTo(member.getId());
        assertThat(event.getRegistrationNumber()).isEqualTo(member.getRegistrationNumber());
        assertThat(event.getResumedBy()).isEqualTo(resumedBy);
        assertThat(event.getResumedAt()).isNotNull();
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("should validate required fields")
    void shouldValidateRequiredFields() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId resumedBy = new UserId(UUID.randomUUID());

        assertThatThrownBy(() -> new MemberResumedEvent(
                null,
                memberId,
                registrationNumber,
                now,
                resumedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Event ID");

        assertThatThrownBy(() -> new MemberResumedEvent(
                UUID.randomUUID(),
                null,
                registrationNumber,
                now,
                resumedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Member ID");

        assertThatThrownBy(() -> new MemberResumedEvent(
                UUID.randomUUID(),
                memberId,
                null,
                now,
                resumedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Registration number");

        assertThatThrownBy(() -> new MemberResumedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                null,
                resumedBy
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Resumed at");

        assertThatThrownBy(() -> new MemberResumedEvent(
                UUID.randomUUID(),
                memberId,
                registrationNumber,
                now,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Resumed by");
    }

    @Test
    @DisplayName("should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId resumedBy = new UserId(UUID.randomUUID());

        MemberResumedEvent event = new MemberResumedEvent(
                memberId,
                registrationNumber,
                now,
                resumedBy
        );

        String toString = event.toString();

        assertThat(toString).contains("MemberResumedEvent");
        assertThat(toString).contains("eventId=");
        assertThat(toString).contains("memberId=");
        assertThat(toString).contains("ZBM0501");
        assertThat(toString).contains("resumedAt=");
        assertThat(toString).contains("resumedBy=");
    }

    @Test
    @DisplayName("should implement equals and hashCode based on eventId")
    void shouldImplementEqualsAndHashCodeBasedOnEventId() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId resumedBy = new UserId(UUID.randomUUID());
        UUID eventId = UUID.randomUUID();

        MemberResumedEvent event1 = new MemberResumedEvent(
                eventId, memberId, registrationNumber, now, resumedBy
        );

        MemberResumedEvent event2 = new MemberResumedEvent(
                eventId, memberId, registrationNumber, now, resumedBy
        );

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    @DisplayName("should not be equal with different eventId")
    void shouldNotBeEqualWithDifferentEventId() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
        Instant now = Instant.now();
        UserId resumedBy = new UserId(UUID.randomUUID());

        MemberResumedEvent event1 = new MemberResumedEvent(
                UUID.randomUUID(), memberId, registrationNumber, now, resumedBy
        );

        MemberResumedEvent event2 = new MemberResumedEvent(
                UUID.randomUUID(), memberId, registrationNumber, now, resumedBy
        );

        assertThat(event1).isNotEqualTo(event2);
    }
}
