package club.klabis.config.authserver;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

// Object holding data what we would like to publish into Klabis JWT tokens
// TODO: find way how to use it
class KlabisOidcUser extends DefaultOidcUser implements OidcUser, UserDetails {
    private UUID id;
    private String username;
    private boolean active;
    private LocalDateTime createdAt;
    private Collection<? extends GrantedAuthority> authorities = new HashSet<>();

    public KlabisOidcUser(OidcIdToken idToken, OidcUserInfo userInfo) {
        super(AuthorityUtils.NO_AUTHORITIES, idToken, userInfo);
    }

    public KlabisOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken) {
        super(authorities, idToken, null, IdTokenClaimNames.SUB);
    }

    public KlabisOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, String nameAttributeKey) {
        super(authorities, idToken, null, nameAttributeKey);
    }

    public KlabisOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo) {
        this(authorities, idToken, userInfo, IdTokenClaimNames.SUB);
    }

    public KlabisOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo, String nameAttributeKey) {
        super(AuthorityUtils.NO_AUTHORITIES, idToken, userInfo, nameAttributeKey);
        /**
         * Keep the authorities mutable
         */
        if (authorities != null) {
            this.authorities = authorities;
        }

        this.createdAt = LocalDateTime.now();
    }


    @Override
    public String getPassword() {
        return "{noop}test";
    }

    @Override
    public String getUsername() {
        return "ZBM8003";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
