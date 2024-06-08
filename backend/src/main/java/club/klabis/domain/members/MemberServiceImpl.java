package club.klabis.domain.members;

import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class MemberServiceImpl implements MemberService {
    private final MembersRepository membersRepository;

    MemberServiceImpl(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
        Member admin = Member.newMember(RegistrationNumber.ofRegistrationId("ZBM8003"), "{noop}secret");
        admin.linkWithGoogle("110875617296914468258");
        admin.setObLicence(OBLicence.C);
        membersRepository.save(admin);
    }

    @Override
    public Optional<Member> findByUserName(String username) {
        return membersRepository.findAll().stream().filter(it -> username.equals(it.getRegistration().toRegistrationId())).findAny();
    }

    @Override
    public Optional<Member> findByGoogleSubject(String googleSub) {
        return membersRepository.findAll().stream().filter(it -> googleSub.equals(it.getGoogleSubject())).findAny();
    }

    @Override
    public Member registerMember(RegistrationForm registrationForm) {
        if (isRegistrationNumberUsed(registrationForm.registrationNumber())) {
            throw new MemberRegistrationError("Registration number '%s' is already used".formatted(registrationForm.registrationNumber()));
        }

        Member newMember = Member.newMember(registrationForm);
        return membersRepository.save(newMember);
    }

    private boolean isRegistrationNumberUsed(RegistrationNumber registrationNumber) {
        return membersRepository.findAll().stream().anyMatch(it -> it.getRegistration().equals(registrationNumber));
    }

    @Override
    public List<Member> findAll(boolean includeSuspended) {
        return membersRepository.findAll();
    }
}