package club.klabis.domain.members;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembersRepository extends InMemoryRepository<Member, Member.Id> {

    default List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex) {
        return findAll().stream()
                .filter(m -> sex.equals(m.getSex()) && m.getDateOfBirth().getYear() == birthDate.getYear())
                .toList();
    }

    boolean existsByRegistration(RegistrationNumber registrationNumber);

    List<Member> findMembersBySuspendedIsFalse();
}
