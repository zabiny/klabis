package club.klabis.domain.users;

import java.util.Optional;

public interface MemberService {

    public Optional<Member> findByUserName(String username);

    Optional<Member> findByGoogleSubject(String googleSub);
}
