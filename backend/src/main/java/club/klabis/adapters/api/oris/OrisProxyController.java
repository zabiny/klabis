package club.klabis.adapters.api.oris;

import club.klabis.adapters.api.HasGrant;
import club.klabis.adapters.oris.OrisApiClient;
import club.klabis.api.OrisApi;
import club.klabis.api.dto.ORISUserInfoApiDto;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.members.domain.RegistrationNumber;
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
        ORISUserInfoApiDto userInfoApiDto = conversionService.convert(orisApiClient.getUserInfo(regNum).data(), ORISUserInfoApiDto.class);
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
