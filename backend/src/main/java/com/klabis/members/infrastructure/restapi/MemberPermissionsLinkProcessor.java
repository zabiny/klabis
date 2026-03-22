package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.infrastructure.restapi.PermissionController;
import com.klabis.members.MemberId;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static com.klabis.members.infrastructure.restapi.MemberPermissionsLinkHelper.addPermissionsLinkIfAuthorized;
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

        if (response.active()) {
            addPermissionsLinkIfAuthorized(model, response.id());
        }

        return model;
    }
}

/**
 * HATEOAS processor that adds conditional permissions link to Member summary responses in list views.
 * <p>
 * Applies the same permissions-link logic as {@link MemberPermissionsLinkProcessor} but for
 * summary items returned by GET /api/members. The link is added only for active members
 * when the authenticated user has MEMBERS:PERMISSIONS authority.
 * <p>
 * Note: This processor runs BEFORE Jackson serialization, so {@code response.active()} returns
 * the real value even though the field has {@code @HasAuthority(MEMBERS_MANAGE)} annotation.
 * Field-level security redaction happens later during JSON serialization.
 *
 * @see MemberSummaryResponse
 * @see MemberPermissionsLinkProcessor
 */
@MvcComponent
class MemberSummaryPermissionsLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberSummaryResponse>> {

    @Override
    public EntityModel<MemberSummaryResponse> process(EntityModel<MemberSummaryResponse> model) {
        MemberSummaryResponse response = model.getContent();
        if (response == null) {
            return model;
        }

        // active() returns real value here — processor runs before Jackson field-level security redaction
        if (Boolean.TRUE.equals(response.active())) {
            addPermissionsLinkIfAuthorized(model, response.id());
        }

        return model;
    }
}

/**
 * Shared helper for adding permissions link to member entity models.
 */
final class MemberPermissionsLinkHelper {

    private MemberPermissionsLinkHelper() {}

    static void addPermissionsLinkIfAuthorized(EntityModel<?> model, MemberId memberId) {
        if (memberId == null) {
            return;
        }
        UUID uuid = memberId.uuid();
        if (uuid == null) {
            return;
        }

        if (hasMembersPermissionsAuthority()) {
            Link permissionsLink = klabisLinkTo(methodOn(PermissionController.class)
                    .getUserPermissions(uuid))
                    .withRel("permissions");
            model.add(permissionsLink);
        }
    }

    private static boolean hasMembersPermissionsAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("MEMBERS:PERMISSIONS"));
    }
}
