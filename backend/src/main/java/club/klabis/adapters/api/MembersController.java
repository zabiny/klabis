package club.klabis.adapters.api;

import club.klabis.api.MembersApi;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.api.dto.MembersListApiDto;
import club.klabis.api.dto.MembersListItemsInnerApiDto;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberService;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MembersController implements MembersApi {

    private final MemberService service;
    private final ConversionService conversionService;

    public MembersController(MemberService service, ConversionService conversionService) {
        this.service = service;
        this.conversionService = conversionService;
    }

    private ProblemDetail memberNotFoundProblemDetail(Integer memberId) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Member id %s not found".formatted(memberId));
    }

    @Override
    public ResponseEntity<MemberApiDto> membersMemberIdGet(Integer memberId) {
        return service.findById(memberId)
                .map(m -> mapToResponseEntity(m, MemberApiDto.class))
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND, memberNotFoundProblemDetail(memberId), null));
    }

    @Override
    public ResponseEntity<MembersListApiDto> membersGet(String view, Boolean suspended) {
        List<? extends MembersListItemsInnerApiDto> result = service.findAll(suspended).stream().map(t -> convertToApiDto(t, view)).toList();
        return ResponseEntity.ok(MembersListApiDto.builder().items((List<MembersListItemsInnerApiDto>) result).build());
    }

    private <T> ResponseEntity<T> mapToResponseEntity(Object data, Class<T> apiDtoType) {
        T payload = conversionService.convert(data, apiDtoType);
        return ResponseEntity.ok(payload);
    }

    private MembersListItemsInnerApiDto convertToApiDto(Member item, String view) {
        if ("full".equalsIgnoreCase(view)) {
            return conversionService.convert(item, MemberApiDto.class);
        } else {
            return conversionService.convert(item, MemberViewCompactApiDto.class);
        }
    }
}
