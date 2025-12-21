package club.klabis.events.infrastructure.restapi;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.events.application.EventsRepository;
import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisId;
import club.klabis.oris.application.OrisEventsImporter;
import club.klabis.oris.infrastructure.apiclient.OrisApiClient;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ApiTestConfiguration(controllers = EventsController.class)
@Import({EventModelMapperImpl.class})
@ComponentScan("club.klabis.oris.infrastructure.restapi")
@ActiveProfiles("oris")
public class EventsControllerTests {
    @MockitoBean
    EventsRepository eventsRepositoryMock;

    @MockitoBean
    OrisApiClient orisApiClientMock;

    @MockitoBean
    OrisEventsImporter orisEventsImporter;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KlabisSecurityService klabisSecurityService;

    static Event createEventWithOrisId(OrisId orisId) {
        Competition result = Competition.newEvent("Test", LocalDate.now(), Set.of());
        result.linkWithOris(orisId);
        return result;
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("it should add synchronize affordance to event with OrisId for user with SystemAdmin grant")
    void itShouldAddLinkToEventWithOrisId() throws Exception {
        final Event event = createEventWithOrisId(new OrisId(3));
        final Event.Id eventId = event.getId();

        when(eventsRepositoryMock.findById(eventId)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/events/{eventId}", eventId.value()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.synchronizeEventWithOris.target").value(
                        "http://localhost/events/%d/synchronizeWithOris".formatted(eventId.value())));
    }


    @Test
    @WithMockUser
    @DisplayName("it should NOT add synchronize affordance to event with OrisId for user WITHOUT SystemAdmin grant")
    void itShouldNotAddLinkToUnauthorizedUser() throws Exception {
        when(eventsRepositoryMock.findById(new Event.Id(1))).thenReturn(
                Optional.of(createEventWithOrisId(new OrisId(3))));

        mockMvc.perform(get("/events/{eventId}", 1).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.synchronizeEventWithOris.target").doesNotExist());
    }

}
