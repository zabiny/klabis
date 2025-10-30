package club.klabis.users.application;

import club.klabis.shared.config.inmemorystorage.DataRepository;
import club.klabis.users.domain.ApplicationUser;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
@SecondaryPort
public interface ApplicationUsersRepository extends DataRepository<ApplicationUser, ApplicationUser.Id> {

    Optional<ApplicationUser> findByUserNameValue(String username);

    Optional<ApplicationUser> findByGoogleSubject(String googleSubject);
}
