package com.klabis.events.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventRepository;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventCoordinatorRelationshipSource")
class EventCoordinatorRelationshipSourceTest {

    @Mock
    private EventRepository eventRepository;

    private EventCoordinatorRelationshipSource source() {
        return new EventCoordinatorRelationshipSource(eventRepository);
    }

    @Test
    @DisplayName("grants event editing and registration reading over every coordinated event")
    void shouldGrantBothAuthoritiesOverCoordinatedEvents() {
        UserId userId = new UserId(UUID.randomUUID());
        EventId first = new EventId(UUID.randomUUID());
        EventId second = new EventId(UUID.randomUUID());
        when(eventRepository.findIdsCoordinatedBy(MemberId.fromUserId(userId))).thenReturn(List.of(first, second));

        var grants = source().grantsOf(userId);

        assertThat(grants).containsOnlyKeys(Authority.EVENTS_EDIT, Authority.EVENTS_REGISTRATIONS_READ);
        assertThat(grants.get(Authority.EVENTS_EDIT))
                .containsExactlyInAnyOrder(TargetRef.event(first.value()), TargetRef.event(second.value()));
        assertThat(grants.get(Authority.EVENTS_REGISTRATIONS_READ))
                .containsExactlyInAnyOrder(TargetRef.event(first.value()), TargetRef.event(second.value()));
    }

    @Test
    @DisplayName("grants nothing to a user who coordinates no event")
    void shouldGrantNothingWithoutCoordinatedEvents() {
        UserId userId = new UserId(UUID.randomUUID());
        when(eventRepository.findIdsCoordinatedBy(MemberId.fromUserId(userId))).thenReturn(List.of());

        assertThat(source().grantsOf(userId)).isEmpty();
    }
}
