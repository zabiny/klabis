package com.klabis.finance;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.MemberId;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
@DisplayName("Account owner access - full-stack integration")
class AccountOwnerAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private DepositPort depositPort;
    @Autowired
    private MemberAccountRepository accountRepository;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    private Member owner;
    private Member other;
    private Transaction ownerTransaction;

    @BeforeEach
    void membersWithAccounts() {
        owner = register("Jan", "Novák");
        other = register("Eva", "Svobodová");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            org.assertj.core.api.Assertions.assertThat(accountRepository.findById(owner.getId())).isPresent();
            org.assertj.core.api.Assertions.assertThat(accountRepository.findById(other.getId())).isPresent();
        });
        ownerTransaction = depositPort.deposit(new DepositPort.DepositCommand(
                owner.getId(), new BigDecimal("100"), LocalDate.now(), "deposit", new UserId(UUID.randomUUID())));
    }

    private Member register(String firstName, String lastName) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(firstName, lastName, LocalDate.of(1990, 1, 1), Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(firstName.toLowerCase() + "." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));
    }

    private RequestPostProcessor loggedInAs(Member member, Authority... authorities) {
        UserId userId = member.getId().toUserId();
        JwtParams params = JwtParams.jwtTokenParams("user", userId).withMemberId(member.getId())
                .withAuthorities(authorities);
        var snapshot = authorities.length == 0
                ? snapshotLoader.loadFor(userId)
                : TestSnapshots.of(Set.of(authorities), Map.of());
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(params, snapshot));
    }

    private ResultActions account(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/account", of.getId().uuid()).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions transactions(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/account/transactions", of.getId().uuid()).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions transaction(Member of, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/account/transactions/{tx}", of.getId().uuid(),
                ownerTransaction.getId().value()).with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    @DisplayName("member without FINANCE:MANAGE reads own account, transactions and a single transaction")
    void memberReadsOwnAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(owner);

        account(owner, user).andExpect(status().isOk()).andExpect(jsonPath("$.balance").exists());
        transactions(owner, user).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.transactions.length()").value(1));
        transaction(owner, user).andExpect(status().isOk());
    }

    @Test
    @DisplayName("member without FINANCE:MANAGE is denied another member's account and transactions")
    void memberIsDeniedOtherAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(other);

        account(owner, user).andExpect(status().isForbidden());
        transactions(owner, user).andExpect(status().isForbidden());
        transaction(owner, user).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FINANCE:MANAGE user reads any account")
    void financeManagerReadsAnyAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(other, Authority.FINANCE_MANAGE);

        account(owner, user).andExpect(status().isOk());
        transactions(owner, user).andExpect(status().isOk());
        transaction(owner, user).andExpect(status().isOk());
    }
}
