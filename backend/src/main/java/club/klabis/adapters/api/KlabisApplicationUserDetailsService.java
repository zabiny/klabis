package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationUser;

import java.util.Optional;

public interface KlabisApplicationUserDetailsService {

    Optional<ApplicationUser> getApplicationUserForUsername(String userName);

}
