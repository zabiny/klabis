package com.klabis.members.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
class MembersDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MembersDataBootstrap.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationNumberGenerator registrationNumberGenerator;

    MembersDataBootstrap(MemberRepository memberRepository, UserService userService,
                         PasswordEncoder passwordEncoder, RegistrationNumberGenerator registrationNumberGenerator) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.registrationNumberGenerator = registrationNumberGenerator;
    }

    @Override
    public boolean requiresBootstrap() {
        return !memberRepository.existsAny();
    }

    @Override
    public void bootstrapData() {
        String passwordHash = passwordEncoder.encode("password");

        createMember("Jan", "Novák", LocalDate.of(1990, 3, 15),
                "jan.novak@example.com", "+420 601 111 222",
                "Hlavní 10", "Praha", "11000",
                passwordHash, Set.of(Authority.values()), Gender.MALE);

        createMember("Eva", "Svobodová", LocalDate.of(1995, 7, 22),
                "eva.svobodova@example.com", "+420 602 333 444",
                "Zahradní 5", "Brno", "60200",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE);

        LOG.info("Created 2 bootstrap members");
    }

    private void createMember(String firstName, String lastName, LocalDate dateOfBirth,
                              String email, String phone,
                              String street, String city, String postalCode,
                              String passwordHash, Set<Authority> authorities, Gender gender) {

        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(dateOfBirth);

        UserId userId = userService.createActiveUser(registrationNumber.getValue(), passwordHash, authorities);

        Member member = Member.register(new Member.RegisterMember(
                MemberId.fromUserId(userId),
                registrationNumber,
                PersonalInformation.of(firstName, lastName, dateOfBirth, "CZ", gender),
                Address.of(street, city, postalCode, "CZ"),
                EmailAddress.of(email),
                PhoneNumber.of(phone),
                null,
                null,
                null,
                null
        ));

        memberRepository.save(member);

        LOG.info("Created bootstrap member: {} {} (username: {}, authorities: {})",
                firstName, lastName, registrationNumber.getValue(),
                authorities.size() == Authority.values().length ? "ALL" : "STANDARD");
    }
}
