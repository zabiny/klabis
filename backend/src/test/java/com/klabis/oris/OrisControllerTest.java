package com.klabis.oris;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.EventSummary;
import com.dpolach.api.orisclient.dto.Organizer;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.application.ImportedOrisEventsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OrisController — GET /api/oris/events filtering tests")
@WebMvcTest(controllers = OrisController.class)
@ActiveProfiles("oris")
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class OrisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrisApiClient orisApiClient;

    @MockitoBean
    private ImportedOrisEventsPort importedOrisEventsPort;

    @Nested
    @DisplayName("GET /api/oris/events — filtering already-imported events")
    class ListOrisEventsFiltering {

        @Test
        @DisplayName("should exclude already-imported events from the response")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldExcludeAlreadyImportedEvents() throws Exception {
            EventSummary importedEvent = orisEventSummary(101, "Imported Race", LocalDate.of(2026, 8, 1));
            EventSummary newEvent = orisEventSummary(202, "New Race", LocalDate.of(2026, 8, 10));
            stubOrisApiReturning(List.of(importedEvent, newEvent));
            when(importedOrisEventsPort.findImportedOrisIds(any())).thenReturn(Set.of(101));

            mockMvc.perform(get("/api/oris/events").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(202))
                    .andExpect(jsonPath("$[0].name").value("New Race"));
        }

        @Test
        @DisplayName("should return all events when none are imported")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnAllEventsWhenNoneImported() throws Exception {
            EventSummary event1 = orisEventSummary(301, "Race Alpha", LocalDate.of(2026, 9, 1));
            EventSummary event2 = orisEventSummary(302, "Race Beta", LocalDate.of(2026, 9, 5));
            stubOrisApiReturning(List.of(event1, event2));
            when(importedOrisEventsPort.findImportedOrisIds(any())).thenReturn(Set.of());

            mockMvc.perform(get("/api/oris/events").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should return empty list when all events are already imported")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnEmptyListWhenAllImported() throws Exception {
            EventSummary event1 = orisEventSummary(401, "Imported A", LocalDate.of(2026, 7, 1));
            EventSummary event2 = orisEventSummary(402, "Imported B", LocalDate.of(2026, 7, 5));
            stubOrisApiReturning(List.of(event1, event2));
            when(importedOrisEventsPort.findImportedOrisIds(any())).thenReturn(Set.of(401, 402));

            mockMvc.perform(get("/api/oris/events").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should return 403 when user lacks EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = "member", authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(get("/api/oris/events").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    private void stubOrisApiReturning(List<EventSummary> items) {
        Map<String, EventSummary> eventMap = new LinkedHashMap<>();
        items.forEach(item -> eventMap.put(String.valueOf(item.id()), item));
        OrisApiClient.OrisResponse<Map<String, EventSummary>> response =
                new OrisApiClient.OrisResponse<>(eventMap, "JSON", "OK", null, "getEventList");
        when(orisApiClient.getEventList(any())).thenReturn(response);
    }

    private EventSummary orisEventSummary(int id, String name, LocalDate date) {
        return new EventSummary(id, name, date, "Test Location",
                new Organizer(1, "OOB", "Orel Brno"),
                null, null, null, null, null, null);
    }
}
