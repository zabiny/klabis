package club.klabis.config.authserver.socialloginsupport;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

@Component
class GoogleOidcUserToKlabisSocialLoginOidcUserMapper implements SocialLoginOidcUserToKlabisOidcUserMapper {
    @Override
    public String getRegistration() {
        return "google";
    }

    @Override
    public Function<String, Optional<ApplicationUser>> findMemberFunction(ApplicationUsersRepository memberService) {
        return memberService::findByGoogleSubject;
    }
}
