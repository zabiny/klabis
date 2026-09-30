package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("LegalGuardianGroupController API tests")
@WebMvcTest(controllers = {LegalGuardianGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class LegalGuardianGroupControllerTest {

    private static final String ADMIN_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String GUARDIAN_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_GUARDIAN_ID = "22222222-2222-2222-2222-222222222222";
    private static final String MINOR_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LegalGuardianGroupPort legalGuardianGroupService;

    private static LegalGuardianGroup groupOfMinor(UUID groupUuid, String name, String guardianId, String minorId) {
        return LegalGuardianGroup.reconstruct(new LegalGuardianGroupId(groupUuid), name,
                Set.of(new UserId(UUID.fromString(guardianId))),
                Set.of(GroupMembership.of(new UserId(UUID.fromString(minorId)))), null);
    }

    @Nested
    @DisplayName("GET /api/legal-guardian-groups")
    class ListGroups {

        @Test
        @DisplayName("returns groups with self link and no create affordance for MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void listsGroups() throws Exception {
            when(legalGuardianGroupService.listGroups())
                    .thenReturn(List.of(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID)));

            mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].name").value("Novák"))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].minorCount").value(1))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0]._links.self.href")
                            .value(containsString("/api/legal-guardian-groups/" + GROUP_UUID)))
                    .andExpect(jsonPath("$._templates").doesNotExist());
        }

        @Test
        @DisplayName("returns 403 without MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenWithoutManage() throws Exception {
            mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/legal-guardian-groups/{id}")
    class GetGroup {

        @Test
        @DisplayName("returns guardians and minors with links to their members")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
        void returnsGuardiansAndMinors() throws Exception {
            when(legalGuardianGroupService.getGroup(new LegalGuardianGroupId(GROUP_UUID)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(GROUP_UUID.toString()))
                    .andExpect(jsonPath("$.name").value("Novák"))
                    .andExpect(jsonPath("$.guardians[0].userId").value(GUARDIAN_ID))
                    .andExpect(jsonPath("$.guardians[0]._links.member.href")
                            .value(containsString("/api/members/" + GUARDIAN_ID)))
                    .andExpect(jsonPath("$.minors[0].memberId").value(MINOR_ID))
                    .andExpect(jsonPath("$.minors[0].joinedAt").exists())
                    .andExpect(jsonPath("$.minors[0]._links.member.href")
                            .value(containsString("/api/members/" + MINOR_ID)));
        }

        @Test
        @DisplayName("offers the guardian-setting affordance and a link back to the list, but no delete")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void offersSetGuardiansAffordance() throws Exception {
            when(legalGuardianGroupService.getGroup(any(LegalGuardianGroupId.class)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.setLegalGuardianGroupGuardians.method").value("PUT"))
                    .andExpect(jsonPath("$._templates.setLegalGuardianGroupGuardians.target")
                            .value(containsString("/api/legal-guardian-groups/" + GROUP_UUID + "/guardians")))
                    .andExpect(jsonPath("$._templates.default").doesNotExist())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("returns 403 to a minor of the group who lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MINOR_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenForMinorOfTheGroup() throws Exception {
            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 for an unknown group")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void notFound() throws Exception {
            when(legalGuardianGroupService.getGroup(any(LegalGuardianGroupId.class)))
                    .thenThrow(new GroupNotFoundException("LegalGuardian", GROUP_UUID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/legal-guardian-groups/{id}/guardians")
    class SetGuardians {

        private static final String BODY = """
                {"legalGuardians": [{"userId": "%s"}, {"userId": "%s"}]}
                """.formatted(GUARDIAN_ID, OTHER_GUARDIAN_ID);

        @Test
        @DisplayName("returns 204 and passes the guardians to the service")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void setsGuardians() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isNoContent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Set<UserId>> guardians = ArgumentCaptor.forClass(Set.class);
            verify(legalGuardianGroupService).changeGroupGuardians(eq(new LegalGuardianGroupId(GROUP_UUID)), guardians.capture());
            assertThat(guardians.getValue()).containsExactlyInAnyOrder(
                    new UserId(UUID.fromString(GUARDIAN_ID)), new UserId(UUID.fromString(OTHER_GUARDIAN_ID)));
        }

        @Test
        @DisplayName("returns 400 for an empty list of guardians")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsEmptyList() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"legalGuardians": []}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when the service refuses the set of guardians")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsWhenServiceRefuses() throws Exception {
            doThrow(new LegalGuardianGroupWithoutGuardianException())
                    .when(legalGuardianGroupService).changeGroupGuardians(any(), any());

            mockMvc.perform(put("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when a guardian has no userId")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsGuardianWithoutUserId() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"legalGuardians": [{}]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 403 without MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenWithoutManage() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(legalGuardianGroupService);
        }
    }

    @Nested
    @DisplayName("Groups cannot be created or deleted by hand")
    class NoManualCreateOrDelete {

        @Test
        @DisplayName("POST on the collection is not allowed")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void noCreate() throws Exception {
            mockMvc.perform(post("/api/legal-guardian-groups")
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE on a group is not allowed")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void noDelete() throws Exception {
            mockMvc.perform(delete("/api/legal-guardian-groups/{id}", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
