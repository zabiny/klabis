package club.klabis.adapters.api;

import club.klabis.api.MembersApi;
import club.klabis.api.dto.*;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserNotFound;
import club.klabis.domain.appusers.ApplicationUserService;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberNotFoundException;
import club.klabis.domain.members.MemberService;
import club.klabis.domain.members.forms.MemberEditForm;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class MembersController implements MembersApi {

    private final MemberService service;
    private final ApplicationUserService applicationUserService;
    private final ConversionService conversionService;

    public MembersController(MemberService service, ApplicationUserService applicationUserService, ConversionService conversionService) {
        this.service = service;
        this.applicationUserService = applicationUserService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<MemberEditFormApiDto> membersMemberIdEditMemberInfoFormGet(Integer memberId) {
        return service.findById(memberId)
                .map(m -> mapToResponseEntity(m, MemberEditFormApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdEditMemberInfoFormPut(Integer memberId, MemberEditFormApiDto memberEditFormApiDto) {
        service.editMember(memberId, conversionService.convert(memberEditFormApiDto, MemberEditForm.class));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<MemberApiDto> membersMemberIdGet(Integer memberId) {
        return service.findById(memberId)
                .map(m -> mapToResponseEntity(m, MemberApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Override
    public ResponseEntity<MembersListApiDto> membersGet(String view, Boolean suspended) {
        List<? extends MembersListItemsInnerApiDto> result = service.findAll(suspended).stream().map(t -> convertToApiDto(t, view)).toList();
        return ResponseEntity.ok(MembersListApiDto.builder().items((List<MembersListItemsInnerApiDto>) result).build());
    }

    @Override
    public ResponseEntity<MembershipSuspensionInfoApiDto> membersMemberIdSuspendMembershipFormGet(Integer memberId) {
        return service.getSuspensionInfoForMember(memberId)
                .map(d -> mapToResponseEntity(d, MembershipSuspensionInfoApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdSuspendMembershipFormPost(Integer memberId, Boolean force) {
        service.suspendMembershipForMember(memberId, force);
        return ResponseEntity.ok(null);
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

    @Override
    public ResponseEntity<MemberGrantsFormApiDto> getMemberGrants(Integer memberId) {
        ApplicationUser appUser = applicationUserService.getApplicationUserForMemberId(memberId);

        return ResponseEntity.ok(conversionService.convert(appUser, MemberGrantsFormApiDto.class));
    }

    @Override
    public ResponseEntity<Void> updateMemberGrants(Integer memberId, MemberGrantsFormApiDto memberGrantsFormApiDto) {
        Collection<ApplicationGrant> globalGrants = (Collection<ApplicationGrant>) conversionService.convert(memberGrantsFormApiDto.getGrants(), TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(ApplicationGrant.class)));
        applicationUserService.setGlobalGrants(memberId, globalGrants);
        return ResponseEntity.ok(null);
    }

}
