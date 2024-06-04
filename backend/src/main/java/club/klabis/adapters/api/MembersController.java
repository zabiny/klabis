package club.klabis.adapters.api;

import club.klabis.api.MembersApi;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.api.dto.MemberViewCompactApiDto;
import club.klabis.api.dto.MembersListApiDto;
import club.klabis.api.dto.MembersListItemsInnerApiDto;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberService;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
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

    @Override
    public ResponseEntity<MembersListApiDto> membersGet(String view, Boolean suspended) {
        List<? extends MembersListItemsInnerApiDto> result = service.findAll(suspended).stream().map(t -> convertToApiDto(t, view)).toList();
        return ResponseEntity.ok(MembersListApiDto.builder().items((List<MembersListItemsInnerApiDto>) result).build());
    }

    private MembersListItemsInnerApiDto convertToApiDto(Member item, String view) {
        if ("full".equalsIgnoreCase(view)) {
            return conversionService.convert(item, MemberApiDto.class);
        } else {
            return conversionService.convert(item, MemberViewCompactApiDto.class);
        }
    }
}
