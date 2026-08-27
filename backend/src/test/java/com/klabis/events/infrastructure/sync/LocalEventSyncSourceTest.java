package com.klabis.events.infrastructure.sync;

import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.RegistrationDeadlines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalEventSyncSource")
class LocalEventSyncSourceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private CategoryRegistrationGuard categoryGuard;

    private LocalEventSyncSource testedInstance;

    @BeforeEach
    void setUp() {
        testedInstance = new LocalEventSyncSource(eventRepository, categoryGuard);
    }

    private EventSyncData syncData(EventId eventId, int orisId, String name, EventTypeId autoMappedType) {
        return new EventSyncData(
                eventId,
                orisId,
                name,
                LocalDate.of(2026, 8, 15),
                "Somewhere",
                "OOB",
                WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=" + orisId),
                RegistrationDeadlines.of(null, null, null),
                List.of(),
                null,
                null,
                autoMappedType);
    }

    @Test
    @DisplayName("type() is EVENT and party() is LOCAL")
    void identity() {
        assertThat(testedInstance.type()).isEqualTo(SyncType.EVENT);
        assertThat(testedInstance.party()).isEqualTo(SyncParty.LOCAL);
    }

    @Test
    @DisplayName("save() creates a new event when eventId is null and no event matches the orisId")
    void save_createBranch() {
        int orisId = 9876;
        EventSyncData data = syncData(null, orisId, "Spring Sprint", null);

        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncItemId result = testedInstance.save(data);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getOrisId()).isEqualTo(orisId);
        assertThat(saved.getValue().getName()).isEqualTo("Spring Sprint");

        assertThat(result.type()).isEqualTo(SyncType.EVENT);
        assertThat(result.party()).isEqualTo(SyncParty.LOCAL);
        assertThat(result.idValue()).isEqualTo(saved.getValue().getId().value().toString());
    }

    @Test
    @DisplayName("save() updates the existing event resolved via findByOrisId; findById is not called")
    void save_updateBranch() {
        int orisId = 4242;
        Event existing = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name("Old Name")
                .eventDate(LocalDate.of(2026, 8, 1))
                .location("Old Location")
                .organizer("OLD")
                .build());
        EventSyncData data = syncData(null, orisId, "New Name from ORIS", null);

        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncItemId result = testedInstance.save(data);

        verify(eventRepository, never()).findById(any(EventId.class));
        verify(categoryGuard).warnIfSyncRemovesCategoriesWithRegistrations(eq(existing), eq(data.categories()));
        assertThat(existing.getName()).isEqualTo("New Name from ORIS");
        assertThat(result.idValue()).isEqualTo(existing.getId().value().toString());
    }

    @Test
    @DisplayName("save() applies the auto-mapped event type when the event has none")
    void save_appliesAutoMappedEventType() {
        int orisId = 555;
        EventTypeId typeId = EventTypeId.of(UUID.randomUUID());
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        testedInstance.save(syncData(null, orisId, "Typed Race", typeId));

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventTypeId()).contains(typeId);
    }

    @Test
    @DisplayName("save() does not overwrite an existing event type on the update branch")
    void save_doesNotOverwriteExistingEventType() {
        int orisId = 777;
        EventTypeId existingTypeId = EventTypeId.of(UUID.randomUUID());
        EventTypeId incomingTypeId = EventTypeId.of(UUID.randomUUID());

        Event existing = com.klabis.events.EventTestDataBuilder.anEvent()
                .withOrisId(orisId)
                .withName("Old")
                .withEventTypeId(existingTypeId)
                .build();
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        testedInstance.save(syncData(null, orisId, "Updated", incomingTypeId));

        assertThat(existing.getEventTypeId()).contains(existingTypeId);
    }

    @Test
    @DisplayName("save() maps a DataIntegrityViolationException from the repository to DuplicateOrisImportException")
    void save_mapsUniqueConstraintViolation() {
        int orisId = 8080;
        EventSyncData data = syncData(null, orisId, "Clashing Race", null);

        DataIntegrityViolationException dbFailure =
                new DataIntegrityViolationException("unique constraint \"uq_events_oris_id\"");
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenThrow(dbFailure);

        assertThatThrownBy(() -> testedInstance.save(data))
                .isInstanceOf(DuplicateOrisImportException.class)
                .hasCause(dbFailure)
                .hasMessageContaining(Integer.toString(orisId));
    }

    @Test
    @DisplayName("fetch() wraps a found event into EventSyncData carrying its local id and orisId")
    void fetch_wrapsEvent() {
        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(1234)
                .name("Race")
                .eventDate(LocalDate.of(2026, 8, 1))
                .location("Loc")
                .organizer("OOB")
                .build());
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        Optional<EventSyncData> result = testedInstance.fetch(
                SyncItemId.localId(SyncType.EVENT, event.getId().value().toString()));

        assertThat(result).isPresent();
        assertThat(result.get().eventId()).isEqualTo(event.getId());
        assertThat(result.get().orisId()).isEqualTo(1234);
        assertThat(result.get().autoMappedEventType()).isNull();
        assertThat(result.get().name()).isEqualTo("Race");
    }

    @Test
    @DisplayName("fetch() maps an event without an orisId to orisId 0")
    void fetch_eventWithoutOrisId() {
        Event event = com.klabis.events.EventTestDataBuilder.anEvent()
                .withName("Local only")
                .build();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        Optional<EventSyncData> result = testedInstance.fetch(
                SyncItemId.localId(SyncType.EVENT, event.getId().value().toString()));

        assertThat(result).isPresent();
        assertThat(result.get().orisId()).isEqualTo(0);
    }
}
