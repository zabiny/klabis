package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.BulkImportResult;
import com.klabis.events.application.BulkSyncResult;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.application.OrisBulkSyncPort;
import com.klabis.events.application.OrisEventBulkImportPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OrisEventController API tests")
@WebMvcTest(controllers = {EventController.class, OrisEventController.class, EventsExceptionHandler.class})
@ActiveProfiles("oris")
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class OrisEventControllerTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventManagementPort eventManagementService;

    @MockitoBean
    private EventRegistrationPort eventRegistrationService;

    @MockitoBean
    private Members members;

    @MockitoBean
    private OrisEventImportPort orisEventImportPort;

    @MockitoBean
    private OrisEventBulkImportPort orisEventBulkImportPort;

    @MockitoBean
    private OrisBulkSyncPort orisBulkSyncPort;

    @Nested
    @DisplayName("POST /api/events/import")
    class ImportEventTests {

        @Test
        @DisplayName("should return 201 with Location header on successful import")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldImportEventSuccessfully() throws Exception {
            Event importedEvent = EventTestDataBuilder.anEvent().withName("ORIS Sprint Race").build();
            when(orisEventImportPort.importEventFromOris(9876)).thenReturn(importedEvent);

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 409 when ORIS event already imported")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenDuplicate() throws Exception {
            when(orisEventImportPort.importEventFromOris(9876))
                    .thenThrow(new DuplicateOrisImportException(9876));

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9876}")
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when ORIS event not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenOrisEventNotFound() throws Exception {
            when(orisEventImportPort.importEventFromOris(9999))
                    .thenThrow(new EventNotFoundException(9999));

            mockMvc.perform(
                            post("/api/events/import")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisId\": 9999}")
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/events/{id}/sync-from-oris")
    class SyncFromOrisTests {

        @Test
        @DisplayName("should return 204 No Content on successful sync")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldSyncEventFromOris() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/sync-from-oris", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());

            verify(orisEventImportPort).syncEventFromOris(new EventId(eventId));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            UUID eventId = UUID.randomUUID();

            mockMvc.perform(
                            post("/api/events/{id}/sync-from-oris", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when event not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenEventNotFound() throws Exception {
            UUID eventId = UUID.randomUUID();
            doThrow(new EventNotFoundException(new EventId(eventId)))
                    .when(orisEventImportPort).syncEventFromOris(any());

            mockMvc.perform(
                            post("/api/events/{id}/sync-from-oris", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/events — importFromOris affordance visibility")
    class ImportAffordanceTests {

        @Test
        @DisplayName("should include importFromOris affordance when oris profile is active and user has EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeImportAffordanceWhenOrisActiveAndManager() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent.target").exists());
        }

        @Test
        @DisplayName("should NOT include importEvent affordance when user lacks EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeImportAffordanceWithoutManageAuthority() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id} — syncFromOris affordance visibility")
    class SyncFromOrisAffordanceTests {

        @Test
        @DisplayName("should include syncFromOris affordance for DRAFT event with orisId when oris is active")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeSyncAffordanceForDraftEventWithOrisId() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withOrisId(100)
                    .build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncEventFromOris.target").exists());
        }

        @Test
        @DisplayName("should include syncFromOris affordance for ACTIVE event with orisId when oris is active")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeSyncAffordanceForActiveEventWithOrisId() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event activeEvent = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withOrisId(100)
                    .buildPublished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(activeEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncEventFromOris.target").exists());
        }

        @Test
        @DisplayName("should NOT include syncFromOris affordance when event has no orisId")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldNotIncludeSyncAffordanceWhenNoOrisId() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event draftEvent = EventTestDataBuilder.anEvent().build();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(draftEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncEventFromOris").doesNotExist());
        }

        @Test
        @DisplayName("should NOT include syncFromOris affordance for FINISHED event")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldNotIncludeSyncAffordanceForFinishedEvent() throws Exception {
            UUID eventId = UUID.randomUUID();
            Event finishedEvent = EventTestDataBuilder.anEventWithId(new EventId(eventId))
                    .withOrisId(100)
                    .buildFinished();

            when(eventManagementService.getEvent(any(), anyBoolean())).thenReturn(finishedEvent);
            when(eventRegistrationService.listRegistrations(any())).thenReturn(List.of());

            mockMvc.perform(
                            get("/api/events/{id}", eventId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncEventFromOris").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/events — list row ORIS affordances")
    class ListRowOrisAffordancesTests {

        private static String tpl(String name) {
            return "$._embedded.eventSummaryDtoList[0]._templates." + name;
        }

        @Test
        @DisplayName("ORIS-imported DRAFT row additionally carries syncEventFromOris affordance")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void orisImportedDraftRowCarriesSyncAffordance() throws Exception {
            Event orisEvent = EventTestDataBuilder.anEvent().withOrisId(42).build();

            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(orisEvent), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(tpl("syncEventFromOris.target")).exists());
        }
    }

    @Nested
    @DisplayName("POST /api/events/sync-from-oris/all-upcoming — bulk sync")
    class BulkSyncTests {

        @Test
        @DisplayName("should return 200 with successCount=3 and failureCount=0 when all events sync")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnSuccessSummaryWhenAllSync() throws Exception {
            BulkSyncResult result = new BulkSyncResult(3, 3, 0, List.of(
                    new BulkSyncResult.EventSyncEntry(EventId.generate(), "Race A", BulkSyncResult.SyncStatus.SYNCED, null),
                    new BulkSyncResult.EventSyncEntry(EventId.generate(), "Race B", BulkSyncResult.SyncStatus.SYNCED, null),
                    new BulkSyncResult.EventSyncEntry(EventId.generate(), "Race C", BulkSyncResult.SyncStatus.SYNCED, null)
            ));
            when(orisBulkSyncPort.syncAllUpcoming()).thenReturn(result);

            mockMvc.perform(post("/api/events/sync-from-oris/all-upcoming")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcessed").value(3))
                    .andExpect(jsonPath("$.successCount").value(3))
                    .andExpect(jsonPath("$.failureCount").value(0));
        }

        @Test
        @DisplayName("should return 200 with partial failure details when one event fails")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnPartialFailureSummary() throws Exception {
            EventId failedId = EventId.generate();
            BulkSyncResult result = new BulkSyncResult(2, 1, 1, List.of(
                    new BulkSyncResult.EventSyncEntry(EventId.generate(), "Race A", BulkSyncResult.SyncStatus.SYNCED, null),
                    new BulkSyncResult.EventSyncEntry(failedId, "Race B", BulkSyncResult.SyncStatus.FAILED, "ORIS endpoint returned 404")
            ));
            when(orisBulkSyncPort.syncAllUpcoming()).thenReturn(result);

            mockMvc.perform(post("/api/events/sync-from-oris/all-upcoming")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcessed").value(2))
                    .andExpect(jsonPath("$.successCount").value(1))
                    .andExpect(jsonPath("$.failureCount").value(1))
                    .andExpect(jsonPath("$.results[1].error").value("ORIS endpoint returned 404"))
                    .andExpect(jsonPath("$.results[1].status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(post("/api/events/sync-from-oris/all-upcoming")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should expose bulk-sync-oris affordance in events list when caller has EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeBulkSyncAffordanceInEventsListForManager() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncAllUpcomingFromOris").exists());
        }

        @Test
        @DisplayName("should NOT expose bulk-sync-oris affordance in events list when caller lacks EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotExposeBulkSyncAffordanceWhenNotManager() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncAllUpcomingFromOris").doesNotExist());
        }

        @Test
        @DisplayName("regression: syncAllUpcomingFromOris affordance present on filtered list (dateFrom param)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeBulkSyncAffordanceOnFilteredListWithDateFrom() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events")
                            .param("dateFrom", "2026-05-12")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.syncAllUpcomingFromOris").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/events/import-batch")
    class ImportBatchTests {

        @Test
        @DisplayName("should return 200 with BulkImportResult containing per-event status")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnBulkImportResult() throws Exception {
            BulkImportResult result = new BulkImportResult(2, 2, 0, List.of(
                    new BulkImportResult.EventImportEntry(101, "Spring Sprint", LocalDate.of(2026, 6, 1), BulkImportResult.ImportStatus.IMPORTED, null),
                    new BulkImportResult.EventImportEntry(102, "Summer Cup", LocalDate.of(2026, 7, 15), BulkImportResult.ImportStatus.IMPORTED, null)
            ));
            when(orisEventBulkImportPort.importEventsFromOris(List.of(101, 102))).thenReturn(result);

            mockMvc.perform(
                            post("/api/events/import-batch")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisIds\": [101, 102]}")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcessed").value(2))
                    .andExpect(jsonPath("$.successCount").value(2))
                    .andExpect(jsonPath("$.failureCount").value(0))
                    .andExpect(jsonPath("$.results[0].orisId").value(101))
                    .andExpect(jsonPath("$.results[0].status").value("IMPORTED"))
                    .andExpect(jsonPath("$.results[1].orisId").value(102));
        }

        @Test
        @DisplayName("should return 200 with partial failure when one event fails")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn200WithPartialFailure() throws Exception {
            BulkImportResult result = new BulkImportResult(2, 1, 1, List.of(
                    new BulkImportResult.EventImportEntry(201, "Good Event", LocalDate.of(2026, 6, 1), BulkImportResult.ImportStatus.IMPORTED, null),
                    new BulkImportResult.EventImportEntry(202, null, null, BulkImportResult.ImportStatus.FAILED, "ORIS event 202 already imported")
            ));
            when(orisEventBulkImportPort.importEventsFromOris(List.of(201, 202))).thenReturn(result);

            mockMvc.perform(
                            post("/api/events/import-batch")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisIds\": [201, 202]}")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.failureCount").value(1))
                    .andExpect(jsonPath("$.results[1].status").value("FAILED"))
                    .andExpect(jsonPath("$.results[1].error").value("ORIS event 202 already imported"));
        }

        @Test
        @DisplayName("should return 400 when orisIds list is empty")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400ForEmptyList() throws Exception {
            mockMvc.perform(
                            post("/api/events/import-batch")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisIds\": []}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when orisIds list exceeds 50 entries")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WhenListExceedsMaxSize() throws Exception {
            String ids = java.util.stream.IntStream.rangeClosed(1, 51)
                    .mapToObj(Integer::toString)
                    .collect(java.util.stream.Collectors.joining(", ", "[", "]"));

            mockMvc.perform(
                            post("/api/events/import-batch")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisIds\": " + ids + "}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/events/import-batch")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{\"orisIds\": [101]}")
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/events — importEventsBatch affordance visibility")
    class ImportBatchAffordanceTests {

        @Test
        @DisplayName("should include importEventsBatch affordance when oris profile active and user has EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeImportBatchAffordanceWhenOrisActiveAndManager() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEventsBatch.target").exists());
        }

        @Test
        @DisplayName("should NOT include importEventsBatch affordance when user lacks EVENTS:MANAGE")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ})
        void shouldNotIncludeImportBatchAffordanceWithoutManageAuthority() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEventsBatch").doesNotExist());
        }

        @Test
        @DisplayName("should also keep importEvent affordance alongside importEventsBatch")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldKeepImportEventAffordanceAlongsideImportBatch() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/events").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent.target").exists())
                    .andExpect(jsonPath("$._templates.importEventsBatch.target").exists());
        }
    }
}
