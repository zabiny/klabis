package com.klabis.calendar.api;

import com.klabis.E2EIntegrationTest;
import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.persistence.CalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CalendarController with real Spring context.
 * <p>
 * Tests HATEOAS link generation, authorization, and end-to-end flows.
 */
@E2EIntegrationTest
@DisplayName("CalendarController Integration Tests")
class CalendarControllerIntegrationTest {

    private static final String CALENDAR_MANAGE_AUTHORITY = "CALENDAR:MANAGE";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarRepository calendarRepository;

    @BeforeEach
    void setUp() {
        // Clean up calendar items before each test to ensure test isolation
        calendarRepository.findByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31))
                .forEach(calendarRepository::delete);
    }

    @Test
    @DisplayName("should create and retrieve manual calendar item with HATEOAS links")
    @WithMockUser(authorities = {CALENDAR_MANAGE_AUTHORITY})
    void shouldCreateAndRetrieveManualCalendarItemWithHateoasLinks() throws Exception {
        String createRequestBody = """
                {
                  "name": "Training Session",
                  "description": "Weekly training at the park",
                  "startDate": "2026-03-15",
                  "endDate": "2026-03-15"
                }
                """;

        String locationHeader = mockMvc.perform(
                        post("/api/calendar-items")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(createRequestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Training Session"))
                .andExpect(jsonPath("$.eventId").isEmpty())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        UUID calendarItemId = UUID.fromString(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

        mockMvc.perform(
                        get("/api/calendar-items/{id}", calendarItemId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(calendarItemId.toString()))
                .andExpect(jsonPath("$.name").value("Training Session"))
                .andExpect(jsonPath("$.description").value("Weekly training at the park"))
                .andExpect(jsonPath("$.eventId").isEmpty())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/calendar-items/" + calendarItemId)))
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andExpect(jsonPath("$._templates.default.method").exists())
                .andExpect(jsonPath("$._templates.deleteCalendarItem.method").exists());
    }

    // Note: Event-linked calendar items require actual events in database due to FK constraint
    // These are tested in event-driven sync integration tests

    @Test
    @DisplayName("should list calendar items with provided date range")
    @WithMockUser(authorities = {MEMBERS_READ_AUTHORITY})
    void shouldListCalendarItemsWithProvidedDateRange() throws Exception {
        CalendarItem item1 = CalendarItem.create(
                "March Training",
                "Training in March",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 10)
        );

        CalendarItem item2 = CalendarItem.create(
                "April Training",
                "Training in April",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 10)
        );

        calendarRepository.save(item1);
        calendarRepository.save(item2);

        mockMvc.perform(
                        get("/api/calendar-items")
                                .param("startDate", "2026-03-01")
                                .param("endDate", "2026-03-31")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.calendarItemDtoList").isArray())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.default.method").exists());
    }

    @Test
    @DisplayName("should list calendar items for current month when dates not provided")
    @WithMockUser(authorities = {MEMBERS_READ_AUTHORITY})
    void shouldListCalendarItemsForCurrentMonthWhenDatesNotProvided() throws Exception {
        LocalDate today = LocalDate.now();
        CalendarItem item = CalendarItem.create(
                "Current Month Training",
                "Training this month",
                today,
                today
        );

        calendarRepository.save(item);

        mockMvc.perform(
                        get("/api/calendar-items")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.calendarItemDtoList").isArray())
                .andExpect(jsonPath("$._embedded.calendarItemDtoList[0].name").value("Current Month Training"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.default.method").exists());
    }

    @Test
    @DisplayName("should update manual calendar item and preserve HATEOAS links")
    @WithMockUser(authorities = {CALENDAR_MANAGE_AUTHORITY})
    void shouldUpdateManualCalendarItemAndPreserveHateoasLinks() throws Exception {
        CalendarItem manualItem = CalendarItem.create(
                "Original Name",
                "Original description",
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15)
        );

        CalendarItem savedItem = calendarRepository.save(manualItem);

        String updateRequestBody = """
                {
                  "name": "Updated Name",
                  "description": "Updated description",
                  "startDate": "2026-03-20",
                  "endDate": "2026-03-20"
                }
                """;

        mockMvc.perform(
                        put("/api/calendar-items/{id}", savedItem.getId().value())
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(updateRequestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.startDate").value("2026-03-20"))
                .andExpect(jsonPath("$.eventId").isEmpty())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._templates.default.method").exists());
    }

    // Note: Event-linked calendar item update tests require actual events in database
    // These are tested in event-driven sync integration tests

    @Test
    @DisplayName("should delete manual calendar item")
    @WithMockUser(authorities = {CALENDAR_MANAGE_AUTHORITY})
    void shouldDeleteManualCalendarItem() throws Exception {
        CalendarItem manualItem = CalendarItem.create(
                "To Delete",
                "This will be deleted",
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15)
        );

        CalendarItem savedItem = calendarRepository.save(manualItem);

        mockMvc.perform(
                        delete("/api/calendar-items/{id}", savedItem.getId().value())
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/calendar-items/{id}", savedItem.getId().value())
                )
                .andExpect(status().isNotFound());
    }

    // Note: Event-linked calendar item delete tests require actual events in database
    // These are tested in event-driven sync integration tests

    @Test
    @DisplayName("should include next and prev month navigation links with ISO DATE format")
    @WithMockUser(authorities = {MEMBERS_READ_AUTHORITY})
    void shouldIncludeNextAndPrevMonthNavigationLinks() throws Exception {
        CalendarItem item = CalendarItem.create(
                "March Event",
                "Event in March",
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15)
        );

        calendarRepository.save(item);

        mockMvc.perform(
                        get("/api/calendar-items")
                                .param("startDate", "2026-03-01")
                                .param("endDate", "2026-03-31")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.next.href").value(org.hamcrest.Matchers.containsString("startDate=2026-04-01")))
                .andExpect(jsonPath("$._links.next.href").value(org.hamcrest.Matchers.containsString("endDate=2026-04-30")))
                .andExpect(jsonPath("$._links.prev.href").exists())
                .andExpect(jsonPath("$._links.prev.href").value(org.hamcrest.Matchers.containsString("startDate=2026-02-01")))
                .andExpect(jsonPath("$._links.prev.href").value(org.hamcrest.Matchers.containsString("endDate=2026-02-28")));
    }

    @Test
    @DisplayName("should preserve sort parameter in navigation links")
    @WithMockUser(authorities = {MEMBERS_READ_AUTHORITY})
    void shouldPreserveSortParameterInNavigationLinks() throws Exception {
        CalendarItem item = CalendarItem.create(
                "March Event",
                "Event in March",
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 15)
        );

        calendarRepository.save(item);

        mockMvc.perform(
                        get("/api/calendar-items")
                                .param("startDate", "2026-03-01")
                                .param("endDate", "2026-03-31")
                                .param("sort", "name,desc")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.next.href").value(org.hamcrest.Matchers.containsString("sort=name%2Cdesc")))
                .andExpect(jsonPath("$._links.prev.href").value(org.hamcrest.Matchers.containsString("sort=name%2Cdesc")));
    }
}
