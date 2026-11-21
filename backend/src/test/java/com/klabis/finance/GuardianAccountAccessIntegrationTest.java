package com.klabis.finance;

import com.klabis.CleanupTestData;
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
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Legal guardian access to a minor's account - full-stack integration")
class GuardianAccountAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Autowired
    private DepositPort depositPort;
    @Autowired
    private MemberAccountRepository accountRepository;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    private Member memberGuardian;
    private UserId extGuardian;
    private Member minor;
    private Member unrelatedMinor;
    private Transaction minorTransaction;

    @BeforeEach
    void guardiansAndMinors() {
        memberGuardian = registerAdult();
        extGuardian = legalGuardianPort.register(
                new LegalGuardianPort.NewLegalGuardian("Eva", "Externí", unique("ext") + "@example.com",
                        "+420777111222")).guardian().getId();
        minor = registerMinor("Dítě", List.of(GuardianInput.existing(memberGuardian.getId().toUserId()),
                GuardianInput.existing(extGuardian)));
        unrelatedMinor = registerMinor("Cizí", List.of(GuardianInput.created(
                new LegalGuardianPort.NewLegalGuardian("Jiný", "Zástupce", unique("other") + "@example.com",
                        "+420777111333"))));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(accountRepository.findById(minor.getId())).isPresent();
            assertThat(accountRepository.findById(unrelatedMinor.getId())).isPresent();
        });
        minorTransaction = depositPort.deposit(new DepositPort.DepositCommand(
                minor.getId(), new BigDecimal("100"), LocalDate.now(), "deposit", new UserId(UUID.randomUUID())));
    }

    private static String unique(String prefix) {
        return prefix + "." + UUID.randomUUID();
    }

    private Member registerAdult() {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Rodič", LocalDate.of(1985, 1, 1), Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(unique("parent") + "@example.com"), PhoneNumber.of("+420777111222"),
                BirthNumber.of("850101/1234"), null, null, List.of(), null));
    }

    private Member registerMinor(String firstName, List<GuardianInput> guardians) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, "Nezletilý", LocalDate.now().minusYears(10), Nationality.of("CZ"),
                        Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, null, null, null, guardians, null));
    }

    private RequestPostProcessor memberGuardianToken() {
        UserId userId = memberGuardian.getId().toUserId();
        JwtParams params = JwtParams.jwtTokenParams("ZBM8001", userId).withMemberId(memberGuardian.getId());
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(userId)));
    }

    private RequestPostProcessor extGuardianToken() {
        JwtParams params = JwtParams.jwtTokenParams("EXT0001", extGuardian);
        return authentication(
                KlabisAuthenticationFactory.createAuthenticationToken(params, snapshotLoader.loadFor(extGuardian)));
    }

    private ResultActions call(String url, RequestPostProcessor user, Object... vars) throws Exception {
        return mockMvc.perform(get(url, vars).with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions account(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account", user, of.getId().uuid());
    }

    private ResultActions transactions(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account/transactions", user, of.getId().uuid());
    }

    private ResultActions transaction(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}/account/transactions/{tx}", user, of.getId().uuid(),
                minorTransaction.getId().value());
    }

    private ResultActions detail(Member of, RequestPostProcessor user) throws Exception {
        return call("/api/members/{id}", user, of.getId().uuid());
    }

    private void assertReadsMinorAccountReadOnly(RequestPostProcessor user) throws Exception {
        account(minor, user).andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").exists())
                .andExpect(jsonPath("$._templates.deposit").doesNotExist())
                .andExpect(jsonPath("$._templates.charge").doesNotExist());
        transactions(minor, user).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.transactions.length()").value(1))
                .andExpect(jsonPath("$._embedded.transactions[0]._templates.reverse").doesNotExist());
        transaction(minor, user).andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.reverse").doesNotExist());
        detail(minor, user).andExpect(status().isOk()).andExpect(jsonPath("$._links.account").exists());
    }

    private void assertNoAccessToUnrelatedMinor(RequestPostProcessor user) throws Exception {
        account(unrelatedMinor, user).andExpect(status().isForbidden());
        transactions(unrelatedMinor, user).andExpect(status().isForbidden());
        detail(unrelatedMinor, user).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.account").doesNotExist());
    }

    @Nested
    @DisplayName("member guardian")
    class MemberGuardian {

        @Test
        @DisplayName("reads the minor's account read-only and sees the account link on the minor's detail")
        void readsMinorAccount() throws Exception {
            assertReadsMinorAccountReadOnly(memberGuardianToken());
        }

        @Test
        @DisplayName("has no access to an unrelated minor's account")
        void deniedUnrelatedMinor() throws Exception {
            assertNoAccessToUnrelatedMinor(memberGuardianToken());
        }

        @Test
        @DisplayName("loses access when the minor leaves the legal guardian group")
        void losesAccess() throws Exception {
            legalGuardianGroupPort.removeMinor(minor.getId());

            RequestPostProcessor user = memberGuardianToken();
            account(minor, user).andExpect(status().isForbidden());
            transactions(minor, user).andExpect(status().isForbidden());
            transaction(minor, user).andExpect(status().isForbidden());
            detail(minor, user).andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.account").doesNotExist());
        }
    }

    @Nested
    @DisplayName("non-member (EXT) guardian")
    class ExtGuardian {

        @Test
        @DisplayName("reads the minor's account read-only and sees the account link on the minor's detail")
        void readsMinorAccount() throws Exception {
            assertReadsMinorAccountReadOnly(extGuardianToken());
        }

        @Test
        @DisplayName("has no access to an unrelated minor's account")
        void deniedUnrelatedMinor() throws Exception {
            assertNoAccessToUnrelatedMinor(extGuardianToken());
        }

        @Test
        @DisplayName("loses access when the minor leaves the legal guardian group")
        void losesAccess() throws Exception {
            legalGuardianGroupPort.removeMinor(minor.getId());

            RequestPostProcessor user = extGuardianToken();
            account(minor, user).andExpect(status().isForbidden());
            transactions(minor, user).andExpect(status().isForbidden());
            transaction(minor, user).andExpect(status().isForbidden());
            detail(minor, user).andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.account").doesNotExist());
        }
    }
}
