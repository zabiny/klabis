package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.autoconfigure.OrisClientProperties;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventEntryBuilder;
import com.klabis.events.EventCategory;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.OrisCategoryNotYetSynchronisedException;
import com.klabis.events.domain.RegistrationNote;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.RetryableSyncFailureException;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventRegistrationSyncAdapter")
class OrisEventRegistrationSyncAdapterTest {

    private static final int ORIS_EVENT_ID = 8123;
    private static final int CLUB_USER_ID = 777;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private OrisEventEntries orisEventEntries;
    @Mock
    private SyncPairingLookup pairingLookup;
    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisEventRegistrationSyncAdapter adapter;

    private Event event;
    private final MemberId memberId = new MemberId(UUID.randomUUID());

    private String entityId() {
        return event.getId().value() + ":" + memberId.value();
    }

    private static String externalId() {
        return ORIS_EVENT_ID + ":" + CLUB_USER_ID;
    }

    private static EventEntryBuilder entry() {
        return EventEntryBuilder.builder().id(1).clubUserId(CLUB_USER_ID).classId(101).si(123456).rentSi(true)
                .note("hello").createdDateTime(LocalDateTime.of(2026, 3, 1, 12, 0, 0));
    }

    @BeforeEach
    void setUp() {
        OrisClientProperties orisProperties = new OrisClientProperties(true, URI.create("https://oris.example"),
                URI.create("https://oris.example"), ZoneId.of("Europe/Prague"), "yyyy-MM-dd HH:mm:ss", 205);
        adapter = new OrisEventRegistrationSyncAdapter(eventRepository, orisEventEntries, pairingLookup,
                orisProperties, synchronizationPort);
        event = EventTestDataBuilder.anEvent()
                .withDate(LocalDate.now().plusDays(30))
                .withCategories(List.of(EventCategory.createFromOris("101", "M21"),
                        EventCategory.createFromOris("102", "W21")))
                .buildPublished();
    }

