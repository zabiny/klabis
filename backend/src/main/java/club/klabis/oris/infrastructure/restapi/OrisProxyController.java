package club.klabis.oris.infrastructure.restapi;

import club.klabis.members.domain.RegistrationNumber;
import club.klabis.oris.infrastructure.apiclient.OrisApiClient;
import club.klabis.oris.infrastructure.restapi.dto.ORISUserInfoApiDto;
import club.klabis.shared.ConversionService;
import club.klabis.shared.RFC7807ErrorResponseApiDto;
import club.klabis.shared.application.OrisIntegrationComponent;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.HttpClientErrorException;

@Tag(name = "ORIS", description = "Integration endpoints with ORIS - https://oris.orientacnisporty.cz/")
@ApiController(openApiTagName = "ORIS", securityScopes = {"oris"})
@OrisIntegrationComponent
class OrisProxyController {

    private final OrisApiClient orisApiClient;
    private final ConversionService conversionService;

    public OrisProxyController(OrisApiClient orisApiClient, ConversionService conversionService) {
        this.orisApiClient = orisApiClient;
        this.conversionService = conversionService;
    }

    @Operation(
            operationId = "orisUserInfoRegNumGet",
            summary = "Get information about user from ORIS",
            description = "#### Required authorization requires `members:register` grant ",
            tags = {"ORIS"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Available information about user read from ORIS", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ORISUserInfoApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ORISUserInfoApiDto.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Invalid user input", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class))
                    }),
                    @ApiResponse(responseCode = "404", description = "Missing required user authentication or authentication failed", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class)),
                            @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RFC7807ErrorResponseApiDto.class))
                    })
            }
    )
    @GetMapping("/oris/userInfo/{regNum}")
    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    public ResponseEntity<ORISUserInfoApiDto> orisUserInfoRegNumGet(
            @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$") @Parameter(name = "regNum", description = "Registration number of user to retrieve ORIS data about", required = true, in = ParameterIn.PATH) @PathVariable("regNum") String regNum
    ) {
        ORISUserInfoApiDto userInfoApiDto = conversionService.convert(orisApiClient.getUserInfo(regNum),
                ORISUserInfoApiDto.class);
        userInfoApiDto.setRegistrationNumber(RegistrationNumber.ofRegistrationId(regNum).toRegistrationId());
        return ResponseEntity.ok(userInfoApiDto);
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ErrorResponse handleOrisApiNotFoundException() {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setDetail("Member not found on ORIS");
        return new ErrorResponseException(HttpStatus.NOT_FOUND, detail, null);
    }
}
