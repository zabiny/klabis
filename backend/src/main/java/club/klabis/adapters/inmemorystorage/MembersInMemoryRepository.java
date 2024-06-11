package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MembersRepository;
import club.klabis.domain.members.Sex;

import java.util.List;

class MembersInMemoryRepository extends InMemoryRepositoryImpl<Member, Integer> implements MembersRepository {
    MembersInMemoryRepository() {
        super(Member::getId);
    }

    @Override
    public List<Member> findMembersByBirthYearAndSex(int birthYear, Sex sex) {
        return findAll().stream().filter(m -> sex.equals(m.getSex()) && birthYear == m.getDateOfBirth().getYear()).toList();
    }
}
