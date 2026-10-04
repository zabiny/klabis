package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.members.MembersWebMvcTest;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.domain.OnlyMinorsAllowedException;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianKind;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("LegalGuardianGroupController API tests")
@MembersWebMvcTest
class LegalGuardianGroupControllerTest {

    private static final String ADMIN_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String GUARDIAN_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_GUARDIAN_ID = "22222222-2222-2222-2222-222222222222";
    private static final String MINOR_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupService;

    @Autowired
    private Members members;

    @BeforeEach
    void stubMinorDetails() {
        when(members.findByIds(anyCollection())).thenReturn(Map.of(new MemberId(UUID.fromString(MINOR_ID)),
                new MemberDto(UUID.fromString(MINOR_ID), "Tomáš", "Novák", null, "ZBM1501", LocalDateTime.now())));
    }

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
            when(legalGuardianGroupService.listGuardiansOf(anyCollection()))
                    .thenReturn(Map.of(new LegalGuardianGroupId(GROUP_UUID), List.of(
                            new GuardianContact(new UserId(UUID.fromString(GUARDIAN_ID)), "Petr", "Novák",
                                    "petr@example.com", "+420123456789", GuardianKind.LEGAL_GUARDIAN))));

            mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].name").value("Novák"))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].minorCount").value(1))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].guardians[0].firstName").value("Petr"))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupSummaryResponseList[0].guardians[0].lastName").value("Novák"))
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
        @DisplayName("renders a minor whose member details are not found with null names")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
        void rendersMinorWithoutDetails() throws Exception {
            when(members.findByIds(anyCollection())).thenReturn(Map.of());
            when(legalGuardianGroupService.getGroup(new LegalGuardianGroupId(GROUP_UUID)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.minors[0].memberId").value(MINOR_ID))
                    .andExpect(jsonPath("$.minors[0].joinedAt").exists())
                    .andExpect(jsonPath("$.minors[0].firstName").doesNotExist())
                    .andExpect(jsonPath("$.minors[0].lastName").doesNotExist())
                    .andExpect(jsonPath("$.minors[0].registrationNumber").doesNotExist());
        }

        @Test
        @DisplayName("returns minors with links to their members and a link to the guardians")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
        void returnsGuardiansAndMinors() throws Exception {
            when(legalGuardianGroupService.getGroup(new LegalGuardianGroupId(GROUP_UUID)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(GROUP_UUID.toString()))
                    .andExpect(jsonPath("$.name").value("Novák"))
                    .andExpect(jsonPath("$.guardians").doesNotExist())
                    .andExpect(jsonPath("$._links.legalGuardians.href")
                            .value(containsString("/api/legal-guardian-groups/" + GROUP_UUID + "/guardians")))
                    .andExpect(jsonPath("$.minors[0].memberId").value(MINOR_ID))
                    .andExpect(jsonPath("$.minors[0].firstName").value("Tomáš"))
                    .andExpect(jsonPath("$.minors[0].lastName").value("Novák"))
                    .andExpect(jsonPath("$.minors[0].registrationNumber").value("ZBM1501"))
                    .andExpect(jsonPath("$.minors[0].joinedAt").exists())
                    .andExpect(jsonPath("$.minors[0]._links.member.href")
                            .value(containsString("/api/members/" + MINOR_ID)));
        }

        @Test
        @DisplayName("points guardian options to the legal guardian options endpoint")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void pointsGuardianOptionsToOptionsEndpoint() throws Exception {
            when(legalGuardianGroupService.getGroup(any(LegalGuardianGroupId.class)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

            mockMvc.perform(get("/api/legal-guardian-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.setLegalGuardianGroupGuardians.properties[?(@.name=='legalGuardians')].options.link.href")
                            .value(hasItem(containsString("/api/legal-guardian-options"))));
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
                            .value(containsString("/api/legal-guardian-groups/" + GROUP_UUID + "/legal-guardians")))
                    .andExpect(jsonPath("$._templates.default").doesNotExist())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("returns 403 to a minor of the group who lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MINOR_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenForMinorOfTheGroup() throws Exception {
            when(legalGuardianGroupService.getGroup(any(LegalGuardianGroupId.class)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));

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
    @DisplayName("PUT /api/legal-guardian-groups/{id}/legal-guardians")
    class SetGuardians {

        private static final String BODY = """
                {"legalGuardians": [{"userId": "%s"}, {"userId": "%s"}]}
                """.formatted(GUARDIAN_ID, OTHER_GUARDIAN_ID);

        private static final String NEW_GUARDIAN_BODY = """
                {"legalGuardians": [{"firstName": "Eva", "lastName": "Nováková",
                                     "email": "eva@example.com", "phone": "+420777111222"}]}
                """;

        @Test
        @DisplayName("returns 204 and passes the guardians to the service")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void setsGuardians() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isNoContent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<GuardianInput>> guardians = ArgumentCaptor.forClass(List.class);
            verify(legalGuardianGroupService).changeGroupGuardians(eq(new LegalGuardianGroupId(GROUP_UUID)), guardians.capture());
            assertThat(guardians.getValue()).containsExactly(
                    GuardianInput.existing(new UserId(UUID.fromString(GUARDIAN_ID))),
                    GuardianInput.existing(new UserId(UUID.fromString(OTHER_GUARDIAN_ID))));
        }

        @Test
        @DisplayName("passes a new guardian given inline to the service")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void setsNewGuardianInline() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(NEW_GUARDIAN_BODY))
                    .andExpect(status().isNoContent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<GuardianInput>> guardians = ArgumentCaptor.forClass(List.class);
            verify(legalGuardianGroupService).changeGroupGuardians(eq(new LegalGuardianGroupId(GROUP_UUID)), guardians.capture());
            assertThat(guardians.getValue()).containsExactly(GuardianInput.created(
                    new NewLegalGuardian("Eva", "Nováková", "eva@example.com", "+420777111222")));
        }

        @Test
        @DisplayName("returns 400 for an empty list of guardians")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsEmptyList() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
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
                    .when(legalGuardianGroupService).changeGroupGuardians(any(LegalGuardianGroupId.class), anyList());

            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when a guardian has neither userId nor new guardian details")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsGuardianWithoutUserId() throws Exception {
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
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
            mockMvc.perform(put("/api/legal-guardian-groups/{id}/legal-guardians", GROUP_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(legalGuardianGroupService);
        }
    }

    @Nested
    @DisplayName("GET /api/legal-guardian-groups/{id}/guardians")
    class ListGuardians {

        private void stubGroupWithContacts() {
            when(legalGuardianGroupService.getGroup(any(LegalGuardianGroupId.class)))
                    .thenReturn(groupOfMinor(GROUP_UUID, "Novák", GUARDIAN_ID, MINOR_ID));
            when(legalGuardianGroupService.listGuardians(any(LegalGuardianGroup.class))).thenReturn(List.of(
                    new GuardianContact(new UserId(UUID.fromString(GUARDIAN_ID)), "Petr", "Novák",
                            "petr@example.com", "+420777111222", GuardianKind.MEMBER),
                    new GuardianContact(new UserId(UUID.fromString(OTHER_GUARDIAN_ID)), "Eva", "Nováková",
                            "eva@example.com", null, GuardianKind.LEGAL_GUARDIAN)));
        }

        @Test
        @DisplayName("returns guardians with contacts and links to a member or a legal guardian profile")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
        void listsGuardiansForManager() throws Exception {
            stubGroupWithContacts();

            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupGuardianResponseList[0].userId").value(GUARDIAN_ID))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupGuardianResponseList[0].email").value("petr@example.com"))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupGuardianResponseList[0]._links.member.href")
                            .value(containsString("/api/members/" + GUARDIAN_ID)))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupGuardianResponseList[1]._links.legalGuardian.href")
                            .value(containsString("/api/legal-guardians/" + OTHER_GUARDIAN_ID)))
                    .andExpect(jsonPath("$._embedded.legalGuardianGroupGuardianResponseList[1]._links.member").doesNotExist());
        }

        @Test
        @DisplayName("is visible to a minor of the group without MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = MINOR_ID, authorities = {Authority.MEMBERS_READ})
        void listsGuardiansForMinorOfTheGroup() throws Exception {
            stubGroupWithContacts();

            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("is visible to a holder of profile editing over a minor of the group")
        @WithKlabisMockUser(memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = MINOR_ID))
        void listsGuardiansForHolderOfProfileEditingOverMinor() throws Exception {
            stubGroupWithContacts();

            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 403 to a holder of profile editing over a member who is not a minor of the group")
        @WithKlabisMockUser(memberId = OTHER_GUARDIAN_ID, authorities = {Authority.MEMBERS_READ},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = "dddddddd-dddd-dddd-dddd-dddddddddddd"))
        void forbiddenForHolderOverUnrelatedMember() throws Exception {
            stubGroupWithContacts();

            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 to a member who is not a minor of the group and lacks MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = OTHER_GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenForOtherMember() throws Exception {
            stubGroupWithContacts();

            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
            verify(legalGuardianGroupService, never()).listGuardians(any(LegalGuardianGroup.class));
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/legal-guardian-groups/{id}/guardians", GROUP_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/members/{id}/legal-guardians")
    class SetMemberLegalGuardians {

        private static final UUID MINOR_UUID = UUID.fromString(MINOR_ID);

        @Test
        @DisplayName("returns 204 and passes existing and new guardians to the service in one call")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void setsGuardiansOfMinor() throws Exception {
            mockMvc.perform(put("/api/members/{id}/legal-guardians", MINOR_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"legalGuardians": [{"userId": "%s"},
                                      {"firstName": "Eva", "lastName": "Nováková",
                                       "email": "eva@example.com", "phone": "+420777111222"}]}
                                    """.formatted(GUARDIAN_ID)))
                    .andExpect(status().isNoContent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<GuardianInput>> guardians = ArgumentCaptor.forClass(List.class);
            verify(legalGuardianGroupService).setGuardiansOf(eq(new MemberId(MINOR_UUID)), guardians.capture());
            assertThat(guardians.getValue()).containsExactly(
                    GuardianInput.existing(new UserId(UUID.fromString(GUARDIAN_ID))),
                    GuardianInput.created(new NewLegalGuardian("Eva", "Nováková", "eva@example.com", "+420777111222")));
        }

        @Test
        @DisplayName("returns 400 for an empty list, so the last guardian cannot be removed")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsEmptyList() throws Exception {
            mockMvc.perform(put("/api/members/{id}/legal-guardians", MINOR_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content("""
                                    {"legalGuardians": []}
                                    """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(legalGuardianGroupService);
        }

        @Test
        @DisplayName("returns 400 when the service refuses, e.g. the member is an adult")
        @WithKlabisMockUser(memberId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void rejectsWhenServiceRefuses() throws Exception {
            doThrow(new OnlyMinorsAllowedException(new MemberId(MINOR_UUID)))
                    .when(legalGuardianGroupService).setGuardiansOf(any(MemberId.class), anyList());

            mockMvc.perform(put("/api/members/{id}/legal-guardians", MINOR_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 403 without MEMBERS:MANAGE")
        @WithKlabisMockUser(memberId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void forbiddenWithoutManage() throws Exception {
            mockMvc.perform(put("/api/members/{id}/legal-guardians", MINOR_UUID)
                            .contentType("application/json")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(BODY))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(legalGuardianGroupService);
        }

        private static final String BODY = """
                {"legalGuardians": [{"userId": "%s"}]}
                """.formatted(GUARDIAN_ID);
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
