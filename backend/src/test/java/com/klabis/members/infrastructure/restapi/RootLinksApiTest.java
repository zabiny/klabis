package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.LegalGuardianDto;
import com.klabis.members.LegalGuardians;
import com.klabis.members.MembersWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MembersWebMvcTest
@DisplayName("Links contributed by members to the API root")
class RootLinksApiTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String GUARDIAN_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalGuardians legalGuardians;

    @Nested
    @DisplayName("profile link")
    class ProfileLinkTests {

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
            when(legalGuardians.findById(new UserId(UUID.fromString(GUARDIAN_ID)))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.profile").doesNotExist());
        }
    }

    @Nested
    @DisplayName("ORIS club-key link")
    class OrisClubKeyLinkTests {

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("adds the club-key link for a user with SYNC:MANAGE")
        void addsClubKeyLinkForUserWithSyncManage() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.orisClubKey.href", endsWith("/api/oris/club-key")));
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("omits the club-key link for a user without SYNC:MANAGE")
        void omitsClubKeyLinkWithoutSyncManage() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.orisClubKey").doesNotExist());
        }
    }
}
