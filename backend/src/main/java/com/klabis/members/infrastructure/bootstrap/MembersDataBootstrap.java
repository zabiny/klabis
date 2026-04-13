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
                passwordHash, Set.of(Authority.values()), Gender.MALE,
                BirthNumber.of("900315/1234"));

        createMember("Eva", "Svobodová", LocalDate.of(1995, 7, 22),
                "eva.svobodova@example.com", "+420 602 333 444",
                "Zahradní 5", "Brno", "60200",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("955722/1234"));

        createMember("Tomáš", "Král", LocalDate.of(1988, 5, 10),
                "tomas.kral@example.com", "+420 603 001 001",
                "Nová 3", "Ostrava", "70200",
                passwordHash, Authority.withStandard(Set.of(Authority.MEMBERS_MANAGE)), Gender.MALE,
                BirthNumber.of("880510/1111"));

        createMember("Marie", "Horáková", LocalDate.of(1992, 9, 18),
                "marie.horakova@example.com", "+420 603 002 002",
                "Lipová 7", "Plzeň", "30100",
                passwordHash, Authority.withStandard(Set.of(Authority.EVENTS_MANAGE)), Gender.FEMALE,
                BirthNumber.of("925918/2222"));

        createMember("Pavel", "Dvořák", LocalDate.of(1985, 11, 25),
                "pavel.dvorak@example.com", "+420 603 003 003",
                "Polní 12", "Olomouc", "77900",
                passwordHash, Authority.withStandard(Set.of(Authority.CALENDAR_MANAGE)), Gender.MALE,
                BirthNumber.of("851125/3333"));

        createMember("Lucie", "Procházková", LocalDate.of(1997, 4, 8),
                "lucie.prochazkova@example.com", "+420 603 004 004",
                "Lesní 2", "Liberec", "46001",
                passwordHash, Authority.withStandard(Set.of(Authority.GROUPS_TRAINING)), Gender.FEMALE,
                BirthNumber.of("975408/4444"));

        createMember("Martin", "Krejčí", LocalDate.of(1986, 2, 14),
                "martin.krejci@example.com", "+420 603 005 005",
                "Školní 9", "České Budějovice", "37001",
                passwordHash, Authority.withStandard(Set.of(Authority.MEMBERS_PERMISSIONS)), Gender.MALE,
                BirthNumber.of("860214/5555"));

        createMember("Petra", "Nováčková", LocalDate.of(2000, 6, 30),
                "petra.novackova@example.com", "+420 603 006 006",
                "Průmyslová 4", "Hradec Králové", "50002",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("005630/6666"));

        createMember("Jakub", "Blažek", LocalDate.of(1999, 1, 5),
                "jakub.blazek@example.com", "+420 603 007 007",
                "Sportovní 18", "Pardubice", "53002",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("990105/7777"));

        createMember("Tereza", "Šimková", LocalDate.of(2001, 8, 12),
                "tereza.simkova@example.com", "+420 603 008 008",
                "Zahradní 1", "Zlín", "76001",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("015812/8888"));

        createMember("Ondřej", "Kratochvíl", LocalDate.of(1994, 12, 3),
                "ondrej.kratochvil@example.com", "+420 603 009 009",
                "Příční 6", "Jihlava", "58601",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("941203/9999"));

        createMember("Kateřina", "Veselá", LocalDate.of(1998, 3, 27),
                "katerina.vesela@example.com", "+420 603 010 010",
                "Okružní 15", "Kladno", "27201",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("985327/1010"));

        createMember("Radek", "Horák", LocalDate.of(1991, 10, 19),
                "radek.horak@example.com", "+420 603 011 011",
                "Náměstní 8", "Most", "43401",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("911019/1111"));

        createMember("Zuzana", "Kolářová", LocalDate.of(2002, 5, 6),
                "zuzana.kolarova@example.com", "+420 603 012 012",
                "Vinohradská 22", "Teplice", "41501",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("025506/1212"));

        createMember("Filip", "Musil", LocalDate.of(1996, 7, 14),
                "filip.musil@example.com", "+420 603 013 013",
                "Ke Škole 11", "Opava", "74601",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("960714/1313"));

        createMember("Alžběta", "Čermáková", LocalDate.of(1993, 9, 2),
                "alzbeta.cermakova@example.com", "+420 603 014 014",
                "Nákladní 3", "Karviná", "73301",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("935902/1414"));

        createMember("Michal", "Pospíšil", LocalDate.of(1987, 6, 21),
                "michal.pospisil@example.com", "+420 603 015 015",
                "Kolová 7", "Frýdek-Místek", "73801",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("870621/1515"));

        LOG.info("Created 17 bootstrap members");
    }

    private void createMember(String firstName, String lastName, LocalDate dateOfBirth,
                              String email, String phone,
                              String street, String city, String postalCode,
                              String passwordHash, Set<Authority> authorities, Gender gender,
                              BirthNumber birthNumber) {

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
                birthNumber,
                null,
                null
        ));

        memberRepository.save(member);

        LOG.info("Created bootstrap member: {} {} (username: {}, authorities: {})",
                firstName, lastName, registrationNumber.getValue(),
                authorities.size() == Authority.values().length ? "ALL" : "STANDARD");
    }
}
