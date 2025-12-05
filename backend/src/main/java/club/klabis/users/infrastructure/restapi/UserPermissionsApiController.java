package club.klabis.users.infrastructure.restapi;

import club.klabis.shared.ConversionService;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.users.application.ApplicationUserNotFound;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import club.klabis.users.domain.ApplicationUser;
import club.klabis.users.infrastructure.restapi.dto.GetAllGrants200ResponseApiDto;
import club.klabis.users.infrastructure.restapi.dto.GlobalGrantDetailApiDto;
import club.klabis.users.infrastructure.restapi.dto.MemberGrantsFormApiDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.List;

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

    /**
     * GET /grants : returns details about available security grants what can be assigned to users
     *
     * @return List of grants which can be assigned to members (status code 200)
     */
    @Operation(
            operationId = "getAllGrants",
            summary = "returns details about available security grants what can be assigned to users",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of grants which can be assigned to members", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = GetAllGrants200ResponseApiDto.class))
                    })
            }
    )
    @GetMapping(
            value = "/grants",
            produces = {"application/json"}
    )
    public ResponseEntity<GetAllGrants200ResponseApiDto> getAllGrants() {
        Collection<ApplicationGrant> globalGrants = ApplicationGrant.globalGrants();
        List<GlobalGrantDetailApiDto> convertedGrants = conversionService.convert(
                globalGrants,
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(GlobalGrantDetailApiDto.class)));
        return ResponseEntity.ok(GetAllGrants200ResponseApiDto.builder().grants(convertedGrants).build());
    }

    public record HalFormsOptionItem(ApplicationGrant value, String prompt) {

    }

    private HalFormsOptionItem fromGrant(ApplicationGrant grant) {
        return new HalFormsOptionItem(grant, grant.getDescription());
    }

    @GetMapping(value = "/grant_options")
    public List<HalFormsOptionItem> getAllGrantsOptions() {
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

        return ResponseEntity.ok(conversionService.convert(appUser,
                MemberGrantsFormApiDto.class));
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
        Collection<ApplicationGrant> globalGrants = conversionService.convert(
                memberGrantsFormApiDto.getGrants(),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(ApplicationGrant.class)));
        userGrantsUpdateUseCase.setGlobalGrants(userId, globalGrants);
        return ResponseEntity.ok(null);
    }
}
