package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventClass;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncParty;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalEventSyncSource")
class LocalEventSyncSourceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    private OrisEventMappingSupport support;
    private LocalEventSyncSource testedInstance;

    @BeforeEach
    void setUp() {
        support = spy(new OrisEventMappingSupport(eventTypeRepository));
        OrisEventDetailsMapper mapper = Mappers.getMapper(OrisEventDetailsMapper.class);
        testedInstance = new LocalEventSyncSource(eventRepository, mapper, support);
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
        EventSyncData data = new EventSyncData(null, orisId, details(orisId, "Spring Sprint"),
                "https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncId result = testedInstance.save(data);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getOrisId()).isEqualTo(orisId);
        assertThat(saved.getValue().getName()).isEqualTo("Spring Sprint");

        assertThat(result.type()).isEqualTo(SyncType.EVENT);
        assertThat(result.party()).isEqualTo(SyncParty.LOCAL);
        assertThat(result.idValue()).isEqualTo(saved.getValue().getId().value().toString());
    }

    @Test
    @DisplayName("save() updates the existing event resolved via findByOrisId when eventId is null")
    void save_updateBranch() {
        int orisId = 4242;
        Event existing = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name("Old Name")
                .eventDate(LocalDate.of(2026, 8, 1))
                .location("Old Location")
                .organizer("OLD")
                .build());
        EventSyncData data = new EventSyncData(null, orisId, details(orisId, "New Name from ORIS"),
                "https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncId result = testedInstance.save(data);

        verify(eventRepository, never()).findById(any(EventId.class));
        assertThat(existing.getName()).isEqualTo("New Name from ORIS");
        assertThat(result.idValue()).isEqualTo(existing.getId().value().toString());
    }

    @Test
    @DisplayName("save() applies the auto-mapped event type on the create branch")
    void save_appliesAutoMappedEventTypeOnCreate() {
        int orisId = 555;
        EventTypeId typeId = EventTypeId.of(UUID.randomUUID());
        EventType eventType = Mockito.mock(EventType.class);
        when(eventType.getId()).thenReturn(typeId);
        when(eventTypeRepository.findByOrisDisciplineId(3)).thenReturn(Optional.of(eventType));
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDetails details = details(orisId, "Typed Race");
        when(details.discipline()).thenReturn(new Discipline(3, "OB", "OB", "OB"));

        testedInstance.save(new EventSyncData(null, orisId, details, "https://oris.ceskyorientak.cz/Zavod?id=" + orisId));

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventTypeId()).contains(typeId);
    }

    @Test
    @DisplayName("save() applies the auto-mapped event type on the update branch")
    void save_appliesAutoMappedEventTypeOnUpdate() {
        int orisId = 666;
        EventTypeId typeId = EventTypeId.of(UUID.randomUUID());
        EventType eventType = Mockito.mock(EventType.class);
        when(eventType.getId()).thenReturn(typeId);
        when(eventTypeRepository.findByOrisDisciplineId(4)).thenReturn(Optional.of(eventType));

        Event existing = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name("Old")
                .eventDate(LocalDate.of(2026, 8, 1))
                .location("Loc")
                .organizer("OOB")
                .build());
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDetails details = details(orisId, "Updated");
        when(details.discipline()).thenReturn(new Discipline(4, "OB", "OB", "OB"));

        testedInstance.save(new EventSyncData(null, orisId, details, "https://oris.ceskyorientak.cz/Zavod?id=" + orisId));

        assertThat(existing.getEventTypeId()).contains(typeId);
    }

    @Test
    @DisplayName("save() does not overwrite an existing event type on the update branch")
    void save_doesNotOverwriteExistingEventTypeOnUpdate() {
        int orisId = 777;
        EventTypeId existingTypeId = EventTypeId.of(UUID.randomUUID());
        EventTypeId disciplineMatchTypeId = EventTypeId.of(UUID.randomUUID());
        EventType disciplineMatch = Mockito.mock(EventType.class);
        Mockito.lenient().when(disciplineMatch.getId()).thenReturn(disciplineMatchTypeId);
        Mockito.lenient().when(eventTypeRepository.findByOrisDisciplineId(5))
                .thenReturn(Optional.of(disciplineMatch));

        Event existing = com.klabis.events.EventTestDataBuilder.anEvent()
                .withOrisId(orisId)
                .withName("Old")
                .withEventTypeId(existingTypeId)
                .build();
        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDetails details = details(orisId, "Updated");
        when(details.discipline()).thenReturn(new Discipline(5, "OB", "OB", "OB"));

        testedInstance.save(new EventSyncData(null, orisId, details, "https://oris.ceskyorientak.cz/Zavod?id=" + orisId));

        assertThat(existing.getEventTypeId()).contains(existingTypeId);
    }

    @Test
    @DisplayName("save() maps a DataIntegrityViolationException from the repository to DuplicateOrisImportException")
    void save_mapsUniqueConstraintViolation() {
        int orisId = 8080;
        EventSyncData data = new EventSyncData(null, orisId, details(orisId, "Clashing Race"),
                "https://oris.ceskyorientak.cz/Zavod?id=" + orisId);

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
    @DisplayName("save() warns when the incoming ORIS classes drop a category that has registrations")
    void save_warnsWhenSyncRemovesCategoryWithRegistrations() {
        int orisId = 9090;
        LocalDate eventDate = LocalDate.now().plusDays(30);
        EventCategory m21 = EventCategory.createFromOris("100", "M21");
        EventCategory w21 = EventCategory.createFromOris("200", "W21");
        Event existing = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name("Race")
                .eventDate(eventDate)
                .location("Forest")
                .organizer("OOB")
                .categories(List.of(m21, w21))
                .build());
        existing.publish();
        existing.registerMember(new MemberId(UUID.randomUUID()), new SiCardNumber("12345"), m21.id());

        EventClass w21Class = mockClass("200", "W21");
        EventDetails details = details(orisId, "Race Updated");
        when(details.classes()).thenReturn(Map.of("W21", w21Class));

        when(eventRepository.findByOrisId(orisId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        testedInstance.save(new EventSyncData(null, orisId, details,
                "https://oris.ceskyorientak.cz/Zavod?id=" + orisId));

        verify(support).warnIfSyncRemovesCategoriesWithRegistrations(eq(existing), anyList());
        assertThat(existing.getCategories()).extracting(EventCategory::name).containsExactly("W21");
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
                SyncId.localId(SyncType.EVENT, event.getId().value().toString()));

        assertThat(result).isPresent();
        assertThat(result.get().eventId()).isEqualTo(event.getId());
        assertThat(result.get().orisId()).isEqualTo(1234);
        assertThat(result.get().orisDetails()).isNull();
    }

    private EventClass mockClass(String orisClassId, String name) {
        EventClass cls = Mockito.mock(EventClass.class);
        Mockito.lenient().when(cls.id()).thenReturn(orisClassId);
        Mockito.lenient().when(cls.name()).thenReturn(name);
        return cls;
    }

    private EventDetails details(int orisId, String name) {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.lenient().when(details.name()).thenReturn(name);
        Mockito.lenient().when(details.date()).thenReturn(LocalDate.of(2026, 8, 15));
        Mockito.lenient().when(details.place()).thenReturn("Somewhere");
        Mockito.lenient().when(details.currency()).thenReturn(null);
        Mockito.lenient().when(details.org1()).thenReturn(new Organizer(205, "OOB", "Orel Brno"));
        Mockito.lenient().when(details.org2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate1()).thenReturn(null);
        Mockito.lenient().when(details.entryDate2()).thenReturn(null);
        Mockito.lenient().when(details.entryDate3()).thenReturn(null);
        Mockito.lenient().when(details.classes()).thenReturn(null);
        Mockito.lenient().when(details.level()).thenReturn(null);
        Mockito.lenient().when(details.discipline()).thenReturn(null);
        return details;
    }
}
