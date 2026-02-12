package com.klabis.members.management;

import com.klabis.members.Member;
import com.klabis.users.Authority;
import com.klabis.users.authorization.HasAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Member resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for member management.
 * Produces HAL+FORMS media type for hypermedia support.
 */
@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members", description = "Member registration and management API")
@SecurityRequirement(name = "OAuth2")
class RegistrationController {

    private final RegistrationService registrationService;
    private final EntityLinks entityLinks;

    public RegistrationController(RegistrationService registrationService, EntityLinks entityLinks) {
        this.registrationService = registrationService;
        this.entityLinks = entityLinks;
    }

    /**
     * Register a new member.
     * <p>
     * POST /api/members
     *
     * @param request registration request
     * @return 201 Created with Location header and member resource
     */
    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_CREATE)
    @Operation(
            summary = "Register a new member",
            description = "Creates a new member with personal information, contact details, and optional guardian information for minors. " +
                          "Automatically generates a unique registration number in format XXXYYSS (club code, birth year, sequence). " +
                          "Returns HATEOAS links for resource navigation."
    )
    @ApiResponse(responseCode = "201", description = "Member successfully registered")
    public ResponseEntity<EntityModel<MemberRegistrationResponse>> registerMember(
            @Parameter(description = "Member registration data including personal information, contacts, and optional guardian")
            @Valid @RequestBody RegisterMemberRequest request) {

        // Call service directly with request object
        UUID memberId = registrationService.registerMember(request);

        // Build response with HATEOAS links
        MemberRegistrationResponse response = new MemberRegistrationResponse(
                memberId,
                request.firstName(),
                request.lastName()
        );

        EntityModel<MemberRegistrationResponse> entityModel = EntityModel.of(response);

        // Add hypermedia links
        entityModel.add(
                entityLinks.linkToItemResource(Member.class, memberId).withSelfRel()
        );

        // Return 201 Created with Location header
        return ResponseEntity
                .created(entityLinks.linkToItemResource(Member.class, memberId).toUri())
                .body(entityModel);
    }

}
