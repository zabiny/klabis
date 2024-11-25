package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface ApplicationUsersRepository extends ListCrudRepository<ApplicationUser, ApplicationUser.Id> {

    Optional<ApplicationUser> findByUserName(String username);

    Optional<ApplicationUser> findByGoogleSubject(String googleSub);

    ApplicationUser findByMemberId(Member.Id memberId) throws ApplicationUserNotFound;

}
