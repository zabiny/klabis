package com.klabis.finance;

import com.klabis.CleanupTestData;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.domain.Member;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanupTestData
@DisplayName("Legal guardian access to a minor's account - full-stack integration")
class GuardianAccountAccessIntegrationTest extends FinanceAccessIntegrationTestBase {

    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private Member memberGuardian;
    private UserId extGuardian;
    private Member minor;
    private Member unrelatedMinor;
    private Transaction minorTransaction;

    @BeforeEach
    void guardiansAndMinors() {
        memberGuardian = registerAdult("Jan", "Rodič");
        extGuardian = legalGuardianPort.register(
                new LegalGuardianPort.NewLegalGuardian("Eva", "Externí", unique("ext") + "@example.com",
                        "+420777111222")).guardian().getId();
        minor = registerMinor("Dítě", List.of(GuardianInput.existing(memberGuardian.getId().toUserId()),
                GuardianInput.existing(extGuardian)));
        unrelatedMinor = registerMinor("Cizí", List.of(GuardianInput.created(
                new LegalGuardianPort.NewLegalGuardian("Jiný", "Zástupce", unique("other") + "@example.com",
                        "+420777111333"))));
        awaitAccountsOf(minor, unrelatedMinor);
        minorTransaction = depositTo(minor);
    }

    private RequestPostProcessor memberGuardianToken() {
        UserId userId = memberGuardian.getId().toUserId();
        return tokenOf(JwtParams.jwtTokenParams("ZBM8001", userId).withMemberId(memberGuardian.getId()), userId);
    }

    private RequestPostProcessor extGuardianToken() {
        return tokenOf(JwtParams.jwtTokenParams("EXT0001", extGuardian), extGuardian);
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
        transaction(minor, minorTransaction, user).andExpect(status().isOk())
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
            transaction(minor, minorTransaction, user).andExpect(status().isForbidden());
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
            transaction(minor, minorTransaction, user).andExpect(status().isForbidden());
            detail(minor, user).andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.account").doesNotExist());
        }
    }
}
