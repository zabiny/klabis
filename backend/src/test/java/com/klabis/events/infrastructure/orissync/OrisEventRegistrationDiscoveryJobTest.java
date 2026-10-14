package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventEntryBuilder;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventRegistrationDiscoveryJob")
class OrisEventRegistrationDiscoveryJobTest {

    private static final int ORIS_EVENT_ID = 8123;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private OrisEventEntries orisEventEntries;
    @Mock
    private SyncPairingLookup pairingLookup;
    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisEventRegistrationDiscoveryJob job;

    @BeforeEach
    void setUp() {
        job = new OrisEventRegistrationDiscoveryJob(eventRepository, orisEventEntries, pairingLookup,
                synchronizationPort);
    }

    private Event activeEvent() {
        return EventTestDataBuilder.anEvent().withDate(LocalDate.now().plusDays(30)).buildPublished();
    }

    private void givenOrisEvent(Event event, int orisEventId) {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, event.getId().value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisEventId)));
        when(pairingLookup.findActiveByEntityType(SyncEntityType.EVENT)).thenReturn(List.of(record));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    }

    private static EventEntry entry(Integer clubUserId) {
        return EventEntryBuilder.builder().id(1).clubUserId(clubUserId).build();
    }

    private void givenMemberPairings(int... clubUserIds) {
        List<SyncedEntityReference> references = java.util.Arrays.stream(clubUserIds)
                .mapToObj(id -> new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.MEMBER, "member-" + id),
                        new ExternalReference(ExternalSystem.ORIS, String.valueOf(id))))
                .toList();
        when(pairingLookup.findByExternalReferences(eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS),
                anyCollection())).thenReturn(references);
    }

    private void givenAlreadyPairedRegistrations(String... externalIds) {
        List<SyncedEntityReference> references = java.util.Arrays.stream(externalIds)
                .map(id -> new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.EVENT_REGISTRATION, "e:m"),
                        new ExternalReference(ExternalSystem.ORIS, id)))
                .toList();
        when(pairingLookup.findByExternalReferences(eq(SyncEntityType.EVENT_REGISTRATION), eq(ExternalSystem.ORIS),
                anyCollection())).thenReturn(references);
    }

    private static ExternalReference registrationRef(int clubUserId) {
        return new ExternalReference(ExternalSystem.ORIS, ORIS_EVENT_ID + ":" + clubUserId);
    }

    @Test
    @DisplayName("enrols entries of members paired as MEMBER/ORIS")
    void enrolsEntriesOfPairedMembers() {
        givenOrisEvent(activeEvent(), ORIS_EVENT_ID);
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11), entry(12)));
        givenMemberPairings(11, 12);
        givenAlreadyPairedRegistrations();

        job.discover();

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), null);
        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(12), null);
    }

    @Test
    @DisplayName("ignores entries of people unknown to Klabis")
    void ignoresUnknownMembers() {
        givenOrisEvent(activeEvent(), ORIS_EVENT_ID);
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11), entry(99)));
        givenMemberPairings(11);
        givenAlreadyPairedRegistrations();

        job.discover();

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), null);
        verify(synchronizationPort, never()).pullAndEnroll(any(), eq(registrationRef(99)), any());
    }

    @Test
    @DisplayName("skips entries without a ClubUserID")
    void skipsEntriesWithoutClubUserId() {
        givenOrisEvent(activeEvent(), ORIS_EVENT_ID);
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(null), entry(11)));
        givenMemberPairings(11);
        givenAlreadyPairedRegistrations();

        job.discover();

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), null);
        verify(synchronizationPort, never()).pullAndEnroll(any(), eq(new ExternalReference(
                ExternalSystem.ORIS, ORIS_EVENT_ID + ":null")), any());
    }

    @Test
    @DisplayName("does not pass registrations that are already paired")
    void subtractsPairedRegistrations() {
        givenOrisEvent(activeEvent(), ORIS_EVENT_ID);
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11), entry(12)));
        givenMemberPairings(11, 12);
        givenAlreadyPairedRegistrations(registrationRef(11).externalId());

        job.discover();

        verify(synchronizationPort, never()).pullAndEnroll(any(), eq(registrationRef(11)), any());
        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(12), null);
    }

    @Test
    @DisplayName("does not read ORIS for DRAFT events")
    void skipsDraftEvents() {
        Event draft = EventTestDataBuilder.anEvent().withDate(LocalDate.now().plusDays(30)).build();
        givenOrisEvent(draft, ORIS_EVENT_ID);

        job.discover();

        verify(orisEventEntries, never()).entriesOf(ORIS_EVENT_ID);
        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("one failing entry does not stop the others")
    void failureOfOneEntryIsIsolated() {
        givenOrisEvent(activeEvent(), ORIS_EVENT_ID);
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11), entry(12)));
        givenMemberPairings(11, 12);
        givenAlreadyPairedRegistrations();
        when(synchronizationPort.pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), null))
                .thenThrow(new IllegalStateException("boom"));

        job.discover();

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(12), null);
    }

    @Test
    @DisplayName("one failing event does not stop the other events")
    void failureOfOneEventIsIsolated() {
        Event failing = activeEvent();
        Event healthy = activeEvent();
        SyncRecord failingRecord = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, failing.getId().value().toString()),
                new ExternalReference(ExternalSystem.ORIS, "1"));
        SyncRecord healthyRecord = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, healthy.getId().value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(ORIS_EVENT_ID)));
        when(pairingLookup.findActiveByEntityType(SyncEntityType.EVENT))
                .thenReturn(List.of(failingRecord, healthyRecord));
        when(eventRepository.findById(failing.getId())).thenReturn(Optional.of(failing));
        when(eventRepository.findById(healthy.getId())).thenReturn(Optional.of(healthy));
        when(orisEventEntries.entriesOf(1)).thenThrow(new IllegalStateException("ORIS down"));
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11)));
        givenMemberPairings(11);
        givenAlreadyPairedRegistrations();

        job.discover();

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), null);
    }

    @Test
    @DisplayName("on-demand discovery of one event carries the acting user")
    void discoverForEventCarriesActingUser() {
        Event event = activeEvent();
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, event.getId().value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(ORIS_EVENT_ID)));
        when(pairingLookup.findActiveByTargets(SyncEntityType.EVENT, List.of(event.getId().value().toString())))
                .thenReturn(List.of(new SyncedEntityReference(record.getTarget(), record.getExternalReference())));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry(11)));
        givenMemberPairings(11);
        givenAlreadyPairedRegistrations();

        job.discoverForEvent(event.getId(), "admin");

        verify(orisEventEntries).evict(ORIS_EVENT_ID);
        verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT_REGISTRATION, registrationRef(11), "admin");
    }
}
