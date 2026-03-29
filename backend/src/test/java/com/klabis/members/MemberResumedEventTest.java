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
        member.handle(MemberSuspendMembershipBuilder.builder()
                .suspendedBy(adminUserId)
                .reason(DeactivationReason.ODHLASKA)
                .note("Suspension note")
                .build());

        UserId resumedBy = new UserId(UUID.randomUUID());
        Member.ResumeMembership command = MemberResumeMembershipBuilder.builder()
                .resumedBy(resumedBy).build();

        MemberResumedEvent event = MemberResumedEvent.fromAggregate(member, command);

        assertThat(event.memberId()).isEqualTo(member.getId());
        assertThat(event.registrationNumber()).isEqualTo(member.getRegistrationNumber());
        assertThat(event.resumedBy()).isEqualTo(resumedBy);
        assertThat(event.resumedAt()).isNotNull();
        assertThat(event.eventId()).isNotNull();
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
    @DisplayName("should have toString with event details")
    void shouldHaveToStringWithEventDetails() {
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

        // Verify key event details are present
        assertThat(toString).contains("MemberResumedEvent");
        assertThat(toString).contains("eventId=");
        assertThat(toString).contains("ZBM0501");
        assertThat(toString).contains("resumedAt=");
        assertThat(toString).contains("resumedBy=");
    }
}
