package com.klabis.members;

import com.klabis.members.domain.Member;
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
        Member member = aMember()
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .build();

        MemberCreatedEvent event = MemberCreatedEvent.fromAggregate(member);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.memberId()).isEqualTo(member.getId());
        assertThat(event.dateOfBirth()).isEqualTo(LocalDate.of(2005, 6, 15));
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("should validate required fields")
    void shouldValidateRequiredFields() {
        assertThatThrownBy(() -> new MemberCreatedEvent(null, LocalDate.of(2005, 6, 15)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Member ID");
    }
}
