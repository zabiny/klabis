package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.ActingUser;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Member;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
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
class RegistrationController implements RegistrationApi {

    private final RegistrationPort registrationService;
    private final ConversionService conversionService;

    public RegistrationController(RegistrationPort registrationService, ConversionService conversionService) {
        this.registrationService = registrationService;
        this.conversionService = conversionService;
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
    @Override
    public ResponseEntity<Void> registerMember(
            RegisterMemberRequest request,
            @ActingUser UserId currentUserId) {

        RegistrationPort.RegisterNewMember serviceCommand = conversionService.convert(
                new RegisterMemberRequestWithParameters(request, currentUserId), RegistrationPort.RegisterNewMember.class);
        Member member = registrationService.registerMember(serviceCommand);

        ResponseEntity.BodyBuilder response = ResponseEntity
                .created(linkTo(methodOn(MembersApi.class).getMember(member.getId().uuid(), null)).toUri());

        List<String> warnings = member.birthNumberConsistencyWarnings();
        if (!warnings.isEmpty()) {
            response.header("X-Warnings", warnings.toArray(String[]::new));
        }

        return response.build();
    }

}
