package club.klabis.users.application;

import club.klabis.members.MemberId;
import club.klabis.users.domain.ApplicationUser;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

@Repository
public interface ApplicationUsersRepository extends ListCrudRepository<ApplicationUser, ApplicationUser.Id> {

    Optional<ApplicationUser> findByUserName(String username);

    Optional<ApplicationUser> findByGoogleSubject(String googleSubject);

    Optional<ApplicationUser> findByMemberId(MemberId memberId);
}
