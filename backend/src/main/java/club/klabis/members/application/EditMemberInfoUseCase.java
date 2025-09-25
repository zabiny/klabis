package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.ddd.UseCase;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@UseCase
@Validated
public class EditMemberInfoUseCase {
    private final MembersRepository membersRepository;
    private final ConversionService conversionService;

    public EditMemberInfoUseCase(MembersRepository membersRepository, ConversionService conversionService) {
        this.membersRepository = membersRepository;
        this.conversionService = conversionService;
    }

    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    public EditOwnMemberInfoForm getEditOwnMemberInfoForm(MemberId memberId) {
        return membersRepository.findById(memberId)
                .map(m -> conversionService.convert(m, EditOwnMemberInfoForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Transactional
    @PreAuthorize("@klabisAuthorizationService.canEditMemberData(#memberId)")
    public Member editMember(MemberId memberId, @Valid EditOwnMemberInfoForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }

    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    public EditAnotherMemberInfoByAdminForm getEditAnotherMemberForm(MemberId memberId) {
        return membersRepository.findById(memberId)
                .map(m -> conversionService.convert(m, EditAnotherMemberInfoByAdminForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Transactional
    @HasGrant(ApplicationGrant.MEMBERS_EDIT)
    public Member editMember(MemberId memberId, @Valid EditAnotherMemberInfoByAdminForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }


}
