package com.klabis.members.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.members.domain.*;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardian.application.LegalGuardianProfile;
import com.klabis.members.legalguardian.application.LegalGuardianNotFoundException;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("Member registration with legal guardians integration test")
class RegisterMemberWithGuardiansIntegrationTest {

    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Autowired
    private LegalGuardianRepository legalGuardianRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionService permissionService;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static NewLegalGuardian newGuardian(String email) {
        return new NewLegalGuardian("Eva", "Svobodová", email, "+420777111222");
    }

    private RegistrationPort.RegisterNewMember minor(String lastName, List<GuardianInput> guardians) {
        return new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Dítě", lastName, LocalDate.of(2015, 4, 10), "SK", Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", "CZ"),
                null, null, null, null, null, guardians, null);
    }

    private RegistrationPort.RegisterNewMember adult(String email, UserId takeOver) {
        return new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 4, 10), "SK", Gender.FEMALE),
                Address.of("Hlavní 1", "Brno", "60200", "CZ"),
                EmailAddress.of(email), PhoneNumber.of("+420777111222"), null, null, null, List.of(), takeOver);
    }

    @Test
    @DisplayName("registers a minor together with a new non-member guardian in one step")
    void registersMinorWithNewGuardian() {
        String email = "eva." + unique() + "@example.com";

        Member minor = registrationPort.registerMember(
                minor("Svobodová", List.of(GuardianInput.created(newGuardian(email)))));

        var guardians = legalGuardianGroupPort.guardiansOf(minor.getId());
        assertThat(guardians).hasSize(1);
        var guardian = legalGuardianRepository.findById(guardians.iterator().next()).orElseThrow();
        assertThat(guardian.getEmail().value()).isEqualTo(email);
        assertThat(userRepository.findById(guardian.getId()).orElseThrow().getUsername()).startsWith("EXT");
        assertThat(memberRepository.findById(minor.getId()).orElseThrow().isDataIncomplete()).isFalse();
    }

    @Test
    @DisplayName("registers a minor with two guardians, an existing one and a new one")
    void registersMinorWithTwoGuardians() {
        LegalGuardianProfile existing = legalGuardianPort.register(newGuardian("first." + unique() + "@example.com"));

        Member minor = registrationPort.registerMember(minor("Dvojice", List.of(
                GuardianInput.existing(existing.guardian().getId()),
                GuardianInput.created(newGuardian("second." + unique() + "@example.com")))));

        assertThat(legalGuardianGroupPort.guardiansOf(minor.getId())).hasSize(2).contains(existing.guardian().getId());
    }

    @Test
    @DisplayName("creates neither the member nor the guardian when the new guardian's e-mail is already in use")
    void createsNothingWhenGuardianEmailIsInUse() {
        String email = "used." + unique() + "@example.com";
        legalGuardianPort.register(newGuardian(email));
        String lastName = "Nevznikne" + unique();

        assertThatThrownBy(() -> registrationPort.registerMember(
                minor(lastName, List.of(GuardianInput.created(newGuardian(email))))))
                .isInstanceOf(LegalGuardianEmailAlreadyInUseException.class);

        assertThat(memberRepository.findAll()).noneMatch(member -> member.getLastName().equals(lastName));
    }

    @Test
    @DisplayName("rejects a minor without any guardian and creates nothing")
    void rejectsMinorWithoutGuardian() {
        String lastName = "BezZastupce" + unique();
        int membersBefore = memberRepository.findAll().size();

        assertThatThrownBy(() -> registrationPort.registerMember(minor(lastName, List.of())))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(memberRepository.findAll()).noneMatch(member -> member.getLastName().equals(lastName));
        assertThat(memberRepository.findAll()).hasSize(membersBefore);
    }

    @Test
    @DisplayName("rejects a chosen guardian who does not exist with a specific error and creates nothing")
    void rejectsUnknownGuardian() {
        String lastName = "NeznamyZastupce" + unique();

        assertThatThrownBy(() -> registrationPort.registerMember(
                minor(lastName, List.of(GuardianInput.existing(new UserId(UUID.randomUUID()))))))
                .isInstanceOf(GuardianNotFoundException.class);

        assertThat(memberRepository.findAll()).noneMatch(member -> member.getLastName().equals(lastName));
    }

    @Test
    @DisplayName("rejects taking over a user who is a member rather than a non-member guardian")
    void rejectsTakeOverOfMember() {
        Member existing = registrationPort.registerMember(adult("member." + unique() + "@example.com", null));
        String email = "another." + unique() + "@example.com";

        assertThatThrownBy(() -> registrationPort.registerMember(adult(email, existing.getId().toUserId())))
                .isInstanceOf(LegalGuardianNotFoundException.class);

        assertThat(memberRepository.findAll()).noneMatch(member -> member.getEmail() != null
                && member.getEmail().value().equals(email));
    }

    @Test
    @DisplayName("adult registration takes over a non-member guardian keeping user, login and guardian groups")
    void takesOverNonMemberGuardian() {
        LegalGuardianProfile profile = legalGuardianPort.register(newGuardian("take." + unique() + "@example.com"));
        UserId guardianId = profile.guardian().getId();
        Member child = registrationPort.registerMember(minor("Potomek", List.of(GuardianInput.existing(guardianId))));

        Member member = registrationPort.registerMember(adult("take." + unique() + "@example.com", guardianId));

        assertThat(member.getId().toUserId()).isEqualTo(guardianId);
        assertThat(legalGuardianRepository.findById(guardianId)).isEmpty();
        User user = userRepository.findById(guardianId).orElseThrow();
        assertThat(user.getUsername()).isEqualTo(profile.loginName());
        assertThat(user.getUsername()).isNotEqualTo(member.getRegistrationNumber().getValue());
        assertThat(legalGuardianGroupPort.guardiansOf(child.getId())).containsExactly(guardianId);
        assertThat(permissionService.getUserPermissions(guardianId).getDirectAuthorities())
                .containsAll(Authority.getStandardUserAuthorities());
    }
}
