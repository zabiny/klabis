package club.klabis.members.adapters.restapi;

import club.klabis.members.MemberId;
import club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto;
import club.klabis.members.application.EditMemberInfoUseCase;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.domain.forms.MemberEditForm;
import club.klabis.shared.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MembersController implements EditMemberUseCaseControllers {

    private final MembersRepository membersRepository;
    private final EditMemberInfoUseCase editMemberUseCase;
    private final ConversionService conversionService;

    public MembersController(MembersRepository membersRepository, EditMemberInfoUseCase editMemberUseCase, ConversionService conversionService) {
        this.membersRepository = membersRepository;
        this.editMemberUseCase = editMemberUseCase;
        this.conversionService = conversionService;
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto> membersMemberIdEditMemberInfoFormGet(Integer memberId) {
        return membersRepository.findById(new MemberId(memberId))
                .map(m -> mapToResponseEntity(m, club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto.class))
                .orElseThrow(() -> new MemberNotFoundException(new MemberId(memberId)));
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @Override
    public ResponseEntity<Void> membersMemberIdEditMemberInfoFormPut(Integer memberId, club.klabis.members.adapters.restapi.dto.MemberEditFormApiDto memberEditFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(memberEditFormApiDto, MemberEditForm.class));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private <T> ResponseEntity<T> mapToResponseEntity(Object data, Class<T> apiDtoType) {
        T payload = conversionService.convert(data, apiDtoType);
        return ResponseEntity.ok(payload);
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto> getMemberEditByAdminForm(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditAnotherMemberForm(new MemberId(
                memberId)), club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto.class));
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<Void> putMemberEditByAdminForm(Integer memberId, club.klabis.members.adapters.restapi.dto.EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editAnotherMemberDetailsFormApiDto, EditAnotherMemberInfoByAdminForm.class));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<EditMyDetailsFormApiDto> membersMemberIdEditOwnMemberInfoFormGet(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditOwnMemberInfoForm(new MemberId(
                memberId)), club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto.class));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdEditOwnMemberInfoFormPut(Integer memberId, club.klabis.members.adapters.restapi.dto.EditMyDetailsFormApiDto editMyDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editMyDetailsFormApiDto, EditOwnMemberInfoForm.class));
        return ResponseEntity.ok(null);
    }
}
