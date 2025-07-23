package club.klabis.domain.users;

import java.util.Optional;

public interface KlabisApplicationUserDetailsService {

    Optional<ApplicationUser> getApplicationUserForUsername(String userName);

}
