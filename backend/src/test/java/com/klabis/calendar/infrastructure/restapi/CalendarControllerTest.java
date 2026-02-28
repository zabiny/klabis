package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CalendarController API tests")
@WebMvcTest(controllers = CalendarController.class)
@MockitoBean(types = {UserService.class, UserDetailsService.class})
class CalendarControllerTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarManagementService calendarManagementService;

    @Nested
    @DisplayName("GET /api/calendar-items")
    class ListCalendarItemsTests {

        @Test
        @DisplayName("should return 200 with paginated calendar items when dates provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldListCalendarItemsWithDates() throws Exception {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            CalendarItemDto dto1 = new CalendarItemDto(
                    UUID.randomUUID(),
                    "Spring Training",
                    "Monthly training session",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15),
                    null
            );

            CalendarItemDto dto2 = new CalendarItemDto(
                    UUID.randomUUID(),
                    "Spring Cup 2026",
                    "Forest Park - OOB",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20),
                    UUID.randomUUID()
            );

            PageImpl<CalendarItemDto> page = new PageImpl<>(
                    List.of(dto1, dto2),
                    PageRequest.of(0, 20),
                    2
            );

            when(calendarManagementService.listCalendarItems(eq(startDate), eq(endDate), any())).thenReturn(List.of(dto1, dto2));

            mockMvc.perform(
                            get("/api/calendar-items")
                                    .param("startDate", "2026-03-01")
                                    .param("endDate", "2026-03-31")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.calendarItemDtoList[0].name").value("Spring Training"))
                    .andExpect(jsonPath("$._embedded.calendarItemDtoList[1].name").value("Spring Cup 2026"))
                    .andExpect(jsonPath("$._embedded.calendarItemDtoList").isArray());
        }

        @Test
        @DisplayName("should use current month as default when dates not provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldUseCurrentMonthAsDefault() throws Exception {
            LocalDate today = LocalDate.now();
            LocalDate firstDay = today.withDayOfMonth(1);
            LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

            CalendarItemDto dto = new CalendarItemDto(
                    UUID.randomUUID(),
                    "Current Month Event",
                    "Event in current month",
                    today,
                    today,
                    null
            );

            PageImpl<CalendarItemDto> page = new PageImpl<>(
                    List.of(dto),
                    PageRequest.of(0, 20),
                    1
            );

            when(calendarManagementService.listCalendarItems(eq(firstDay), eq(lastDay), any())).thenReturn(List.of(dto));

            mockMvc.perform(
                            get("/api/calendar-items")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.calendarItemDtoList[0].name").value("Current Month Event"))
                    .andExpect(jsonPath("$._embedded.calendarItemDtoList").isArray());
        }

        @Test
        @DisplayName("should return 400 with invalid sort field")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldReturn400WithInvalidSortField() throws Exception {
            mockMvc.perform(
                            get("/api/calendar-items")
                                    .param("startDate", "2026-03-01")
                                    .param("endDate", "2026-03-31")
                                    .param("sort", "invalidField,asc")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should include next and prev month navigation links with ISO DATE format")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldIncludeNextAndPrevMonthNavigationLinks() throws Exception {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            CalendarItemDto dto = new CalendarItemDto(
                    UUID.randomUUID(),
                    "March Event",
                    "Event in March",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15),
                    null
            );

            when(calendarManagementService.listCalendarItems(eq(startDate), eq(endDate), any())).thenReturn(List.of(dto));

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
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldPreserveSortParameterInNavigationLinks() throws Exception {
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            CalendarItemDto dto = new CalendarItemDto(
                    UUID.randomUUID(),
                    "March Event",
                    "Event in March",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15),
                    null
            );

            when(calendarManagementService.listCalendarItems(eq(startDate), eq(endDate), any())).thenReturn(List.of(dto));

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

    @Nested
    @DisplayName("GET /api/calendar-items/{id}")
    class GetCalendarItemTests {

        @Test
        @DisplayName("should return 200 with calendar item details for manual item")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldGetManualCalendarItem() throws Exception {
            UUID calendarItemId = UUID.randomUUID();
            CalendarItemDto dto = new CalendarItemDto(
                    calendarItemId,
                    "Training Session",
                    "Weekly training session",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15),
                    null
            );

            when(calendarManagementService.getCalendarItem(calendarItemId)).thenReturn(dto);

            mockMvc.perform(
                            get("/api/calendar-items/{id}", calendarItemId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(calendarItemId.toString()))
                    .andExpect(jsonPath("$.name").value("Training Session"))
                    .andExpect(jsonPath("$.description").value("Weekly training session"))
                    .andExpect(jsonPath("$.eventId").isEmpty())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._templates.default.method").exists())
                    .andExpect(jsonPath("$._templates.deleteCalendarItem.method").exists());
        }

        @Test
        @DisplayName("should return 200 with calendar item details for event-linked item")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldGetEventLinkedCalendarItem() throws Exception {
            UUID calendarItemId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            CalendarItemDto dto = new CalendarItemDto(
                    calendarItemId,
                    "Spring Cup 2026",
                    "Forest Park - OOB",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20),
                    eventId
            );

            when(calendarManagementService.getCalendarItem(calendarItemId)).thenReturn(dto);

            mockMvc.perform(
                            get("/api/calendar-items/{id}", calendarItemId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(calendarItemId.toString()))
                    .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").value("/api/events/" + eventId))
                    .andExpect(jsonPath("$._links.collection.href").exists());
        }

        @Test
        @DisplayName("should return 404 when calendar item not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldReturn404WhenCalendarItemNotFound() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            when(calendarManagementService.getCalendarItem(calendarItemId))
                    .thenThrow(new CalendarNotFoundException(calendarItemId));

            mockMvc.perform(
                            get("/api/calendar-items/{id}", calendarItemId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/calendar-items")
    class CreateCalendarItemTests {

        @Test
        @DisplayName("should return 201 with Location header and HAL+FORMS links")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldCreateCalendarItemWithValidData() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            CalendarItemDto calendarItemDto = new CalendarItemDto(
                    calendarItemId,
                    "Training Session",
                    "Weekly training session at the park",
                    LocalDate.of(2026, 3, 15),
                    LocalDate.of(2026, 3, 15),
                    null
            );

            when(calendarManagementService.createCalendarItem(any(CreateCalendarItemCommand.class))).thenReturn(calendarItemId);
            when(calendarManagementService.getCalendarItem(calendarItemId)).thenReturn(calendarItemDto);

            mockMvc.perform(
                            post("/api/calendar-items")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Training Session",
                                              "description": "Weekly training session at the park",
                                              "startDate": "2026-03-15",
                                              "endDate": "2026-03-15"
                                            }
                                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/calendar-items/" + calendarItemId)))
                    .andExpect(jsonPath("$.id").value(calendarItemId.toString()))
                    .andExpect(jsonPath("$.name").value("Training Session"))
                    .andExpect(jsonPath("$.eventId").isEmpty())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should return 403 without CALENDAR:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldReturn403WithoutCalendarManageAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/calendar-items")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Training Session",
                                              "description": "Weekly training session",
                                              "startDate": "2026-03-15",
                                              "endDate": "2026-03-15"
                                            }
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 with invalid data (blank name)")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn400WithInvalidData() throws Exception {
            mockMvc.perform(
                            post("/api/calendar-items")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "",
                                              "description": "Description",
                                              "startDate": "2026-03-15",
                                              "endDate": "2026-03-15"
                                            }
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/calendar-items/{id}")
    class UpdateCalendarItemTests {

        @Test
        @DisplayName("should return 200 with updated calendar item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldUpdateCalendarItemWithValidData() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            CalendarItemDto calendarItemDto = new CalendarItemDto(
                    calendarItemId,
                    "Updated Training Session",
                    "Updated description",
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 3, 20),
                    null
            );

            when(calendarManagementService.getCalendarItem(calendarItemId)).thenReturn(calendarItemDto);

            mockMvc.perform(
                            put("/api/calendar-items/{id}", calendarItemId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Updated Training Session",
                                              "description": "Updated description",
                                              "startDate": "2026-03-20",
                                              "endDate": "2026-03-20"
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(calendarItemId.toString()))
                    .andExpect(jsonPath("$.name").value("Updated Training Session"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._templates.default.method").exists());
        }

        @Test
        @DisplayName("should return 400 when trying to update event-linked calendar item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn400WhenUpdatingEventLinkedItem() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            doThrow(new CalendarItemReadOnlyException())
                    .when(calendarManagementService).updateCalendarItem(eq(calendarItemId), any(UpdateCalendarItemCommand.class));

            mockMvc.perform(
                            put("/api/calendar-items/{id}", calendarItemId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Updated Name",
                                              "description": "Updated description",
                                              "startDate": "2026-03-20",
                                              "endDate": "2026-03-20"
                                            }
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 without CALENDAR:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldReturn403WithoutCalendarManageAuthority() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            mockMvc.perform(
                            put("/api/calendar-items/{id}", calendarItemId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Updated Name",
                                              "description": "Updated description",
                                              "startDate": "2026-03-20",
                                              "endDate": "2026-03-20"
                                            }
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when calendar item not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn404WhenCalendarItemNotFound() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            doThrow(new CalendarNotFoundException(calendarItemId))
                    .when(calendarManagementService).updateCalendarItem(eq(calendarItemId), any(UpdateCalendarItemCommand.class));

            mockMvc.perform(
                            put("/api/calendar-items/{id}", calendarItemId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                              "name": "Updated Name",
                                              "description": "Updated description",
                                              "startDate": "2026-03-20",
                                              "endDate": "2026-03-20"
                                            }
                                            """)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/calendar-items/{id}")
    class DeleteCalendarItemTests {

        @Test
        @DisplayName("should return 204 when deleting calendar item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldDeleteCalendarItem() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            mockMvc.perform(
                            delete("/api/calendar-items/{id}", calendarItemId)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when trying to delete event-linked calendar item")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn400WhenDeletingEventLinkedItem() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            doThrow(new CalendarItemReadOnlyException())
                    .when(calendarManagementService).deleteCalendarItem(calendarItemId);

            mockMvc.perform(
                            delete("/api/calendar-items/{id}", calendarItemId)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 without CALENDAR:MANAGE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME)
        void shouldReturn403WithoutCalendarManageAuthority() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            mockMvc.perform(
                            delete("/api/calendar-items/{id}", calendarItemId)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when calendar item not found")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.CALENDAR_MANAGE})
        void shouldReturn404WhenCalendarItemNotFound() throws Exception {
            UUID calendarItemId = UUID.randomUUID();

            doThrow(new CalendarNotFoundException(calendarItemId))
                    .when(calendarManagementService).deleteCalendarItem(calendarItemId);

            mockMvc.perform(
                            delete("/api/calendar-items/{id}", calendarItemId)
                    )
                    .andExpect(status().isNotFound());
        }
    }
}
