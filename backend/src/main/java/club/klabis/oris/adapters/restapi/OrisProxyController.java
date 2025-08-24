package club.klabis.oris.adapters.restapi;

import club.klabis.members.domain.RegistrationNumber;
import club.klabis.oris.adapters.restapi.dto.ORISUserInfoApiDto;
import club.klabis.oris.application.apiclient.OrisApiClient;
import club.klabis.shared.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
class OrisProxyController implements OrisApi {

    private final OrisApiClient orisApiClient;
    private final ConversionService conversionService;

    public OrisProxyController(OrisApiClient orisApiClient, ConversionService conversionService) {
        this.orisApiClient = orisApiClient;
        this.conversionService = conversionService;
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<ORISUserInfoApiDto> orisUserInfoRegNumGet(String regNum) {
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
