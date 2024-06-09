package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MembersRepository;
import org.springframework.stereotype.Component;

@Component
abstract class InMemoryMembersRepository extends InMemoryRepositoryImpl<Member, Integer> implements MembersRepository {
    public InMemoryMembersRepository() {
        super(Member::getId);
    }
}
