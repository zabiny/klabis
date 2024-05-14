package club.klabis.config.authserver.sociallogin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Slf4j
public class RegisterMemberFromSocialLoginHandler implements Consumer<OidcUser> {


    @Override
    public void accept(OidcUser oidcUser) {
        log.info("Handling user " + oidcUser.toString());
    }


//    @Override
//    public void accept(OidcUser user) {
//        // Capture user in a local data store on first authentication
//        CustomOidcUser oidcUser = (CustomOidcUser)user;
//
//        if (oidcUser.getId() == null
//            && this.userService.getByUsername(user.getName()) == null
//        ) {
//            Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>)oidcUser.getAuthorities();
//            User localUser = oidcUser.toInstantUser();
//
//            //Role defaultRole = roleService.getDefaultRole();
//
//            //if (defaultRole != null) {
//            //    localUser.setRoles(Set.of(defaultRole));
//            //}
//
//            // register new user?
//            //this.userService.save(localUser);
//
//            if (!CollectionUtils.isEmpty(localUser.getRoles())) {
//                Set<? extends GrantedAuthority> authorities = localUser.getRoles().stream()
//                        .flatMap(role -> role.getAuthorities().stream()
//                                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
//                        )
//                        .collect(Collectors.toSet());
//
//                grantedAuthorities.addAll(authorities);
//            }
//
//            oidcUser.setId(localUser.getId());
//        }
//    }
}