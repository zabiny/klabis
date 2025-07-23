package club.klabis.members.application;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.domain.forms.MemberEditForm;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditMemberInfoUseCase {
    private final MembersRepository membersRepository;
    private final ConversionService conversionService;

    public EditMemberInfoUseCase(MembersRepository membersRepository, ConversionService conversionService) {
        this.membersRepository = membersRepository;
        this.conversionService = conversionService;
    }

    @Transactional
    public Member editMember(Member.Id memberId, MemberEditForm editForm) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(editForm);
        return membersRepository.save(member);
    }

    @Transactional
    public Member editMember(Member.Id memberId, EditOwnMemberInfoForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }

    public EditAnotherMemberInfoByAdminForm getEditAnotherMemberForm(Member.Id memberId) {
        return membersRepository.findById(memberId)
                .map(m -> conversionService.convert(m, EditAnotherMemberInfoByAdminForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    public Member editMember(Member.Id memberId, EditAnotherMemberInfoByAdminForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }

    public EditOwnMemberInfoForm getEditOwnMemberInfoForm(Member.Id memberId) {
        return membersRepository.findById(memberId)
                .map(m -> conversionService.convert(m, EditOwnMemberInfoForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }


}
