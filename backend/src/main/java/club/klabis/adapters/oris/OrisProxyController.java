package club.klabis.adapters.oris;

import club.klabis.api.OrisApi;
import club.klabis.api.dto.ORISUserInfoApiDto;
import club.klabis.domain.members.RegistrationNumber;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrisProxyController implements OrisApi {

    private final OrisService orisService;
    private final ConversionService conversionService;

    public OrisProxyController(OrisService orisService, ConversionService conversionService) {
        this.orisService = orisService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ORISUserInfoApiDto> orisUserInfoRegNumGet(String regNum) {
        ORISUserInfoApiDto userInfoApiDto = conversionService.convert(orisService.getUserInfo(regNum).data(), ORISUserInfoApiDto.class);
        userInfoApiDto.setRegistrationNumber(RegistrationNumber.ofRegistrationId(regNum).toRegistrationId());
        return ResponseEntity.ok(userInfoApiDto);
    }

    @ExceptionHandler(MismatchedInputException.class)
    public ErrorResponse handleMismatchInputException(MismatchedInputException e) throws MismatchedInputException {
        // TODO: ORIS returns HTTP 200 with Data containing empty array instead of object when member is not found... :( Any better way how to detect that?
        if ("Cannot deserialize value of type `club.klabis.adapters.oris.OrisService$OrisUserInfo` from Array value (token `JsonToken.START_ARRAY`)".equals(e.getOriginalMessage())) {
            ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            detail.setDetail("Member not found on ORIS (probably)");
            return new ErrorResponseException(HttpStatus.NOT_FOUND, detail, null);
        } else {
            throw e;
        }
    }
}
