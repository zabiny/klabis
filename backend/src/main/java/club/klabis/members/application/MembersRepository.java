package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.members.domain.Sex;
import club.klabis.shared.config.inmemorystorage.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Repository
@SecondaryPort
public interface MembersRepository extends DataRepository<Member, MemberId> {

    List<Member> findMembersWithSameBirthyearAndSex(LocalDate birthDate, Sex sex);

    boolean existsByRegistration(RegistrationNumber registrationNumber);

    Page<Member> findAllBySuspended(boolean includeSuspended, Pageable page);

}
