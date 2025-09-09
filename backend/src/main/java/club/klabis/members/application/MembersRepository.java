package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.members.domain.Sex;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembersRepository extends ListCrudRepository<Member, MemberId>, PagingAndSortingRepository<Member, MemberId> {

    default List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex) {
        return findAll().stream()
                .filter(m -> sex.equals(m.getSex()) && m.getDateOfBirth().getYear() == birthDate.getYear())
                .toList();
    }

    boolean existsByRegistration(RegistrationNumber registrationNumber);

    Page<Member> findAllBySuspended(boolean includeSuspended, Pageable page);

}
