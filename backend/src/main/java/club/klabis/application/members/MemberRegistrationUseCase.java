package club.klabis.application.members;

import club.klabis.domain.members.*;
import club.klabis.domain.members.forms.RegistrationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

    public RegistrationNumber suggestRegistrationNumber(LocalDate dateOfBirth, Sex sex) {
        // TODO: pripomenout si co tady dela pohlavi... proc je dulezite? (a pokud je to spatne, tak opravit)
        return membersRepository.findMembersWithSameBirthyearAndSex(dateOfBirth, sex).stream()
                .map(Member::getRegistration)
                .sorted()
                .reduce((first, second) -> second)    // find last (highest) item
                .map(RegistrationNumber::followingRegistrationNumber)
                .orElseGet(() -> RegistrationNumber.ofZbmClub(dateOfBirth, 1));
    }

}
