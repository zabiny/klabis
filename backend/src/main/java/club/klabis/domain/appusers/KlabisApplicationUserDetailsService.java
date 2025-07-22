package club.klabis.domain.appusers;

import java.util.Optional;

public interface KlabisApplicationUserDetailsService {

    Optional<ApplicationUser> getApplicationUserForUsername(String userName);

}
