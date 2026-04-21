package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.application.DuplicateOrisImportException;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventRegistrationPort;
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
        @DisplayName("should include importFromOris affordance when oris profile is active")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeImportAffordanceWhenOrisActive() throws Exception {
            when(eventManagementService.listEvents(any(EventFilter.class), any(), anyBoolean()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(
                            get("/api/events")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.importEvent.target").exists());
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
}
