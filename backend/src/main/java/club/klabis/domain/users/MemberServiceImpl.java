package club.klabis.domain.users;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
class MemberServiceImpl implements MemberService {
    private Collection<Member> members = List.of(new Member("ZBM8003", "{noop}secret", "110875617296914468258", null));

    @Override
    public Optional<Member> findByUserName(String username) {
        return members.stream().filter(it -> username.equals(it.getUserName())).findAny();
    }

    @Override
    public Optional<Member> findByGoogleSubject(String googleSub) {
        return members.stream().filter(it -> googleSub.equals(it.getGoogleSubject())).findAny();
    }
}
