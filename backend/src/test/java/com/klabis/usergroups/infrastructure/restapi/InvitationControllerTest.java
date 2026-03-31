package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupNotFoundException;
import com.klabis.usergroups.application.InvitationPort;
import com.klabis.usergroups.application.NotGroupOwnerException;
import com.klabis.usergroups.application.NotInvitedMemberException;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.Invitation;
import com.klabis.usergroups.domain.InvitationId;
import com.klabis.usergroups.domain.InvitationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("InvitationController API tests")
@WebMvcTest(controllers = {InvitationController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class InvitationControllerTest {

    private static final String OWNER_MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String INVITED_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID INVITATION_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UserGroupId GROUP_ID = new UserGroupId(GROUP_UUID);
    private static final InvitationId INVITATION_ID = new InvitationId(INVITATION_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationPort invitationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("POST /api/groups/{groupId}/invitations")
    class InviteMemberTests {

        @Test
        @DisplayName("should return 204 when owner invites a member")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldInviteMemberAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(INVITED_MEMBER_ID))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-owner tries to invite")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldReturn400WhenNonOwnerInvites() throws Exception {
            MemberId nonOwner = new MemberId(UUID.fromString(OWNER_MEMBER_ID));
            doThrow(new NotGroupOwnerException(nonOwner, GROUP_ID))
                    .when(invitationService).inviteMember(any(UserGroupId.class), eq(nonOwner), any(MemberId.class));

            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(INVITED_MEMBER_ID))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when memberId is null")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldReturn400WhenMemberIdIsNull() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(INVITED_MEMBER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(INVITED_MEMBER_ID))
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException(GROUP_ID))
                    .when(invitationService).inviteMember(any(), any(), any());

            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(INVITED_MEMBER_ID))
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{groupId}/invitations/{invitationId}/accept")
    class AcceptInvitationTests {

        @Test
        @DisplayName("should return 204 when invited member accepts invitation")
        @WithKlabisMockUser(memberId = INVITED_MEMBER_ID)
        void shouldAcceptInvitationAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-invited member tries to accept")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldReturn400WhenNonInvitedMemberAccepts() throws Exception {
            MemberId notInvited = new MemberId(UUID.fromString(OWNER_MEMBER_ID));
            doThrow(new NotInvitedMemberException(notInvited, INVITATION_ID))
                    .when(invitationService).acceptInvitation(any(), any(), eq(notInvited));

            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 when group not found")
        @WithKlabisMockUser(memberId = INVITED_MEMBER_ID)
        void shouldReturn404WhenGroupNotFound() throws Exception {
            doThrow(new GroupNotFoundException(GROUP_ID))
                    .when(invitationService).acceptInvitation(any(), any(), any());

            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/accept",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{groupId}/invitations/{invitationId}/reject")
    class RejectInvitationTests {

        @Test
        @DisplayName("should return 204 when invited member rejects invitation")
        @WithKlabisMockUser(memberId = INVITED_MEMBER_ID)
        void shouldRejectInvitationAndReturn204() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when non-invited member tries to reject")
        @WithKlabisMockUser(memberId = OWNER_MEMBER_ID)
        void shouldReturn400WhenNonInvitedMemberRejects() throws Exception {
            MemberId notInvited = new MemberId(UUID.fromString(OWNER_MEMBER_ID));
            doThrow(new NotInvitedMemberException(notInvited, INVITATION_ID))
                    .when(invitationService).rejectInvitation(any(), any(), eq(notInvited));

            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/groups/{groupId}/invitations/{invitationId}/reject",
                                    GROUP_UUID, INVITATION_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/invitations/pending")
    class GetPendingInvitationsTests {

        @Test
        @DisplayName("should return 200 with list of groups with pending invitations")
        @WithKlabisMockUser(memberId = INVITED_MEMBER_ID)
        void shouldReturnPendingInvitations() throws Exception {
            MemberId owner = new MemberId(UUID.fromString(OWNER_MEMBER_ID));
            MemberId invitedMember = new MemberId(UUID.fromString(INVITED_MEMBER_ID));
            Invitation invitation = new Invitation(INVITATION_ID, invitedMember, owner, InvitationStatus.PENDING, Instant.now());
            FreeGroup group = FreeGroup.reconstruct(GROUP_ID, "Trail Runners",
                    Set.of(owner), Set.of(), Set.of(invitation), null);
            when(invitationService.getGroupsWithPendingInvitations(any(MemberId.class)))
                    .thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.pendingInvitationResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.pendingInvitationResponseList[0].groupId").value(GROUP_UUID.toString()))
                    .andExpect(jsonPath("$._embedded.pendingInvitationResponseList[0].groupName").value("Trail Runners"))
                    .andExpect(jsonPath("$._embedded.pendingInvitationResponseList[0].invitationId").value(INVITATION_UUID.toString()));
        }

        @Test
        @DisplayName("should return 200 with empty list when no pending invitations")
        @WithKlabisMockUser(memberId = INVITED_MEMBER_ID)
        void shouldReturnEmptyListWhenNoPendingInvitations() throws Exception {
            when(invitationService.getGroupsWithPendingInvitations(any(MemberId.class)))
                    .thenReturn(List.of());

            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when user has no member profile")
        @WithKlabisMockUser
        void shouldReturn403WhenNoMemberProfile() throws Exception {
            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/invitations/pending")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }
}
