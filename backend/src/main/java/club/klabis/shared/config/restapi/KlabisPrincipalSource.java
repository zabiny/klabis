package club.klabis.shared.config.restapi;

import java.util.Optional;

public interface KlabisPrincipalSource {

    Optional<KlabisPrincipal> getPrincipalForUserName(String username);

}
