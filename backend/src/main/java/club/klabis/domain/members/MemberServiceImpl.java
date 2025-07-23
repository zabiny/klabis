package club.klabis.domain.members;

import club.klabis.domain.members.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.domain.members.forms.EditOwnMemberInfoForm;
import club.klabis.domain.members.forms.MemberEditForm;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class MemberServiceImpl implements MemberService {
    private final MembersRepository membersRepository;
    private final ConversionService conversionService;

    MemberServiceImpl(MembersRepository membersRepository, ConversionService conversionService) {
        this.membersRepository = membersRepository;
        this.conversionService = conversionService;
    }

    @Override
    public List<Member> findAll(boolean includeSuspended) {
        if (includeSuspended) {
            return membersRepository.findAll();
        } else {
            return membersRepository.findMembersBySuspendedIsFalse();
        }
    }

    @Override
    public Optional<Member> findById(Member.Id memberId) {
        return membersRepository.findById(memberId);
    }

    @Transactional
    @Override
    public Member editMember(Member.Id memberId, MemberEditForm editForm) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(editForm);
        return membersRepository.save(member);
    }

    @Override
    public Optional<MembershipSuspensionInfo> getSuspensionInfoForMember(Member.Id memberId) {
        return membersRepository.findById(memberId)
                .map(this::suspensionInfoForMember);
    }

    private MembershipSuspensionInfo suspensionInfoForMember(Member member) {
        // TODO: suspension status for finance account...
        return new MembershipSuspensionInfo(member.isSuspended(), MembershipSuspensionInfo.DetailStatus.OK);
    }

    @Override
    @Transactional
    public void suspendMembershipForMember(Member.Id memberId, boolean forceSuspension) {
        Member memberForSuspension = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (forceSuspension || suspensionInfoForMember(memberForSuspension).canSuspendAccount()) {
            memberForSuspension.suspend();
            membersRepository.save(memberForSuspension);
        } else {
            throw new MembershipCannotBeSuspendedException(memberId, "member is already suspended");
        }
    }

    @Transactional
    @Override
    public Member editMember(Member.Id memberId, EditOwnMemberInfoForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }

    @Override
    public EditAnotherMemberInfoByAdminForm getEditAnotherMemberForm(Member.Id memberId) {
        return findById(memberId)
                .map(m -> conversionService.convert(m, EditAnotherMemberInfoByAdminForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Override
    public Member editMember(Member.Id memberId, EditAnotherMemberInfoByAdminForm form) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(form);
        return membersRepository.save(member);
    }

    @Override
    public EditOwnMemberInfoForm getEditOwnMemberInfoForm(Member.Id memberId) {
        return findById(memberId)
                .map(m -> conversionService.convert(m, EditOwnMemberInfoForm.class))
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }


}
