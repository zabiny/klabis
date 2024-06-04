package club.klabis.domain.members;

import club.klabis.domain.members.forms.RegistrationForm;
import org.jmolecules.ddd.annotation.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface MemberService {

    Optional<Member> findByUserName(String username);

    Optional<Member> findByGoogleSubject(String googleSub);

    Member registerMember(RegistrationForm registrationForm);

    List<Member> findAll(boolean includeSuspended);
}
