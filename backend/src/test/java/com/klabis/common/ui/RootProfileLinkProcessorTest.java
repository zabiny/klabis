package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.LegalGuardianDto;
import com.klabis.members.LegalGuardians;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RootProfileLinkProcessor")
@WebMvcTest(controllers = RootController.class)
@Import(HalFormsSupport.class)
@WithPostprocessors
class RootProfileLinkProcessorTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String GUARDIAN_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalGuardians legalGuardians;

    @Test
    @WithKlabisMockUser(memberId = MEMBER_ID, authorities = Authority.MEMBERS_READ)
    @DisplayName("links the profile of a member to the member detail")
    void memberProfile() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.profile.href", endsWith("/api/members/" + MEMBER_ID)));
    }

    @Test
    @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = Authority.MEMBERS_READ)
    @DisplayName("links the profile of a non-member guardian to the guardian profile")
    void guardianProfile() throws Exception {
        reset(legalGuardians);
        when(legalGuardians.findById(new UserId(UUID.fromString(GUARDIAN_ID)))).thenReturn(Optional.of(
                new LegalGuardianDto(UUID.fromString(GUARDIAN_ID), "Petr", "Rodič", "petr@example.com", LocalDateTime.now())));

        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.profile.href", endsWith("/api/legal-guardians/" + GUARDIAN_ID)));
    }

    @Test
    @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = Authority.MEMBERS_READ)
    @DisplayName("omits the profile link for a user who is neither member nor guardian")
    void noProfile() throws Exception {
        reset(legalGuardians);
        when(legalGuardians.findById(new UserId(UUID.fromString(GUARDIAN_ID)))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.profile").doesNotExist());
    }
}
