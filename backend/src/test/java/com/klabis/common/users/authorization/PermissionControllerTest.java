package com.klabis.common.users.authorization;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.UserNotFoundException;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.common.users.infrastructure.restapi.PermissionController;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermissionController.class)
@Import(EncryptionConfiguration.class)
@DisplayName("PermissionController permissions endpoints tests")
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private FamilyGroupRepository familyGroupRepository;

    @MockitoBean
    private TrainingGroupRepository trainingGroupRepository;

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Nested
    @DisplayName("GET /api/users/{id}/permissions")
    class GetUserPermissions {

        @Test
        @DisplayName("should return 200 and permissions when user has MEMBERS:PERMISSIONS authority")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn200AndPermissionsWhenAuthorized() throws Exception {
            // Given
            when(permissionService.getUserPermissions(any(UserId.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(get("/api/users/{id}/permissions", USER_ID.uuid()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.authorities").isArray())
                    .andExpect(jsonPath("$.authorities", hasItems("MEMBERS:MANAGE")));
        }

        @Test
        @DisplayName("should include HATEOAS links in response")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldIncludeHateoasLinks() throws Exception {
            // Given
            when(permissionService.getUserPermissions(any(UserId.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(get("/api/users/{id}/permissions", USER_ID.uuid()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(containsString("/api/users/" + USER_ID.uuid().toString() + "/permissions")));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            when(permissionService.getUserPermissions(any(UserId.class)))
                    .thenThrow(new UserNotFoundException("User with ID " + USER_ID + " not found"));

            // When & Then
            mockMvc.perform(get("/api/users/{id}/permissions", USER_ID.uuid()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://klabis.com/problems/user-not-found"))
                    .andExpect(jsonPath("$.title").value("User Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value(containsString("not found")));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}/permissions")
    class UpdateUserPermissions {

        @Test
        @DisplayName("should return 204 No Content when authorized")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn204WhenAuthorized() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_MANAGE,
                            Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return Location header pointing to permissions resource")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturnLocationHeaderPointingToPermissionsResource() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_MANAGE,
                            Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string("Location",
                            containsString("/api/users/" + USER_ID.uuid() + "/permissions")));
        }

        @Test
        @DisplayName("should return 204 No Content with no response body")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn204WithNoBody() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("should return 409 when attempting to remove last admin")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn409WhenRemovingLastAdmin() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenThrow(new AuthorizationPolicy.AdminLockoutException(
                            "Cannot revoke MEMBERS:PERMISSIONS from user"));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("https://klabis.com/problems/admin-lockout"))
                    .andExpect(jsonPath("$.title").value("Admin Lockout Prevention"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value(containsString("Cannot revoke MEMBERS:PERMISSIONS")));
        }

        @Test
        @DisplayName("should return 400 for invalid authority")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn400ForInvalidAuthority() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_PERMISSIONS));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenThrow(new IllegalArgumentException(
                            "Invalid authority: INVALID:AUTHORITY. Valid authorities: [...]"));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://klabis.com/problems/invalid-authority"))
                    .andExpect(jsonPath("$.title").value("Invalid Request"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("Invalid authority")));
        }

        @Test
        @DisplayName("should return 400 for empty authorities")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn400ForEmptyAuthorities() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of());

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.authorities").value("must not be empty"));
        }

        @Test
        @DisplayName("should return 400 for invalid request body")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn400ForInvalidRequestBody() throws Exception {
            // When & Then - Missing authorities field
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should accept authorities in colon-format string (e.g. MEMBERS:READ)")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldAcceptColonFormatAuthorities() throws Exception {
            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ)));

            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"authorities\":[\"MEMBERS:READ\",\"EVENTS:READ\"]}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should accept GROUPS:TRAINING and return 204 when enabled")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldAcceptGroupsTrainingWhenEnabled() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.GROUPS_TRAINING, Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.GROUPS_TRAINING, Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should remove GROUPS:TRAINING when disabled (not in request authorities)")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldRemoveGroupsTrainingWhenDisabled() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ));

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(UserPermissions.create(USER_ID, Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ)));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(permissionService).updateUserPermissions(
                    any(UserId.class),
                    argThat(authorities -> !authorities.contains(Authority.GROUPS_TRAINING))
            );
        }
    }
}
