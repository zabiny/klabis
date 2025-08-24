package club.klabis.members.infrastructure.restapi;

import club.klabis.members.MemberId;
import club.klabis.members.application.EditMemberInfoUseCase;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.infrastructure.restapi.dto.EditMyDetailsFormApiDto;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MembersController implements EditMemberUseCaseControllers {

    private final EditMemberInfoUseCase editMemberUseCase;
    private final ConversionService conversionService;

    public MembersController(EditMemberInfoUseCase editMemberUseCase, ConversionService conversionService) {
        this.editMemberUseCase = editMemberUseCase;
        this.conversionService = conversionService;
    }

    // TODO: try to replace form DTOs from API by form DTOs from domain..

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<club.klabis.members.infrastructure.restapi.dto.EditAnotherMemberDetailsFormApiDto> getMemberEditByAdminForm(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditAnotherMemberForm(new MemberId(
                memberId)), club.klabis.members.infrastructure.restapi.dto.EditAnotherMemberDetailsFormApiDto.class));
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    @Override
    public ResponseEntity<Void> putMemberEditByAdminForm(Integer memberId, club.klabis.members.infrastructure.restapi.dto.EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editAnotherMemberDetailsFormApiDto, EditAnotherMemberInfoByAdminForm.class));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<EditMyDetailsFormApiDto> membersMemberIdEditOwnMemberInfoFormGet(Integer memberId) {
        return ResponseEntity.ok(conversionService.convert(editMemberUseCase.getEditOwnMemberInfoForm(new MemberId(
                memberId)), club.klabis.members.infrastructure.restapi.dto.EditMyDetailsFormApiDto.class));
    }

    @Override
    public ResponseEntity<Void> membersMemberIdEditOwnMemberInfoFormPut(Integer memberId, club.klabis.members.infrastructure.restapi.dto.EditMyDetailsFormApiDto editMyDetailsFormApiDto) {
        editMemberUseCase.editMember(new MemberId(memberId),
                conversionService.convert(editMyDetailsFormApiDto, EditOwnMemberInfoForm.class));
        return ResponseEntity.ok(null);
    }
}
