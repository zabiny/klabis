package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembersRepository extends ListCrudRepository<Member, Member.Id> {

    default List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex) {
        return findAll().stream()
                .filter(m -> sex.equals(m.getSex()) && m.getDateOfBirth().getYear() == birthDate.getYear())
                .toList();
    }

    boolean existsByRegistration(RegistrationNumber registrationNumber);

    List<Member> findMembersBySuspendedIsFalse();
}
