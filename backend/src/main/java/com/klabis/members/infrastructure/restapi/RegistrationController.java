package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.ActingUser;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Member resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for member management.
 * Produces HAL+FORMS media type for hypermedia support.
 */
@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members", description = "Member registration and management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class RegistrationController implements RegistrationApi {

    private final RegistrationPort registrationService;
    private final MemberMapper memberMapper;

    public RegistrationController(RegistrationPort registrationService, MemberMapper memberMapper) {
        this.registrationService = registrationService;
        this.memberMapper = memberMapper;
    }

    /**
     * Register a new member.
     * <p>
     * POST /api/members
     *
     * @param request registration request
     * @param currentUserId the authenticated user performing the registration
     * @return 201 Created with Location header and member resource
     */
    @Operation(
            summary = "Register a new member",
            description = "Creates a new member with personal information, contact details, and optional guardian information for minors. " +
                          "Automatically generates a unique registration number in format XXXYYSS (club code, birth year, sequence)."
    )
    @ApiResponse(responseCode = "201", description = "Member successfully registered")
    @Override
    public ResponseEntity<Void> registerMember(
            @Parameter(description = "Member registration data including personal information, contacts, and optional guardian")
            @Valid @RequestBody RegisterMemberRequest request,
            @ActingUser UserId currentUserId) {

        RegistrationPort.RegisterNewMember serviceCommand = memberMapper.toRegisterNewMemberCommand(request, currentUserId);
        Member member = registrationService.registerMember(serviceCommand);

        ResponseEntity.BodyBuilder response = ResponseEntity
                .created(linkTo(methodOn(MemberController.class).getMember(member.getId().uuid(), null)).toUri());

        List<String> warnings = member.birthNumberConsistencyWarnings();
        if (!warnings.isEmpty()) {
            response.header("X-Warnings", warnings.toArray(String[]::new));
        }

        return response.build();
    }

}
