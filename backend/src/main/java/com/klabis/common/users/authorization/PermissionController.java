package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.AuthorizationPolicy;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for user permission management.
 * <p>
 * Provides endpoints for managing user permissions using the {@link PermissionService}.
 * Separated from authentication concerns - this controller only handles authorization (permissions).
 * <p>
 * Note: Must be public for Spring HATEOAS linkTo(methodOn(...)) pattern used in cross-module HATEOAS links.
 *
 * @see PermissionService
 * @see UserPermissions
 */
@RestController
@RequestMapping("/api/users")
@PrimaryAdapter
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
public class PermissionController {

    private final PermissionService permissionService;
    private final PermissionsResponseModelAssembler permissionsAssembler;

    public PermissionController(
            PermissionService permissionService,
            PermissionsResponseModelAssembler permissionsAssembler) {
        this.permissionService = permissionService;
        this.permissionsAssembler = permissionsAssembler;
    }

    /**
     * GET /api/users/{id}/permissions - Retrieve user permissions.
     *
     * @param id user ID
     * @return PermissionsResponse with user's authorities
     */
    @GetMapping("/{id}/permissions")
    @HasAuthority(Authority.MEMBERS_PERMISSIONS)
    public ResponseEntity<PermissionsResponseModel> getUserPermissions(@PathVariable UUID id) {
        PermissionsResponse response = permissionService.getUserPermissions(new UserId(id));
        PermissionsResponseModel model = permissionsAssembler.toModel(response);

        // Add conditional permissions link with affordance (only if authorized)
        if (hasMembersPermissionsAuthority()) {
            Link permissionsLink = klabisLinkTo(methodOn(PermissionController.class)
                    .updatePermissions(id, null))
                    .withRel("permissions")
                    .andAffordances(klabisAfford(methodOn(PermissionController.class).updatePermissions(id, null)));
            model.add(permissionsLink);
        }

        return ResponseEntity.ok(model);
    }

    /**
     * PUT /api/users/{id}/permissions - Update user permissions.
     *
     * @param id      user ID
     * @param request request with new authorities
     * @return updated PermissionsResponse
     */
    @PutMapping("/{id}/permissions")
    @HasAuthority(Authority.MEMBERS_PERMISSIONS)
    public ResponseEntity<PermissionsResponseModel> updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequest request) {

        UserPermissions updatedPermissions = permissionService.updateUserPermissions(new UserId(id),
                request.authorities());

        // Use actual saved authorities from updatedPermissions, not the request
        // This ensures the response reflects the validated/saved state, not what was requested
        PermissionsResponse response = new PermissionsResponse(
                updatedPermissions.getUserId(),
                updatedPermissions.getDirectAuthorities().stream().map(Authority::getValue).toList()
        );
        PermissionsResponseModel model = permissionsAssembler.toModel(response);

        return ResponseEntity.ok()
                .location(URI.create("/api/users/" + id + "/permissions"))
                .body(model);
    }

    /**
     * Exception handler for UserNotFoundException.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/user-not-found"));
        problemDetail.setTitle("User Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Exception handler for CannotRemoveLastPermissionManagerException.
     */
    @ExceptionHandler(CannotRemoveLastPermissionManagerException.class)
    public ResponseEntity<ProblemDetail> handleCannotRemoveLastPermissionManager(
            CannotRemoveLastPermissionManagerException ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/cannot-remove-last-admin"));
        problemDetail.setTitle("Admin Lockout Prevention");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Exception handler for AuthorizationPolicy.AdminLockoutException.
     */
    @ExceptionHandler(AuthorizationPolicy.AdminLockoutException.class)
    public ResponseEntity<ProblemDetail> handleAdminLockout(AuthorizationPolicy.AdminLockoutException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/admin-lockout"));
        problemDetail.setTitle("Admin Lockout Prevention");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Exception handler for IllegalArgumentException (invalid authorities).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        String type = ex.getMessage().contains("Invalid authority")
                ? "https://klabis.com/problems/invalid-authority"
                : "https://klabis.com/problems/invalid-request";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle("Invalid Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Check if current user has MEMBERS:PERMISSIONS authority.
     */
    private boolean hasMembersPermissionsAuthority() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("MEMBERS:PERMISSIONS"));
    }

    /**
     * Request DTO for updating permissions.
     */
    public record UpdatePermissionsRequest(
            @NotEmpty
            Set<Authority> authorities
    ) {
    }
}
