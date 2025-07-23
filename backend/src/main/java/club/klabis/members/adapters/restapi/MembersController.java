package club.klabis.members.adapters.restapi;

import club.klabis.config.security.HasGrant;
import club.klabis.api.MembersApi;
import club.klabis.api.dto.*;
import club.klabis.members.application.EditMemberInfoUseCase;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.application.MembershipSuspendUseCase;
import club.klabis.users.application.ApplicationUserNotFound;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.domain.forms.MemberEditForm;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class MembersController implements MembersApi {

    private final MembersRepository membersRepository;
    private final MembershipSuspendUseCase membershipSuspendUseCase;
    private final EditMemberInfoUseCase editMemberUseCase;
    private final ConversionService conversionService;
    private final UserGrantsUpdateUseCase userGrantsUpdateUseCase;
    private final ApplicationUsersRepository applicationUsersRepository;

    public MembersController(MembersRepository membersRepository, MembershipSuspendUseCase membershipSuspendUseCase, EditMemberInfoUseCase editMemberUseCase, ConversionService conversionService, UserGrantsUpdateUseCase userGrantsUpdateUseCase, ApplicationUsersRepository applicationUsersRepository) {
        this.membersRepository = membersRepository;
        this.membershipSuspendUseCase = membershipSuspendUseCase;
        this.editMemberUseCase = editMemberUseCase;
        this.conversionService = conversionService;
        this.userGrantsUpdateUseCase = userGrantsUpdateUseCase;
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<MemberEditFormApiDto> membersMemberIdEditMemberInfoFormGet(Integer memberId) {
        return membersRepository.findById(new Member.Id(memberId))
                .map(m -> mapToResponseEntity(m, MemberEditFormApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new Member.Id(memberId)));
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<Void> membersMemberIdEditMemberInfoFormPut(Integer memberId, MemberEditFormApiDto memberEditFormApiDto) {
        editMemberUseCase.editMember(new Member.Id(memberId),
                conversionService.convert(memberEditFormApiDto, MemberEditForm.class));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<MemberApiDto> membersMemberIdGet(Integer memberId) {
        return membersRepository.findById(new Member.Id(memberId))
                .map(m -> mapToResponseEntity(m, MemberApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new Member.Id(memberId)));
    }

    @Override
    public ResponseEntity<MembersListApiDto> membersGet(String view, Boolean suspended) {
        List<? extends MembersListItemsInnerApiDto> result = membersRepository.findAll(suspended)
                .stream()
                .map(t -> convertToApiDto(t, view))
                .toList();
        return ResponseEntity.ok(MembersListApiDto.builder().items((List<MembersListItemsInnerApiDto>) result).build());
    }

    @Override
    public ResponseEntity<MembershipSuspensionInfoApiDto> membersMemberIdSuspendMembershipFormGet(Integer memberId) {
        return membershipSuspendUseCase.getSuspensionInfoForMember(new Member.Id(memberId))
                .map(d -> mapToResponseEntity(d, MembershipSuspensionInfoApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new Member.Id(memberId)));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdSuspendMembershipFormPut(Integer memberId, Boolean force) {
        membershipSuspendUseCase.suspendMembershipForMember(new Member.Id(memberId), force);
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

    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<MemberGrantsFormApiDto> getMemberGrants(Integer memberIdValue) {
        Member.Id memberId = new Member.Id(memberIdValue);
        ApplicationUser appUser = applicationUsersRepository.findByMemberId(memberId).orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));

        return ResponseEntity.ok(conversionService.convert(appUser, MemberGrantsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.APPUSERS_PERMISSIONS)
    @Override
    public ResponseEntity<Void> updateMemberGrants(Integer memberId, MemberGrantsFormApiDto memberGrantsFormApiDto) {
        Collection<ApplicationGrant> globalGrants = (Collection<ApplicationGrant>) conversionService.convert(
                memberGrantsFormApiDto.getGrants(),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(ApplicationGrant.class)));
        userGrantsUpdateUseCase.setGlobalGrants(new Member.Id(memberId), globalGrants);
        return ResponseEntity.ok(null);
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<EditAnotherMemberDetailsFormApiDto> getMemberEditByAdminForm(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditAnotherMemberForm(new Member.Id(
                memberId)), EditAnotherMemberDetailsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<Void> putMemberEditByAdminForm(Integer memberId, EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsFormApiDto) {
        editMemberUseCase.editMember(new Member.Id(memberId),
                conversionService.convert(editAnotherMemberDetailsFormApiDto, EditAnotherMemberInfoByAdminForm.class));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<EditMyDetailsFormApiDto> membersMemberIdEditOwnMemberInfoFormGet(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditOwnMemberInfoForm(new Member.Id(
                memberId)), EditMyDetailsFormApiDto.class));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdEditOwnMemberInfoFormPut(Integer memberId, EditMyDetailsFormApiDto editMyDetailsFormApiDto) {
        editMemberUseCase.editMember(new Member.Id(memberId),
                conversionService.convert(editMyDetailsFormApiDto, EditOwnMemberInfoForm.class));
        return ResponseEntity.ok(null);
    }
}
