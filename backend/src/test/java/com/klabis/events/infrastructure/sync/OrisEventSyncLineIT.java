package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.DataSyncImpl;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncLine;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncRecordRepository;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Integration test for the ORIS event {@link SyncLine} driven through a hand-assembled
 * {@link DataSyncImpl}: a real {@link LocalEventSyncSource} + real {@link OrisEventSyncSource}
 * (with a mocked {@link OrisApiClient}) + an in-memory {@link EventRepository} + a fake
 * {@link SyncRecordRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ORIS event SyncLine (integration)")
class OrisEventSyncLineIT {

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private OrisWebUrls orisWebUrls;

    @Mock
    private EventTypeRepository eventTypeRepository;

    private EventRepository eventRepository;
    private DataSync dataSync;

    @BeforeEach
    void setUp() {
        eventRepository = inMemoryEventRepository();

        OrisEventMappingSupport support = new OrisEventMappingSupport(eventTypeRepository);
        LocalEventSyncSource local = new LocalEventSyncSource(
                eventRepository, Mappers.getMapper(OrisEventDetailsMapper.class), support);
        OrisEventSyncSource oris = new OrisEventSyncSource(orisApiClient, orisWebUrls);
        IdentityEventConverter identity = new IdentityEventConverter();
        SyncLine<EventSyncData, EventSyncData> syncLine = new SyncLine<>(local, oris, identity, identity);

        dataSync = new DataSyncImpl(List.of(syncLine), new InMemoryFakeSyncRecordRepository());

        Mockito.lenient().when(orisWebUrls.eventUrl(anyInt()))
                .thenAnswer(inv -> "https://oris.ceskyorientak.cz/Zavod?id=" + inv.getArgument(0));
    }

    @Test
    @DisplayName("PULL with an external id creates the event and returns a SYNCED record; a subsequent PULL with the local id updates it")
    void pullExternalThenLocal() {
        int orisId = 123;
        EventDetails imported = details(orisId, "Imported Race");
        EventDetails resynced = details(orisId, "Resynced Race");
        when(orisApiClient.getEventDetails(orisId))
                .thenReturn(new OrisApiClient.OrisResponse<>(imported, "JSON", "OK", null, "getEvent"))
                .thenReturn(new OrisApiClient.OrisResponse<>(resynced, "JSON", "OK", null, "getEvent"));

        SyncRecord first = dataSync.sync(SyncId.externalId(SyncType.EVENT, "123"), DataSync.Direction.PULL);

        assertThat(first.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(first.localId()).isNotNull();
        assertThat(first.externalId()).isEqualTo(SyncId.externalId(SyncType.EVENT, "123"));

        UUID createdId = UUID.fromString(first.localId().idValue());
        assertThat(eventRepository.findById(new EventId(createdId))).isPresent();
        assertThat(eventRepository.findById(new EventId(createdId)).get().getName()).isEqualTo("Imported Race");

        SyncRecord second = dataSync.sync(first.localId(), DataSync.Direction.PULL);

        assertThat(second.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(second.localId()).isEqualTo(first.localId());
        assertThat(eventRepository.findById(new EventId(createdId)).get().getName()).isEqualTo("Resynced Race");
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

    private EventRepository inMemoryEventRepository() {
        EventRepository repo = Mockito.mock(EventRepository.class);
        Map<UUID, Event> store = new ConcurrentHashMap<>();

        Mockito.lenient().when(repo.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            store.put(e.getId().value(), e);
            return e;
        });
        Mockito.lenient().when(repo.findById(any(EventId.class))).thenAnswer(inv -> {
            EventId id = inv.getArgument(0);
            return Optional.ofNullable(store.get(id.value()));
        });
        Mockito.lenient().when(repo.findByOrisId(anyInt())).thenAnswer(inv -> {
            int orisId = inv.getArgument(0);
            return store.values().stream()
                    .filter(e -> e.getOrisId() != null && e.getOrisId() == orisId)
                    .findFirst();
        });
        return repo;
    }

    private static final class InMemoryFakeSyncRecordRepository implements SyncRecordRepository {
        private final Map<UUID, SyncRecord> records = new ConcurrentHashMap<>();

        @Override
        public Optional<SyncRecord> findById(SyncId id) {
            return records.values().stream()
                    .filter(r -> id.equals(r.localId()) || id.equals(r.externalId()))
                    .findAny();
        }

        @Override
        public SyncRecord save(SyncRecord record) {
            records.put(record.id(), record);
            return record;
        }
    }
}
