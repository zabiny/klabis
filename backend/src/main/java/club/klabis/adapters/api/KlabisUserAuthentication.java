package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KlabisUserAuthentication extends AbstractAuthenticationToken {

    private final ApplicationUser applicationUser;
    private final Jwt authentication;

    private static Collection<GrantedAuthority> forUser(ApplicationUser user) {
        if (user == null) {
            return List.of();
        }
        return user.getGlobalGrants().stream().map(ApplicationGrant::name).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    static KlabisUserAuthentication authenticated(ApplicationUser user, Jwt credentials) {
        return new KlabisUserAuthentication(user, credentials);
    }

    static KlabisUserAuthentication noUser(Jwt credentials) {
        return new KlabisUserAuthentication(null, credentials);
    }

    private KlabisUserAuthentication(ApplicationUser applicationUser, Jwt authentication) {
        super(forUser(applicationUser));
        this.applicationUser = applicationUser;
        this.authentication = authentication;
        setAuthenticated(applicationUser != null);
    }

    @Override
    public Jwt getCredentials() {
        return authentication;
    }

    @Override
    public ApplicationUser getPrincipal() {
        return applicationUser;
    }
}
