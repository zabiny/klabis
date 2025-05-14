package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUsersRepository extends InMemoryRepository<ApplicationUser, ApplicationUser.Id> {

    Optional<ApplicationUser> findByUserName(String username);

    Optional<ApplicationUser> findByGoogleSubject(String googleSubject);

    Optional<ApplicationUser> findByMemberId(Member.Id memberId);
}
