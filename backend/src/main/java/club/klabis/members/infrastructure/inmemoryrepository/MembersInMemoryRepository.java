package club.klabis.members.infrastructure.inmemoryrepository;

import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.Sex;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

import java.time.LocalDate;
import java.util.List;

@SecondaryAdapter
interface MembersInMemoryRepository extends MembersRepository, InMemoryRepository<Member, MemberId> {

    @Override
    default List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex) {
        return findAll().stream()
                .filter(m -> sex.equals(m.getSex()) && m.getDateOfBirth().getYear() == birthDate.getYear())
                .toList();
    }

}
