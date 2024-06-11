package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MembersRepository;
import club.klabis.domain.members.RegistrationNumber;
import club.klabis.domain.members.Sex;

import java.util.List;
import java.util.Optional;

class MembersInMemoryRepository extends InMemoryRepositoryImpl<Member, Integer> implements MembersRepository {
    MembersInMemoryRepository() {
        super(Member::getId);
    }

    @Override
    public List<Member> findMembersByBirthYearAndSex(int birthYear, Sex sex) {
        return findAll().stream().filter(m -> sex.equals(m.getSex()) && birthYear == m.getDateOfBirth().getYear()).toList();
    }

    @Override
    public Optional<Member> findByUserName(String username) {
        return this.findAll().stream().filter(it -> username.equals(it.getRegistration().toRegistrationId())).findAny();
    }

    @Override
    public Optional<Member> findByGoogleSubject(String googleSub) {
        return this.findAll().stream().filter(it -> googleSub.equals(it.getGoogleSubject())).findAny();
    }

    @Override
    public boolean isRegistrationNumberUsed(RegistrationNumber registrationNumber) {
        return this.findAll().stream().anyMatch(it -> it.getRegistration().equals(registrationNumber));
    }


}
