package club.klabis.oris.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.oris.application.OrisEventsImporter;
import club.klabis.oris.application.dto.OrisEventListFilter;
import club.klabis.oris.infrastructure.apiclient.OrisApiClient;
import club.klabis.oris.infrastructure.restapi.dto.ORISUserInfoApiDto;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@RestController
class OrisProxyController implements OrisApi {

    private final OrisApiClient orisApiClient;
    private final OrisEventsImporter orisEventsImporter;
    private final ConversionService conversionService;

    public OrisProxyController(OrisApiClient orisApiClient, OrisEventsImporter orisEventsImporter, ConversionService conversionService) {
        this.orisApiClient = orisApiClient;
        this.orisEventsImporter = orisEventsImporter;
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

    @HasGrant(ApplicationGrant.SYSTEM_ADMIN)
    @Override
    public ResponseEntity<Void> synchronizeAllEventsWithOris() {
        orisEventsImporter.loadOrisEvents(OrisEventListFilter.createDefault());
        return ResponseEntity.ok(null);
    }

    @HasGrant(ApplicationGrant.SYSTEM_ADMIN)
    @Override
    public ResponseEntity<Void> synchronizeEventWithOris(Event.Id eventId) {
        orisEventsImporter.synchronizeEvents(List.of(eventId));
        return ResponseEntity.ok(null);
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ErrorResponse handleOrisApiNotFoundException() {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setDetail("Member not found on ORIS");
        return new ErrorResponseException(HttpStatus.NOT_FOUND, detail, null);
    }
}
