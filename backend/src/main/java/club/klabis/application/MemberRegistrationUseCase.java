package club.klabis.application;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberRegistrationFailedException;
import club.klabis.domain.members.MembersRepository;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberRegistrationUseCase {

    private final MembersRepository membersRepository;

    MemberRegistrationUseCase(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    @Transactional
    public Member registerMember(RegistrationForm registrationForm) {
        if (membersRepository.existsByRegistration(registrationForm.registrationNumber())) {
            throw new MemberRegistrationFailedException("Registration number '%s' is already used".formatted(
                    registrationForm.registrationNumber()));
        }

        Member newMember = Member.fromRegistration(registrationForm);
        return membersRepository.save(newMember);
    }


}
