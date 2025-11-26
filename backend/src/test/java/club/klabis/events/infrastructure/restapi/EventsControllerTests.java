package club.klabis.events.infrastructure.restapi;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.events.application.EventsRepository;
import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisData;
import club.klabis.events.domain.OrisId;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventsController.class)
@Import({ApiTestConfiguration.class, EventModelMapperImpl.class})
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL_FORMS)
@ComponentScan("club.klabis.oris.infrastructure.restapi.eventapi")
public class EventsControllerTests {
    @MockitoBean
    EventsRepository eventsRepositoryMock;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KlabisSecurityService klabisSecurityService;

    static Event createEventWithOrisId(OrisId orisId) {
        return Competition.importFrom(new OrisData(
                orisId,
                "Test",
                LocalDate.now(),
                ZonedDateTime.now().minusDays(2),
                "Brno",
                "ZBM",
                Collections.emptyList(),
                null));
    }

    @Test
    @WithMockUser
    @DisplayName("it should add synchronize link to event with OrisId for user with SystemAdmin grant")
    void itShouldAddLinkToEventWithOrisId() throws Exception {
        when(klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)).thenReturn(true);

        when(eventsRepositoryMock.findById(new Event.Id(1))).thenReturn(
                Optional.of(createEventWithOrisId(new OrisId(3))));

        mockMvc.perform(get("/events/{eventId}", 1).accept(MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.synchronize.href").value("http://localhost/oris/synchronizeEvents"));
    }

    @Test
    @WithMockUser
    @DisplayName("it should NOT add synchronize link to event with OrisId for user WITHOUT SystemAdmin grant")
    void itShouldNotAddLinkToUnauthorizedUser() throws Exception {
        when(klabisSecurityService.hasGrant(ApplicationGrant.SYSTEM_ADMIN)).thenReturn(false);

        when(eventsRepositoryMock.findById(new Event.Id(1))).thenReturn(
                Optional.of(createEventWithOrisId(new OrisId(3))));

        mockMvc.perform(get("/events/{eventId}", 1).accept(MediaTypes.HAL_FORMS_JSON_VALUE, MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.synchronize").doesNotExist());
    }

}
