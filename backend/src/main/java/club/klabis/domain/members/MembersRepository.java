package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembersRepository extends ListCrudRepository<Member, Integer> {

    List<Member> findMembersByBirthYearAndSex(int birthYear, Sex sex);

    Optional<Member> findByUserName(String username);

    Optional<Member> findByGoogleSubject(String googleSub);

    boolean isRegistrationNumberUsed(RegistrationNumber registrationNumber);

    List<Member> findAllActive();
}
