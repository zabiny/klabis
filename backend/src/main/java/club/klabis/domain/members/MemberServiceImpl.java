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
        Member admin = Member.fromRegistration(RegistrationNumber.ofRegistrationId("ZBM8003"), "{noop}secret");
        admin.linkWithGoogle("110875617296914468258");
        admin.setObLicence(OBLicence.C);
        membersRepository.save(admin);
    }

    @Override
    public Optional<Member> findByGoogleSubject(String googleSub) {
        return membersRepository.findByGoogleSubject(googleSub);
    }

    @Override
    public Optional<Member> findByUserName(String username) {
        return membersRepository.findByUserName(username);
    }

    @Override
    public List<Member> findAll(boolean includeSuspended) {
        return membersRepository.findAll();
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
        return membersRepository.findMembersByBirthYearAndSex(dateOfBirth.getYear(), sex).stream()
                .map(Member::getRegistration)
                .sorted()
                .reduce((first, second) -> second)    // find last item
                .map(RegistrationNumber::followingRegistrationNumber)
                .orElseGet(() -> RegistrationNumber.ofZbmClub(dateOfBirth.getYear(), 1));
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
                .map(m -> new MembershipSuspensionInfo(true,true));
    }
}
