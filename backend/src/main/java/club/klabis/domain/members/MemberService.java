package club.klabis.domain.members;

import club.klabis.domain.members.forms.RegistrationForm;
import jakarta.validation.Valid;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Validated
@Service
public interface MemberService {

    Optional<Member> findByUserName(String username);

    Optional<Member> findByGoogleSubject(String googleSub);

    Member registerMember(@Valid RegistrationForm registrationForm);

    List<Member> findAll(boolean includeSuspended);
}