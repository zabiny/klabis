package com.klabis.members.management;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.users.authorization.PermissionController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.linkToIfAuthorized;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * HATEOAS processor that adds conditional permissions link to Member detail responses.
 * <p>
 * This is a cross-module concern where members module needs to link to users module's
 * permission endpoint. The link is only added if the authenticated user has
 * MEMBERS:PERMISSIONS authority.
 * <p>
 * <b>Design rationale:</b> Uses existing member.id field (which is UserId due to 1:1
 * Member-User relationship) without adding redundant userId field to DTOs.
 *
 * @see MemberDetailsResponse
 * @see PermissionController
 */
@MvcComponent
class MemberPermissionsLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberDetailsResponse response = model.getContent();
        if (response == null) {
            return model;
        }

        UUID memberId = response.id();
        if (memberId == null) {
            return model;
        }

        if (hasMembersPermissionsAuthority()) {
            Link permissionsLink = linkToIfAuthorized(methodOn(PermissionController.class)
                    .getUserPermissions(memberId))
                    .withRel("permissions");
            model.add(permissionsLink);
        }

        return model;
    }

    /**
     * Checks if the current user has MEMBERS:PERMISSIONS authority.
     *
     * @return true if authenticated user has MEMBERS:PERMISSIONS, false otherwise
     */
    private boolean hasMembersPermissionsAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("MEMBERS:PERMISSIONS"));
    }
}
