package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.CannotRemoveLastPermissionManagerException;
import com.klabis.common.users.domain.UserNotFoundException;
import com.klabis.common.users.domain.UserPermissions;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
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
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(UserPermissions.class)
public class PermissionController implements PermissionsApi {

    private final PermissionService permissionService;
    private final ConversionService conversionService;

    public PermissionController(PermissionService permissionService, ConversionService conversionService) {
        this.permissionService = permissionService;
        this.conversionService = conversionService;
    }

    /**
     * GET /api/users/{id}/permissions - Retrieve user permissions.
     *
     * @param id user ID
     * @return PermissionsResponse with user's authorities
     */
    @Override
    public ResponseEntity<PermissionsResponse> getUserPermissions(UUID id) {
        UserPermissions permissions = permissionService.getUserPermissions(new UserId(id));
        PermissionsResponse response = toPermissionsResponse(permissions);
        HalResponseContext.setDomain(permissions);
        return ResponseEntity.ok(response);
    }

    private PermissionsResponse toPermissionsResponse(UserPermissions permissions) {
        return new PermissionsResponse(
                permissions.getManageableAuthorities().stream().map(Authority::getValue).toList(),
                permissions.getUserId().uuid()
        );
    }

    /**
     * PUT /api/users/{id}/permissions - Update user permissions.
     *
     * @param id      user ID
     * @param request request with new authorities
     * @return updated PermissionsResponse
     */
    @Override
    public ResponseEntity<Void> updatePermissions(UUID id, UpdatePermissionsRequest request) {

        @SuppressWarnings("unchecked")
        Set<com.klabis.common.users.Authority> authorities = (Set<com.klabis.common.users.Authority>) conversionService.convert(
                request.authorities(),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Authority.class)),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(com.klabis.common.users.Authority.class)));

        permissionService.updateUserPermissions(new UserId(id), authorities);

        URI location = klabisLinkTo(methodOn(PermissionsApi.class).getUserPermissions(id))
                .map(link -> URI.create(link.toUri().toString()))
                .orElseGet(() -> URI.create("/api/users/" + id + "/permissions"));
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

}

@MvcComponent
class PermissionsDetailsPostprocessor extends ModelWithDomainPostprocessor<PermissionsResponse, UserPermissions> {

    @Override
    public void process(EntityModel<PermissionsResponse> dtoModel, UserPermissions permissions) {
        klabisLinkTo(methodOn(PermissionsApi.class).getUserPermissions(permissions.getUserId().uuid()))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(PermissionsApi.class)
                                .updatePermissions(permissions.getUserId().uuid(), null))))
                .ifPresent(dtoModel::add);
    }
}
