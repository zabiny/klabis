package com.klabis.members;

import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Registers people through the real application ports and logs them in with snapshots from the real
 * {@link AuthorizationSnapshotLoader}, so full-stack access tests exercise production relationship sources
 * instead of emulating them. Import it next to {@code TestApplicationConfiguration}.
 */
@TestComponent
public class AccessTestFixture {

    public record ExtGuardian(UserId id, String loginName) {
    }

    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    public static String unique(String prefix) {
        return prefix + "." + UUID.randomUUID();
    }

    public Member adult(String firstName) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, "Dospělý", LocalDate.of(1990, 1, 1), Nationality.of("CZ"),
                        Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(unique(firstName.toLowerCase()) + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));
    }

    public Member minor(String firstName, GuardianInput... guardians) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, "Nezletilý", LocalDate.now().minusYears(10), Nationality.of("CZ"),
                        Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null, List.of(guardians), null));
    }

    public ExtGuardian extGuardian(String prefix) {
        var profile = legalGuardianPort.register(new LegalGuardianPort.NewLegalGuardian("Eva", "Externí",
                unique(prefix) + "@example.com", "+420777111444"));
        userService.activateWithPassword(profile.guardian().getId(), "{noop}password");
        return new ExtGuardian(profile.guardian().getId(), profile.loginName());
    }

    public RequestPostProcessor tokenFor(Member member, Authority... authorities) {
        UserId userId = member.getId().toUserId();
        return tokenFor(JwtParams.jwtTokenParams("ZBM8001", userId).withMemberId(member.getId())
                .withAuthorities(authorities), userId);
    }

    public RequestPostProcessor tokenFor(ExtGuardian guardian, Authority... authorities) {
        return tokenFor(JwtParams.jwtTokenParams(guardian.loginName(), guardian.id()).withAuthorities(authorities),
                guardian.id());
    }

    public RequestPostProcessor tokenForNonMember(UserId userId, Authority... authorities) {
        return tokenFor(JwtParams.jwtTokenParams("guardian", userId).withAuthorities(authorities), userId);
    }

    private RequestPostProcessor tokenFor(JwtParams params, UserId userId) {
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(userId)));
    }
}
