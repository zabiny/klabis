package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventId;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.RegistrationSyncId;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.events.EventTestDataBuilder.anEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Events module sync entity describers")
class SyncEntityDescribersTest {

    private final EventManagementPort eventManagement = mock(EventManagementPort.class);
    private final DisciplineManagementPort disciplineManagement = mock(DisciplineManagementPort.class);
    private final Members members = mock(Members.class);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a",
                        List.of(new SimpleGrantedAuthority(Authority.EVENTS_READ.getValue()),
                                new SimpleGrantedAuthority(Authority.EVENTS_MANAGE.getValue()))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Event springCup() {
        return anEvent().withName("Spring Cup").withDate(LocalDate.of(2026, 5, 3)).build();
    }

    @Test
    @DisplayName("event: name with date and link to the event")
    void describesEvent() {
        Event event = springCup();
        when(eventManagement.getEvent(event.getId(), true)).thenReturn(event);

        var description = new EventSyncEntityDescriber(eventManagement).describe(event.getId().value().toString());

        assertThat(description).isPresent();
        assertThat(description.get().label()).isEqualTo("Spring Cup (3. 5. 2026)");
        assertThat(description.get().entityLink().getHref()).endsWith("/api/events/" + event.getId().value());
    }

    @Test
    @DisplayName("event: empty when it no longer exists")
    void eventMissing() {
        when(eventManagement.getEvent(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new EventNotFoundException(EventId.generate()));

        assertThat(new EventSyncEntityDescriber(eventManagement).describe(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    @DisplayName("discipline: name and link to the discipline list")
    void describesDiscipline() {
        Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("SP", "Sprint"));
        when(disciplineManagement.get(discipline.getId())).thenReturn(discipline);

        var description = new DisciplineSyncEntityDescriber(disciplineManagement)
                .describe(discipline.getId().value().toString());

        assertThat(description).isPresent();
        assertThat(description.get().label()).isEqualTo("Sprint");
        assertThat(description.get().entityLink().getHref()).endsWith("/api/disciplines");
    }

    @Test
    @DisplayName("discipline: empty when it no longer exists")
    void disciplineMissing() {
        when(disciplineManagement.get(any())).thenThrow(new DisciplineNotFoundException(DisciplineId.generate()));

        assertThat(new DisciplineSyncEntityDescriber(disciplineManagement)
                .describe(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    @DisplayName("registration: member name and event name, link to the event")
    void describesRegistration() {
        Event event = springCup();
        MemberId memberId = new MemberId(UUID.randomUUID());
        when(eventManagement.getEvent(event.getId(), true)).thenReturn(event);
        when(members.findById(memberId)).thenReturn(Optional.of(
                new MemberDto(memberId.value(), "Jan", "Novák", "jan@example.com", "ZBM9500", LocalDateTime.now(), null)));

        var description = new RegistrationSyncEntityDescriber(eventManagement, members)
                .describe(new RegistrationSyncId(event.getId(), memberId).format());

        assertThat(description).isPresent();
        assertThat(description.get().label()).isEqualTo("Jan Novák – Spring Cup");
        assertThat(description.get().entityLink().getHref()).endsWith("/api/events/" + event.getId().value());
    }

    @Test
    @DisplayName("registration: empty when the member or the event is gone")
    void registrationMissing() {
        Event event = springCup();
        MemberId memberId = new MemberId(UUID.randomUUID());
        String entityId = new RegistrationSyncId(event.getId(), memberId).format();
        var describer = new RegistrationSyncEntityDescriber(eventManagement, members);

        when(members.findById(memberId)).thenReturn(Optional.empty());
        assertThat(describer.describe(entityId)).isEmpty();

        when(members.findById(memberId)).thenReturn(Optional.of(
                new MemberDto(memberId.value(), "Jan", "Novák", "jan@example.com", "ZBM9500", LocalDateTime.now(), null)));
        when(eventManagement.getEvent(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new EventNotFoundException(event.getId()));
        assertThat(describer.describe(entityId)).isEmpty();
    }
}
