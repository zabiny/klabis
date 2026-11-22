package com.klabis.finance;

import com.klabis.CleanupTestData;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanupTestData
@DisplayName("Account owner access - full-stack integration")
class AccountOwnerAccessIntegrationTest extends FinanceAccessIntegrationTestBase {

    private Member owner;
    private Member other;
    private Transaction ownerTransaction;

    @BeforeEach
    void membersWithAccounts() {
        owner = registerAdult("Jan", "Novák");
        other = registerAdult("Eva", "Svobodová");
        awaitAccountsOf(owner, other);
        ownerTransaction = depositTo(owner);
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

    @Test
    @DisplayName("member without FINANCE:MANAGE reads own account, transactions and a single transaction")
    void memberReadsOwnAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(owner);

        account(owner, user).andExpect(status().isOk()).andExpect(jsonPath("$.balance").exists());
        transactions(owner, user).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.transactions.length()").value(1));
        transaction(owner, ownerTransaction, user).andExpect(status().isOk());
    }

    @Test
    @DisplayName("member without FINANCE:MANAGE is denied another member's account and transactions")
    void memberIsDeniedOtherAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(other);

        account(owner, user).andExpect(status().isForbidden());
        transactions(owner, user).andExpect(status().isForbidden());
        transaction(owner, ownerTransaction, user).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FINANCE:MANAGE user reads any account")
    void financeManagerReadsAnyAccount() throws Exception {
        RequestPostProcessor user = loggedInAs(other, Authority.FINANCE_MANAGE);

        account(owner, user).andExpect(status().isOk());
        transactions(owner, user).andExpect(status().isOk());
        transaction(owner, ownerTransaction, user).andExpect(status().isOk());
    }
}
