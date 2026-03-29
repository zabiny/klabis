package com.klabis.oris;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.oris.apiclient.OrisApiClient;
import com.klabis.oris.apiclient.OrisEventListFilter;
import com.klabis.oris.apiclient.dto.EventSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("OrisController API tests")
@WebMvcTest(controllers = OrisController.class)
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@ActiveProfiles("oris")
class OrisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrisApiClient orisApiClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("GET /api/oris/events")
    class ListOrisEventsTests {

        @Test
        @DisplayName("should return 200 with correct payload using default region")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn200WithEventList() throws Exception {
            EventSummary event1 = buildEventSummary(1001, "City Sprint", LocalDate.of(2026, 9, 5));
            EventSummary event2 = buildEventSummary(1002, "Forest Long", LocalDate.of(2026, 10, 12));

            when(orisApiClient.getEventList(any(OrisEventListFilter.class))).thenReturn(
                    new OrisApiClient.OrisResponse<>(
                            Map.of("1001", event1, "1002", event2),
                            "JSON", "OK", null, "getEventList"
                    )
            );

            mockMvc.perform(
                            get("/api/oris/events")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should accept multiple region parameters")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldAcceptMultipleRegions() throws Exception {
            EventSummary event1 = buildEventSummary(1001, "JM Event", LocalDate.of(2026, 9, 5));
            EventSummary event2 = buildEventSummary(1002, "M Event", LocalDate.of(2026, 10, 12));

            when(orisApiClient.getEventList(any(OrisEventListFilter.class)))
                    .thenReturn(new OrisApiClient.OrisResponse<>(
                            Map.of("1001", event1), "JSON", "OK", null, "getEventList"))
                    .thenReturn(new OrisApiClient.OrisResponse<>(
                            Map.of("1002", event2), "JSON", "OK", null, "getEventList"));

            mockMvc.perform(
                            get("/api/oris/events")
                                    .param("region", "JM", "M")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should pass any region value to ORIS API")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldPassAnyRegionToOrisApi() throws Exception {
            when(orisApiClient.getEventList(any(OrisEventListFilter.class))).thenReturn(
                    new OrisApiClient.OrisResponse<>(Map.of(), "JSON", "OK", null, "getEventList")
            );

            mockMvc.perform(
                            get("/api/oris/events")
                                    .param("region", "VC")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("should return 403 without EVENTS:MANAGE authority")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_READ})
        void shouldReturn403WithoutEventsManageAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/oris/events")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return empty array when ORIS returns no data")
        @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnEmptyArrayWhenOrisReturnsNoData() throws Exception {
            when(orisApiClient.getEventList(any(OrisEventListFilter.class))).thenReturn(
                    new OrisApiClient.OrisResponse<>(null, "JSON", "OK", null, "getEventList")
            );

            mockMvc.perform(
                            get("/api/oris/events")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        private EventSummary buildEventSummary(int id, String name, LocalDate date) {
            return new EventSummary(id, name, date, "Test Location", new com.klabis.oris.apiclient.dto.Organizer(1, "TST", "Test Club"), null, null, null, null, null, null);
        }
    }
}
