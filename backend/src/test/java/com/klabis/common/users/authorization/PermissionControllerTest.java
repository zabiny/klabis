package com.klabis.common.users.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.klabis.common.users.infrastructure.restapi.PermissionsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
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

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Nested
    @DisplayName("GET /api/users/{id}/permissions")
    class GetUserPermissions {

        @Test
        @DisplayName("should return 200 and permissions when user has MEMBERS:PERMISSIONS authority")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn200AndPermissionsWhenAuthorized() throws Exception {
            // Given
            PermissionsResponse response = new PermissionsResponse(
                    USER_ID,
                    List.of("MEMBERS:CREATE", "MEMBERS:READ")
            );
            when(permissionService.getUserPermissions(any(UserId.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/{id}/permissions", USER_ID.uuid()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.authorities").isArray())
                    .andExpect(jsonPath("$.authorities", hasItems("MEMBERS:CREATE", "MEMBERS:READ")));
        }

        @Test
        @DisplayName("should include HATEOAS links in response")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldIncludeHateoasLinks() throws Exception {
            // Given
            PermissionsResponse response = new PermissionsResponse(
                    USER_ID,
                    List.of("MEMBERS:READ")
            );
            when(permissionService.getUserPermissions(any(UserId.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/users/{id}/permissions", USER_ID.uuid()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(containsString("/api/users/" + USER_ID.uuid().toString() + "/permissions")))
                    .andExpect(jsonPath("$._links.permissions.href").exists());
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
        @DisplayName("should return 200 and updated permissions when authorized")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturn200AndUpdatedPermissionsWhenAuthorized() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_CREATE,
                            Authority.MEMBERS_READ));

            UserPermissions updatedPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_CREATE, Authority.MEMBERS_READ)
            );
            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(updatedPermissions);

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.authorities").isArray())
                    .andExpect(jsonPath("$.authorities", hasItems("MEMBERS:CREATE", "MEMBERS:READ")));
        }

        @Test
        @DisplayName("should include HATEOAS links in response")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldIncludeHateoasLinks() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_CREATE,
                            Authority.MEMBERS_READ));

            UserPermissions updatedPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_CREATE, Authority.MEMBERS_READ)
            );
            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(updatedPermissions);

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should return Location header")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_PERMISSIONS})
        void shouldReturnLocationHeader() throws Exception {
            // Given
            PermissionController.UpdatePermissionsRequest request =
                    new PermissionController.UpdatePermissionsRequest(Set.of(Authority.MEMBERS_READ));

            UserPermissions updatedPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_READ)
            );
            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenReturn(updatedPermissions);

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location",
                            containsString("/api/users/" + USER_ID.uuid() + "/permissions")));
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

            when(permissionService.updateUserPermissions(any(UserId.class), any(Set.class)))
                    .thenThrow(new IllegalArgumentException("At least one authority required"));

            // When & Then
            mockMvc.perform(put("/api/users/{id}/permissions", USER_ID.uuid())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
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
    }
}
