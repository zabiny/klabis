package club.klabis.users.infrastructure.restapi;

import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.users.application.ApplicationUserNotFound;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import club.klabis.users.domain.ApplicationUser;
import club.klabis.users.infrastructure.restapi.dto.MemberGrantsFormApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(openApiTagName = "User permissions")
public class UserPermissionsApiController {

    private final ConversionService conversionService;
    private final UserGrantsUpdateUseCase userGrantsUpdateUseCase;
    private final ApplicationUsersRepository applicationUsersRepository;

    public UserPermissionsApiController(ConversionService conversionService, UserGrantsUpdateUseCase userGrantsUpdateUseCase, ApplicationUsersRepository applicationUsersRepository) {
        this.conversionService = conversionService;
        this.userGrantsUpdateUseCase = userGrantsUpdateUseCase;
        this.applicationUsersRepository = applicationUsersRepository;
    }

    public record HalFormsOptionItem<T>(T value, String prompt) {

    }

    private HalFormsOptionItem<ApplicationGrant> fromGrant(ApplicationGrant grant) {
        return new HalFormsOptionItem<>(grant, grant.getDescription());
    }

    /**
     * GET /grants : returns details about available security grants what can be assigned to users
     *
     * @return List of grants which can be assigned to members (status code 200)
     */
    @Operation(
            operationId = "getAllGrants",
            summary = "returns details about available security grants what can be assigned to users"
    )
    @GetMapping(value = "/grants")
    public List<HalFormsOptionItem<ApplicationGrant>> getAllGrantsOptions() {
        return ApplicationGrant.globalGrants().stream().map(this::fromGrant).toList();
    }

    @Operation(
            operationId = "getUserGrants",
            summary = "returns grants assigned to member",
            description = "Requires `members:permissions` grant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Edit member grants form content", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = MemberGrantsFormApiDto.class))
                    })
            },
            security = {
                    @SecurityRequirement(name = "klabis", scopes = {"openid"})
            }
    )
    @GetMapping("/users/{userId}/changeGrantsForm")
    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    public ResponseEntity<MemberGrantsFormApiDto> getUserGrants(
            @Parameter(name = "userId", description = "ID of application user", required = true, in = ParameterIn.PATH) @PathVariable("userId") ApplicationUser.Id userId
    ) {
        ApplicationUser appUser = applicationUsersRepository.findById(userId)
                .orElseThrow(() -> ApplicationUserNotFound.forUserId(userId));

        return ResponseEntity.ok(new MemberGrantsFormApiDto(appUser.getGlobalGrants()));
    }

    @Operation(
            operationId = "updateUserGrants",
            summary = "updates grants assigned to user",
            description = "Requires `members:permissions` grant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User grants were successfully updated")
            }
    )
    @PutMapping("/users/{userId}/changeGrantsForm")
    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    public ResponseEntity<Void> updateMemberGrants(
            @Parameter(name = "userId", description = "ID of application user", required = true, in = ParameterIn.PATH) @PathVariable("userId") ApplicationUser.Id userId,
            @Parameter(name = "MemberGrantsFormApiDto", description = "") @Valid @RequestBody(required = false) MemberGrantsFormApiDto memberGrantsFormApiDto
    ) {
        userGrantsUpdateUseCase.setGlobalGrants(userId, memberGrantsFormApiDto.grants());
        return ResponseEntity.ok(null);
    }
}

@Component
class MemberGrantLinksPostprocessor implements RepresentationModelProcessor<EntityModel<MembersApiResponse>> {

    private final KlabisSecurityService securityService;

    MemberGrantLinksPostprocessor(KlabisSecurityService securityService) {
        this.securityService = securityService;
    }

    private HalFormsOptions getGrantsOptions() {
        return HalFormsOptions.remote(linkTo(methodOn(UserPermissionsApiController.class).getAllGrantsOptions()).withSelfRel());
    }

    private Affordance createChangeGrantsAffordance(ApplicationUser.Id appUserId) {
        return affordBetter(methodOn(UserPermissionsApiController.class).updateMemberGrants(appUserId, null),
                a -> a.defineOptions("grants", getGrantsOptions()));
    }

    @Override
    public EntityModel<MembersApiResponse> process(EntityModel<MembersApiResponse> model) {
        if (securityService.hasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)) {
            model.getContent().member().getAppUserId()
                    .ifPresent(appUserId -> model.mapLink(IanaLinkRelations.SELF,
                            link -> link.andAffordance(createChangeGrantsAffordance(appUserId))));
        }

        return model;
    }
}