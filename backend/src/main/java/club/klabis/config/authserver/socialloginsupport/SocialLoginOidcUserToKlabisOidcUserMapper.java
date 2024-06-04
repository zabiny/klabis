package club.klabis.config.authserver.socialloginsupport;

import club.klabis.config.authserver.KlabisOidcUser;
import club.klabis.domain.users.Member;
import club.klabis.domain.users.MemberService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface SocialLoginOidcUserToKlabisOidcUserMapper {
    /**
     * Returns registration ID for which is mapper supposed to be used
     */
    String getRegistration();
    KlabisOidcUser map(OidcIdToken idToken, OidcUserInfo userInfo, Member user, List<String> roles);
    Function<String, Optional<Member>> findMemberFunction(MemberService memberService);
}
