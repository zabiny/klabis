package com.klabis.members.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@Profile("example-data & !oris")
@Order(1)
class MembersDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MembersDataBootstrap.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationNumberGenerator registrationNumberGenerator;
    private final RegistrationPort registrationPort;
    private final LegalGuardianGroupPort legalGuardianGroupPort;

    MembersDataBootstrap(MemberRepository memberRepository, UserService userService,
                         PasswordEncoder passwordEncoder, RegistrationNumberGenerator registrationNumberGenerator,
                         RegistrationPort registrationPort, LegalGuardianGroupPort legalGuardianGroupPort) {
        this.registrationPort = registrationPort;
        this.legalGuardianGroupPort = legalGuardianGroupPort;
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

        Member jan = createMember("Jan", "Novák", LocalDate.of(1990, 3, 15),
                "jan.novak@example.com", "+420 601 111 222",
                "Hlavní 10", "Praha", "11000",
                passwordHash, Set.of(Authority.values()), Gender.MALE,
                BirthNumber.of("900315/1234"), "8012345");

        Member eva = createMember("Eva", "Svobodová", LocalDate.of(1995, 7, 22),
                "eva.svobodova@example.com", "+420 602 333 444",
                "Zahradní 5", "Brno", "60200",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("955722/1234"), "8112233");

        createMember("Tomáš", "Král", LocalDate.of(1988, 5, 10),
                "tomas.kral@example.com", "+420 603 001 001",
                "Nová 3", "Ostrava", "70200",
                passwordHash, Authority.withStandard(Set.of(Authority.MEMBERS_MANAGE)), Gender.MALE,
                BirthNumber.of("880510/1111"), "8023456");

        createMember("Marie", "Horáková", LocalDate.of(1992, 9, 18),
                "marie.horakova@example.com", "+420 603 002 002",
                "Lipová 7", "Plzeň", "30100",
                passwordHash, Authority.withStandard(Set.of(Authority.EVENTS_MANAGE)), Gender.FEMALE,
                BirthNumber.of("925918/2222"), "8034567");

        createMember("Pavel", "Dvořák", LocalDate.of(1985, 11, 25),
                "pavel.dvorak@example.com", "+420 603 003 003",
                "Polní 12", "Olomouc", "77900",
                passwordHash, Authority.withStandard(Set.of(Authority.CALENDAR_MANAGE)), Gender.MALE,
                BirthNumber.of("851125/3333"), "8045678");

        createMember("Lucie", "Procházková", LocalDate.of(1997, 4, 8),
                "lucie.prochazkova@example.com", "+420 603 004 004",
                "Lesní 2", "Liberec", "46001",
                passwordHash, Authority.withStandard(Set.of(Authority.GROUPS_TRAINING)), Gender.FEMALE,
                BirthNumber.of("975408/4444"), "8056789");

        createMember("Martin", "Krejčí", LocalDate.of(1986, 2, 14),
                "martin.krejci@example.com", "+420 603 005 005",
                "Školní 9", "České Budějovice", "37001",
                passwordHash, Authority.withStandard(Set.of(Authority.MEMBERS_PERMISSIONS)), Gender.MALE,
                BirthNumber.of("860214/5555"), null);

        createMember("Petra", "Nováčková", LocalDate.of(2000, 6, 30),
                "petra.novackova@example.com", "+420 603 006 006",
                "Průmyslová 4", "Hradec Králové", "50002",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("005630/6666"), null);

        createMember("Jakub", "Blažek", LocalDate.of(1999, 1, 5),
                "jakub.blazek@example.com", "+420 603 007 007",
                "Sportovní 18", "Pardubice", "53002",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("990105/7777"), "8067890");

        createMember("Tereza", "Šimková", LocalDate.of(2001, 8, 12),
                "tereza.simkova@example.com", "+420 603 008 008",
                "Zahradní 1", "Zlín", "76001",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("015812/8888"), "8078901");

        Member ondrej = createMember("Ondřej", "Kratochvíl", LocalDate.of(1994, 12, 3),
                "ondrej.kratochvil@example.com", "+420 603 009 009",
                "Příční 6", "Jihlava", "58601",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("941203/9999"), "8089012");

        createMember("Kateřina", "Veselá", LocalDate.of(1998, 3, 27),
                "katerina.vesela@example.com", "+420 603 010 010",
                "Okružní 15", "Kladno", "27201",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("985327/1010"), null);

        createMember("Radek", "Horák", LocalDate.of(1991, 10, 19),
                "radek.horak@example.com", "+420 603 011 011",
                "Náměstní 8", "Most", "43401",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("911019/1111"), null);

        createMember("Zuzana", "Kolářová", LocalDate.of(2002, 5, 6),
                "zuzana.kolarova@example.com", "+420 603 012 012",
                "Vinohradská 22", "Teplice", "41501",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("025506/1212"), null);

        createMember("Filip", "Musil", LocalDate.of(1996, 7, 14),
                "filip.musil@example.com", "+420 603 013 013",
                "Ke Škole 11", "Opava", "74601",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("960714/1313"), null);

        createMember("Alžběta", "Čermáková", LocalDate.of(1993, 9, 2),
                "alzbeta.cermakova@example.com", "+420 603 014 014",
                "Nákladní 3", "Karviná", "73301",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.FEMALE,
                BirthNumber.of("935902/1414"), null);

        createMember("Michal", "Pospíšil", LocalDate.of(1987, 6, 21),
                "michal.pospisil@example.com", "+420 603 015 015",
                "Kolová 7", "Frýdek-Místek", "73801",
                passwordHash, Authority.getStandardUserAuthorities(), Gender.MALE,
                BirthNumber.of("870621/1515"), null);

        LOG.info("Created 15 bootstrap members");

        createLegalGuardianScenarios(jan.getId().toUserId(), eva.getId().toUserId(), ondrej.getId().toUserId());
    }

    private void createLegalGuardianScenarios(UserId registeredBy, UserId memberGuardian, UserId memberGuardianOfTwo) {
        LocalDate today = LocalDate.now();
        NewLegalGuardian ivana = new NewLegalGuardian("Ivana", "Dlouhá", "ivana.dlouha@example.com", "+420 604 100 100");
        NewLegalGuardian lenka = new NewLegalGuardian("Lenka", "Kratochvílová", "lenka.kratochvilova@example.com",
                "+420 604 200 200");

        Member adam = registerMinor("Adam", "Dlouhý", today.minusYears(10), Gender.MALE, registeredBy,
                List.of(GuardianInput.created(ivana)));
        UserId ivanaId = legalGuardianGroupPort.guardiansOf(adam.getId()).iterator().next();
        registerMinor("Klára", "Dlouhá", today.minusYears(8), Gender.FEMALE, registeredBy,
                List.of(GuardianInput.existing(ivanaId)));

        registerMinor("Sofie", "Svobodová", today.minusYears(12), Gender.FEMALE, registeredBy,
                List.of(GuardianInput.existing(memberGuardian)));

        registerMinor("Matyáš", "Kratochvíl", today.minusYears(9), Gender.MALE, registeredBy,
                List.of(GuardianInput.existing(memberGuardianOfTwo), GuardianInput.created(lenka)));

        importMinorWithoutGuardian("Vojtěch", "Malý", today.minusYears(14), Gender.MALE, registeredBy);

        LOG.info("Created bootstrap legal guardian scenarios (non-member guardians log in with EXT0001, EXT0002)");
    }

    private Member registerMinor(String firstName, String lastName, LocalDate dateOfBirth, Gender gender,
                                 UserId registeredBy, List<GuardianInput> guardians) {
        Member member = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, lastName, dateOfBirth, "CZ", gender),
                Address.of("Dětská 1", "Praha", "11000", "CZ"),
                null, null, birthNumberOf(dateOfBirth, gender), null,
                registeredBy, guardians, null));
        LOG.info("Created bootstrap minor: {} {} (registration number: {})", firstName, lastName,
                member.getRegistrationNumber().getValue());
        return member;
    }

    private static BirthNumber birthNumberOf(LocalDate dateOfBirth, Gender gender) {
        int month = dateOfBirth.getMonthValue() + (gender == Gender.FEMALE ? 50 : 0);
        return BirthNumber.of("%02d%02d%02d/%04d".formatted(dateOfBirth.getYear() % 100, month,
                dateOfBirth.getDayOfMonth(), 1000 + dateOfBirth.getDayOfYear()));
    }

    private void importMinorWithoutGuardian(String firstName, String lastName, LocalDate dateOfBirth, Gender gender,
                                            UserId registeredBy) {
        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(dateOfBirth);
        registrationPort.importMember(new RegistrationPort.ImportMember(
                new RegistrationPort.RegisterNewMember(
                        PersonalInformation.of(firstName, lastName, dateOfBirth, "CZ", gender),
                        null, null, null, null, null, registeredBy),
                registrationNumber));
        LOG.info("Created bootstrap ORIS-imported minor without guardian: {} {} (registration number: {})",
                firstName, lastName, registrationNumber.getValue());
    }

    private Member createMember(String firstName, String lastName, LocalDate dateOfBirth,
                              String email, String phone,
                              String street, String city, String postalCode,
                              String passwordHash, Set<Authority> authorities, Gender gender,
                              BirthNumber birthNumber, String chipNumber) {

        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(dateOfBirth);

        UserId userId = userService.createActiveUser(registrationNumber.getValue(), passwordHash, authorities);

        Member member = Member.register(new Member.RegisterMember(
                MemberId.fromUserId(userId),
                registrationNumber,
                PersonalInformation.of(firstName, lastName, dateOfBirth, "CZ", gender),
                Address.of(street, city, postalCode, "CZ"),
                EmailAddress.of(email),
                PhoneNumber.of(phone),
                birthNumber,
                null,
                null
        ));

        if (chipNumber != null) {
            member.update(MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .chipNumber(chipNumber)
                    .build(), GuardianContacts.NONE);
        }

        member = memberRepository.save(member);

        LOG.info("Created bootstrap member: {} {} (username: {}, authorities: {})",
                firstName, lastName, registrationNumber.getValue(),
                authorities.size() == Authority.values().length ? "ALL" : "STANDARD");
        return member;
    }
}
