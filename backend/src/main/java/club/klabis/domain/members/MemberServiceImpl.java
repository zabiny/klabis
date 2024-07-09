package club.klabis.domain.members;

import club.klabis.domain.members.forms.MemberEditForm;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class MemberServiceImpl implements MemberService {
    private final MembersRepository membersRepository;

    MemberServiceImpl(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    @Override
    public List<Member> findAll(boolean includeSuspended) {
        if (includeSuspended) {
            return membersRepository.findAll();
        } else {
            return membersRepository.findAllActive();
        }
    }

    @Override
    public Optional<Member> findById(Integer memberId) {
        return membersRepository.findById(memberId);
    }


    @Transactional
    @Override
    public Member registerMember(RegistrationForm registrationForm) {
        if (membersRepository.isRegistrationNumberUsed(registrationForm.registrationNumber())) {
            throw new MemberRegistrationFailedException("Registration number '%s' is already used".formatted(registrationForm.registrationNumber()));
        }

        Member newMember = Member.fromRegistration(registrationForm);
        return membersRepository.save(newMember);
    }

    @Override
    public RegistrationNumber suggestRegistrationNumber(LocalDate dateOfBirth, Sex sex) {
        // TODO: pripomenout si co tady dela pohlavi... proc je dulezite? (a pokud je to spatne, tak opravit)
        return membersRepository.findMembersWithSameBirthyearAndSex(dateOfBirth, sex).stream()
                .map(Member::getRegistration)
                .sorted()
                .reduce((first, second) -> second)    // find last (highest) item
                .map(RegistrationNumber::followingRegistrationNumber)
                .orElseGet(() -> RegistrationNumber.ofZbmClub(dateOfBirth, 1));
    }

    @Transactional
    @Override
    public Member editMember(Integer memberId, MemberEditForm editForm) {
        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.edit(editForm);
        return membersRepository.save(member);
    }

    @Override
    public Optional<MembershipSuspensionInfo> getSuspensionInfoForMember(int memberId) {
        return membersRepository.findById(memberId)
                .map(this::suspensionInfoForMember);
    }

    private MembershipSuspensionInfo suspensionInfoForMember(Member member) {
        return new MembershipSuspensionInfo(member.isSuspended(),true);
    }

    @Override
    @Transactional
    public void suspendMembershipForMember(int memberId, boolean forceSuspension) {
        Member memberForSuspension = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (forceSuspension || suspensionInfoForMember(memberForSuspension).canSuspendAccount()) {
            memberForSuspension.suspend();
            membersRepository.save(memberForSuspension);
        } else {
            throw new MembershipCannotBeSuspendedException(memberId, "member is already suspended");
        }
    }
}
