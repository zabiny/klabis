package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.events.application.EventManagementService;
import com.klabis.events.application.EventRegistrationService;
import com.klabis.events.domain.EventFilter;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests EventController behavior when OrisApiClient bean is NOT present (ORIS profile inactive).
 */
@DisplayName("EventController — no ORIS integration")
@WebMvcTest(controllers = {EventController.class, EventsExceptionHandler.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class EventControllerNoOrisTest {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventManagementService eventManagementService;

    @MockitoBean
    private EventRegistrationService eventRegistrationService;

    @MockitoBean
    private Members members;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    // No OrisApiClient mock — Optional<OrisApiClient> will be Optional.empty()

    @Test
    @DisplayName("should NOT include importFromOris affordance when OrisApiClient bean is absent")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
    void shouldNotIncludeImportAffordanceWhenOrisInactive() throws Exception {
        when(eventManagementService.listEvents(any(EventFilter.class), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(
                        get("/api/events")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.importEvent").doesNotExist());
    }
}
