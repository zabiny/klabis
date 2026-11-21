package com.klabis.finance;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.UserId;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
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
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
abstract class FinanceAccessIntegrationTestBase {

    // Czech nationals must supply a birth number, which minors in these scenarios do not have.
    private static final Nationality MINOR_NATIONALITY = Nationality.of("SK");

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected RegistrationPort registrationPort;
    @Autowired
    protected DepositPort depositPort;
    @Autowired
    protected MemberAccountRepository accountRepository;
    @Autowired
    protected AuthorizationSnapshotLoader snapshotLoader;

    protected static String unique(String prefix) {
        return prefix + "." + UUID.randomUUID();
    }

    protected Member registerAdult(String firstName, String lastName) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, lastName, LocalDate.of(1990, 1, 1), Nationality.of("CZ"),
                        Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(unique(firstName.toLowerCase()) + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));
    }

    protected Member registerMinor(String firstName, List<GuardianInput> guardians) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, "Nezletilý", LocalDate.now().minusYears(10), MINOR_NATIONALITY,
                        Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, null, null, null, guardians, null));
    }

    protected void awaitAccountsOf(Member... members) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            for (Member member : members) {
                assertThat(accountRepository.findById(member.getId())).isPresent();
            }
        });
    }

    protected Transaction depositTo(Member member) {
        return depositPort.deposit(new DepositPort.DepositCommand(
                member.getId(), new BigDecimal("100"), LocalDate.now(), "deposit", new UserId(UUID.randomUUID())));
    }

    protected RequestPostProcessor tokenOf(JwtParams params, UserId userId) {
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(userId)));
    }

    protected ResultActions call(String url, RequestPostProcessor user, Object... vars) throws Exception {
        return mockMvc.perform(get(url, vars).with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    protected ResultActions account(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account", user, of.getId().uuid());
    }

    protected ResultActions transactions(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account/transactions", user, of.getId().uuid());
    }

    protected ResultActions transaction(Member of, Transaction tx, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account/transactions/{tx}", user, of.getId().uuid(), tx.getId().value());
    }
}
