package club.klabis.shared.config.restapi;

import club.klabis.shared.config.security.ApplicationGrant;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KlabisUserAuthentication extends AbstractAuthenticationToken {

    private final Jwt authentication;

    private final KlabisPrincipal principal;

    private static Collection<GrantedAuthority> forUser(KlabisPrincipal user) {
        if (user == null) {
            return List.of();
        }
        return user.globalGrants()
                .stream()
                .map(ApplicationGrant::name)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    static KlabisUserAuthentication authenticated(KlabisPrincipal principal, Jwt credentials) {
        return new KlabisUserAuthentication(principal, credentials);
    }

    static KlabisUserAuthentication noUser(Jwt credentials) {
        return new KlabisUserAuthentication(null, credentials);
    }

    private KlabisUserAuthentication(KlabisPrincipal principal, Jwt authentication) {
        super(forUser(principal));
        this.principal = principal;
        this.authentication = authentication;
        setAuthenticated(principal != null);
    }

    @Override
    public Jwt getCredentials() {
        return authentication;
    }

    @Override
    public KlabisPrincipal getPrincipal() {
        return principal;
    }

}
