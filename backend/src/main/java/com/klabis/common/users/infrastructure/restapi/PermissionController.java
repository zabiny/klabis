package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.CannotRemoveLastPermissionManagerException;
import com.klabis.common.users.domain.UserNotFoundException;
import com.klabis.common.users.domain.UserPermissions;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
@ExposesResourceFor(UserPermissions.class)
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
        UserPermissions permissions = permissionService.getUserPermissions(new UserId(id));
        PermissionsResponse response = toPermissionsResponse(permissions);
        PermissionsResponseModel model = permissionsAssembler.toModel(response);

        Link selfLink = klabisLinkTo(methodOn(PermissionController.class)
                .getUserPermissions(id))
                .withSelfRel()
                .andAffordances(klabisAfford(methodOn(PermissionController.class).updatePermissions(id, null)));
        model.add(selfLink);

        return ResponseEntity.ok(model);
    }

    private PermissionsResponse toPermissionsResponse(UserPermissions permissions) {
        return new PermissionsResponse(
                permissions.getUserId(),
                permissions.getDirectAuthorities().stream().map(Authority::getValue).toList()
        );
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
    public ResponseEntity<Void> updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequest request) {

        permissionService.updateUserPermissions(new UserId(id), request.authorities());

        URI location = klabisLinkTo(methodOn(PermissionController.class).getUserPermissions(id)).toUri();
        return ResponseEntity.noContent().location(location).build();
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
     * Request DTO for updating permissions.
     */
    public record UpdatePermissionsRequest(
            @NotEmpty
            Set<Authority> authorities
    ) {
    }
}
