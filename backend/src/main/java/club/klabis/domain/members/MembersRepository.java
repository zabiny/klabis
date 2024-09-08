package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembersRepository extends ListCrudRepository<Member, Integer> {

    List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex);

    boolean isRegistrationNumberUsed(RegistrationNumber registrationNumber);

    List<Member> findAllActive();
}