    private void pairingsResolve() {
        when(pairingLookup.findByExternalReferences(eq(SyncEntityType.EVENT), eq(ExternalSystem.ORIS), anyList()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.EVENT, event.getId().value().toString()),
                        new ExternalReference(ExternalSystem.ORIS, String.valueOf(ORIS_EVENT_ID)))));
        when(pairingLookup.findByExternalReferences(eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS), anyList()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.MEMBER, memberId.value().toString()),
                        new ExternalReference(ExternalSystem.ORIS, String.valueOf(CLUB_USER_ID)))));
    }

    private OrisEventRegistrationProjection readExternalOf(EventEntry... entries) {
        pairingsResolve();
        when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entries));
        return (OrisEventRegistrationProjection) adapter.readExternal(externalId());
    }

    @Test
    @DisplayName("declares pull-only-creating capabilities with sensitive data")
    void declaresCapabilities() {
        var capabilities = adapter.capabilities();

        assertThat(adapter.entityType()).isEqualTo(SyncEntityType.EVENT_REGISTRATION);
        assertThat(adapter.system()).isEqualTo(ExternalSystem.ORIS);
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isTrue();
        assertThat(capabilities.createsExternal()).isFalse();
        assertThat(capabilities.containsSensitiveData()).isTrue();
        assertThat(adapter.projectionType()).isEqualTo(OrisEventRegistrationProjection.class);
    }

    @Nested
    @DisplayName("readLocal")
    class ReadLocal {

        @Test
        @DisplayName("is absent when the member has no registration")
        void absentWithoutRegistration() {
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThat(adapter.readLocal(entityId())).isEqualTo(OrisEventRegistrationProjection.absent());
        }

        @Test
        @DisplayName("projects category by ORIS id, SI card, rental and ORIS note, not the internal note")
        void projectsRegistration() {
            event.syncRegistrationFromOris(memberId, new Event.SyncRegistrationFromOris("102",
                    SiCardNumber.of("123456"), true, RegistrationNote.ofNullable("hello"), Instant.now()));
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            OrisEventRegistrationProjection projection = (OrisEventRegistrationProjection) adapter.readLocal(entityId());

            assertThat(projection.present()).isTrue();
            assertThat(projection.categoryOrisId()).isEqualTo("102");
            assertThat(projection.siCardNumber()).isEqualTo("123456");
            assertThat(projection.wantsToRentSi()).isTrue();
            assertThat(projection.orisNote()).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("readExternal")
    class ReadExternal {

        @Test
        @DisplayName("takes the member's entry and carries what createLocal needs")
        void projectsEntry() {
            OrisEventRegistrationProjection projection = readExternalOf(entry().build());

            assertThat(projection.present()).isTrue();
            assertThat(projection.categoryOrisId()).isEqualTo("101");
            assertThat(projection.siCardNumber()).isEqualTo("123456");
            assertThat(projection.wantsToRentSi()).isTrue();
            assertThat(projection.orisNote()).isEqualTo("hello");
            assertThat(projection.eventId()).isEqualTo(event.getId());
            assertThat(projection.memberId()).isEqualTo(memberId);
        }

        @Test
        @DisplayName("converts the ORIS local creation time with the configured timezone")
        void convertsCreationTime() {
            OrisEventRegistrationProjection projection = readExternalOf(entry().build());

            assertThat(projection.registeredAt()).isEqualTo(Instant.parse("2026-03-01T11:00:00Z"));
        }

        @Test
        @DisplayName("is absent when ORIS lists no entry of that person")
        void absentWithoutEntry() {
            when(orisEventEntries.entriesOf(ORIS_EVENT_ID)).thenReturn(List.of(entry().clubUserId(999).build()));

            assertThat(adapter.readExternal(externalId())).isEqualTo(OrisEventRegistrationProjection.absent());
        }

        @Test
        @DisplayName("normalises SI 0 to no card")
        void siZero() {
            assertThat(readExternalOf(entry().si(0).build()).siCardNumber()).isNull();
        }

        @Test
        @DisplayName("normalises a missing SI to no card")
        void siNull() {
            assertThat(readExternalOf(entry().si(null).build()).siCardNumber()).isNull();
        }

        @Test
        @DisplayName("normalises an SI that is not 4-8 digits to no card")
        void siOutOfRange() {
            assertThat(readExternalOf(entry().si(123).build()).siCardNumber()).isNull();
            assertThat(readExternalOf(entry().si(123456789).build()).siCardNumber()).isNull();
        }

        @Test
        @DisplayName("normalises a null RentSI to false")
        void rentNull() {
            assertThat(readExternalOf(entry().rentSi(null).build()).wantsToRentSi()).isFalse();
        }

        @Test
        @DisplayName("normalises a blank note to no note and trims")
        void blankNote() {
            assertThat(readExternalOf(entry().note("   ").build()).orisNote()).isNull();
            assertThat(readExternalOf(entry().note("  x  ").build()).orisNote()).isEqualTo("x");
        }

        @Test
        @DisplayName("truncates an over-long note to the 255 characters Klabis stores")
        void longNote() {
            String note = readExternalOf(entry().note("a".repeat(300)).build()).orisNote();

            assertThat(note).hasSize(255);
        }

        @Test
        @DisplayName("wraps a non-OK ORIS answer as retryable")
        void orisErrorIsRetryable() {
            when(orisEventEntries.entriesOf(ORIS_EVENT_ID))
                    .thenThrow(new IllegalStateException("ORIS API returned error response with status ERROR"));

            assertThatThrownBy(() -> adapter.readExternal(externalId()))
                    .isInstanceOf(RetryableSyncFailureException.class);
        }
    }

    @Nested
    @DisplayName("createLocal")
    class CreateLocal {

        @Test
        @DisplayName("registers the member from the ORIS projection and returns the participation id")
        void createsRegistration() {
            OrisEventRegistrationProjection projection = readExternalOf(entry().build());
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            String id = adapter.createLocal(projection);

            assertThat(id).isEqualTo(entityId());
            verify(eventRepository).save(event);
            EventRegistration registration = event.findRegistration(memberId).orElseThrow();
            assertThat(registration.categoryId()).isEqualTo(event.getCategories().get(0).id());
            assertThat(registration.registeredAt()).isEqualTo(Instant.parse("2026-03-01T11:00:00Z"));
        }

        @Test
        @DisplayName("is idempotent with the next read: local and external projections hash equal")
        void secondPassIsEqual() {
            OrisEventRegistrationProjection external = readExternalOf(
                    entry().si(0).rentSi(null).note("x".repeat(400)).build());
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            adapter.createLocal(external);
            assertThat(SyncProjectionCodec.hash(adapter.readLocal(entityId())))
                    .isEqualTo(SyncProjectionCodec.hash(external));
        }

        @Test
        @DisplayName("marks the EVENT record dirty and fails retryable when the category is not synchronised yet")
        void unknownCategory() {
            OrisEventRegistrationProjection projection = readExternalOf(entry().classId(999).build());
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> adapter.createLocal(projection))
                    .isInstanceOf(OrisCategoryNotYetSynchronisedException.class);

            verify(synchronizationPort).markDirty(
                    new SyncTarget(SyncEntityType.EVENT, event.getId().value().toString()));
        }

        @Test
        @DisplayName("wraps an optimistic locking failure as retryable")
        void optimisticLock() {
            OrisEventRegistrationProjection projection = readExternalOf(entry().build());
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenThrow(new OptimisticLockingFailureException("stale"));

            assertThatThrownBy(() -> adapter.createLocal(projection))
                    .isInstanceOf(RetryableSyncFailureException.class);
        }
    }

    @Test
    @DisplayName("applyToLocal for a present projection registers the member")
    void applyToLocalPresent() {
        OrisEventRegistrationProjection projection = readExternalOf(entry().build());
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        adapter.applyToLocal(entityId(), projection);

        assertThat(event.findRegistration(memberId)).isPresent();
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("applyToExternal is unsupported")
    void applyToExternalUnsupported() {
        assertThatThrownBy(() -> adapter.applyToExternal(externalId(), OrisEventRegistrationProjection.absent()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
