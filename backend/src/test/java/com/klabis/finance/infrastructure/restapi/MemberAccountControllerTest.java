package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.application.ReversePort;
import com.klabis.finance.application.TransactionQueryPort;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.OverdraftLimitExceededException;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionAlreadyReversedException;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@DisplayName("MemberAccountController REST API Tests")
@WebMvcTest(MemberAccountController.class)
@Import(HalFormsSupport.class)
@WithPostprocessors
class MemberAccountControllerTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final MemberId MEMBER_ID = new MemberId(MEMBER_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepositPort depositPort;

    @MockitoBean
    private ChargePort chargePort;

    @MockitoBean
    private ReversePort reversePort;

    @MockitoBean
    private MemberAccountRepository memberAccountRepository;

    @MockitoBean
    private TransactionQueryPort transactionQueryPort;

    @Nested
    @DisplayName("POST /api/members/{id}/account/transactions (deposit)")
    class DepositEndpoint {

        @Test
        @DisplayName("returns 401 when not authenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDepositBody()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 when authenticated without FINANCE:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WithoutFinanceManage() throws Exception {
            mockMvc.perform(post("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDepositBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 201 Created with Location header on valid deposit")
        @WithKlabisMockUser(authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn201WithLocationOnValidDeposit() throws Exception {
            Transaction tx = buildDepositTransaction();
            when(depositPort.deposit(any())).thenReturn(tx);

            mockMvc.perform(post("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validDepositBody()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            containsString("/api/members/" + MEMBER_UUID + "/account/transactions/" + TX_UUID)));
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/account/transactions/charge")
    class ChargeEndpoint {

        @Test
        @DisplayName("returns 201 Created with Location header on valid charge within overdraft limit")
        @WithKlabisMockUser(authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn201WithLocationOnValidCharge() throws Exception {
            Transaction tx = buildChargeTransaction();
            when(chargePort.charge(any())).thenReturn(tx);

            mockMvc.perform(post("/api/members/{id}/account/transactions/charge", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validChargeBody()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            containsString("/api/members/" + MEMBER_UUID + "/account/transactions/" + TX_UUID)));
        }

        @Test
        @DisplayName("returns 422 with OVERDRAFT_LIMIT_EXCEEDED code when charge would breach limit")
        @WithKlabisMockUser(authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn422WhenChargeExceedsOverdraftLimit() throws Exception {
            Money balance = Money.ofCzk(BigDecimal.valueOf(-400));
            Money chargeAmount = Money.ofCzk(BigDecimal.valueOf(200));
            Money limit = Money.ofCzk(BigDecimal.valueOf(-500));
            when(chargePort.charge(any())).thenThrow(new OverdraftLimitExceededException(balance, chargeAmount, limit));

            mockMvc.perform(post("/api/members/{id}/account/transactions/charge", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validChargeBody()))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.type").value(containsString("OVERDRAFT_LIMIT_EXCEEDED")));
        }

        @Test
        @DisplayName("returns 403 when authenticated without FINANCE:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WithoutFinanceManage() throws Exception {
            mockMvc.perform(post("/api/members/{id}/account/transactions/charge", MEMBER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validChargeBody()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/members/{id}/account")
    class GetAccountEndpoint {

        @Test
        @DisplayName("returns 401 when not authenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("5.1 owner gets 200 with history link")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldReturn200ForOwner() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").exists())
                    .andExpect(jsonPath("$._links.transactions").exists());
        }

        @Test
        @DisplayName("5.2 non-owner without FINANCE:MANAGE gets 403")
        @WithKlabisMockUser(memberId = "99999999-9999-9999-9999-999999999999")
        void shouldReturn403ForNonOwnerWithoutFinanceManage() throws Exception {
            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("5.3 FINANCE:MANAGE can read any account")
        @WithKlabisMockUser(memberId = "99999999-9999-9999-9999-999999999999", authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn200ForFinanceManager() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").exists());
        }

        @Test
        @DisplayName("returns 200 with deposit affordance for FINANCE:MANAGE")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.FINANCE_MANAGE})
        void shouldReturnAccountWithDepositAffordanceForFinanceManager() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.deposit").exists());
        }

        @Test
        @DisplayName("returns 200 without deposit affordance for plain member viewing own account")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldReturnAccountWithoutDepositAffordanceForRegularMember() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.deposit").doesNotExist());
        }

        @Test
        @DisplayName("returns 200 with charge affordance for FINANCE:MANAGE")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.FINANCE_MANAGE})
        void shouldReturnAccountWithChargeAffordanceForFinanceManager() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.charge").exists());
        }

        @Test
        @DisplayName("returns 200 without charge affordance for plain member viewing own account")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldReturnAccountWithoutChargeAffordanceForRegularMember() throws Exception {
            when(memberAccountRepository.findBalanceById(MEMBER_ID)).thenReturn(Optional.of(Money.zero()));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.charge").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/members/{id}/account/transactions")
    class TransactionsListEndpoint {

        @Test
        @DisplayName("5.5 returns paginated transactions for owner")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldReturnPaginatedTransactionsForOwner() throws Exception {
            Transaction tx = buildDepositTransaction();
            var page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);
            when(transactionQueryPort.findTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.transactions").isArray())
                    .andExpect(jsonPath("$.page.size").value(20));
        }

        @Test
        @DisplayName("5.5 returns 403 for non-owner without FINANCE:MANAGE")
        @WithKlabisMockUser(memberId = "99999999-9999-9999-9999-999999999999")
        void shouldReturn403ForNonOwnerWithoutFinanceManage() throws Exception {
            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("5.6 supports sorting by occurredAt")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldSupportSortingByOccurredAt() throws Exception {
            Page<Transaction> page = new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "occurredAt")), 0);
            when(transactionQueryPort.findTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .param("sort", "occurredAt,asc")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("5.6 supports sorting by amount desc")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldSupportSortingByAmountDesc() throws Exception {
            Page<Transaction> page = new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "amount")), 0);
            when(transactionQueryPort.findTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .param("sort", "amount,desc")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("5.7 supports filtering by date range")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldSupportFilteringByDateRange() throws Exception {
            Page<Transaction> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(transactionQueryPort.findTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .param("occurredAtFrom", "2026-01-01")
                            .param("occurredAtTo", "2026-12-31")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("5.7 supports filtering by type")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldSupportFilteringByType() throws Exception {
            Transaction tx = buildDepositTransaction();
            var page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);
            when(transactionQueryPort.findTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/members/{id}/account/transactions", MEMBER_UUID)
                            .param("type", "DEPOSIT")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.transactions", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/members/{id}/account/transactions/{txId}")
    class GetTransactionEndpoint {

        @Test
        @DisplayName("5.9 reversed transaction has reversedBy link")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldExposeReversedByLinkOnReversedTransaction() throws Exception {
            UUID reversalTxUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
            Transaction original = buildDepositTransaction();
            Transaction reversal = buildReversalTransaction(reversalTxUuid);
            MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of(original));
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
            when(memberAccountRepository.findReversalOf(new TransactionId(TX_UUID))).thenReturn(Optional.of(reversal));

            mockMvc.perform(get("/api/members/{id}/account/transactions/{txId}", MEMBER_UUID, TX_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.reversedBy").exists());
        }

        @Test
        @DisplayName("5.10 reversal transaction has reverses link")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldExposeReversesLinkOnReversalTransaction() throws Exception {
            UUID reversalTxUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
            Transaction reversal = buildReversalTransaction(reversalTxUuid);
            MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of(reversal));
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
            when(memberAccountRepository.findReversalOf(new TransactionId(reversalTxUuid))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/members/{id}/account/transactions/{txId}", MEMBER_UUID, reversalTxUuid)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.reverses").exists());
        }

        @Test
        @DisplayName("5.10 not-yet-reversed transaction has reverse affordance for FINANCE:MANAGE")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.FINANCE_MANAGE})
        void shouldExposeReverseAffordanceForFinanceManagerOnUnreversedTransaction() throws Exception {
            Transaction original = buildDepositTransaction();
            MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of(original));
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
            when(memberAccountRepository.findReversalOf(new TransactionId(TX_UUID))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/members/{id}/account/transactions/{txId}", MEMBER_UUID, TX_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.reverse").exists());
        }

        @Test
        @DisplayName("5.10 reversed transaction has no reverse affordance")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.FINANCE_MANAGE})
        void shouldNotExposeReverseAffordanceOnAlreadyReversedTransaction() throws Exception {
            UUID reversalTxUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
            Transaction original = buildDepositTransaction();
            Transaction reversal = buildReversalTransaction(reversalTxUuid);
            MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of(original));
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
            when(memberAccountRepository.findReversalOf(new TransactionId(TX_UUID))).thenReturn(Optional.of(reversal));

            mockMvc.perform(get("/api/members/{id}/account/transactions/{txId}", MEMBER_UUID, TX_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.reverse").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/account/transactions/{txId}/reverse")
    class ReverseEndpoint {

        @Test
        @DisplayName("returns 401 when not authenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/members/{id}/account/transactions/{txId}/reverse",
                            MEMBER_UUID, TX_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validReverseBody()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 when authenticated without FINANCE:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WithoutFinanceManage() throws Exception {
            mockMvc.perform(post("/api/members/{id}/account/transactions/{txId}/reverse",
                            MEMBER_UUID, TX_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validReverseBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 201 Created with Location header on valid reversal")
        @WithKlabisMockUser(authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn201WithLocationOnValidReversal() throws Exception {
            UUID reversalTxUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
            Transaction reversal = buildReversalTransaction(reversalTxUuid);
            when(reversePort.reverse(any())).thenReturn(reversal);

            mockMvc.perform(post("/api/members/{id}/account/transactions/{txId}/reverse",
                            MEMBER_UUID, TX_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validReverseBody()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location",
                            containsString("/api/members/" + MEMBER_UUID + "/account/transactions/" + reversalTxUuid)));
        }

        @Test
        @DisplayName("returns 409 Conflict when transaction has already been reversed")
        @WithKlabisMockUser(authorities = {Authority.FINANCE_MANAGE})
        void shouldReturn409WhenTransactionAlreadyReversed() throws Exception {
            when(reversePort.reverse(any())).thenThrow(
                    new TransactionAlreadyReversedException(new TransactionId(TX_UUID)));

            mockMvc.perform(post("/api/members/{id}/account/transactions/{txId}/reverse",
                            MEMBER_UUID, TX_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validReverseBody()))
                    .andExpect(status().is(409))
                    .andExpect(jsonPath("$.type").value(containsString("TRANSACTION_ALREADY_REVERSED")));
        }
    }

    private String validReverseBody() {
        return """
                {
                    "note": "Storno",
                    "occurredAt": "2026-05-01"
                }
                """;
    }

    private Transaction buildReversalTransaction(UUID reversalTxUuid) {
        return Transaction.reconstruct(
                new TransactionId(reversalTxUuid),
                TransactionType.OTHER,
                Money.ofCzk(BigDecimal.valueOf(-200)),
                "Storno",
                Instant.now(),
                LocalDate.of(2026, 5, 1),
                new com.klabis.common.users.UserId(UUID.randomUUID()),
                new TransactionId(TX_UUID)
        );
    }

    private String validDepositBody() {
        return """
                {
                    "amount": 200.00,
                    "occurredAt": "2026-05-01",
                    "note": "Test deposit"
                }
                """;
    }

    private String validChargeBody() {
        return """
                {
                    "amount": 100.00,
                    "occurredAt": "2026-05-01",
                    "note": "Test charge"
                }
                """;
    }

    private Transaction buildChargeTransaction() {
        return Transaction.reconstruct(
                new TransactionId(TX_UUID),
                TransactionType.OTHER,
                Money.ofCzk(BigDecimal.valueOf(-100)),
                "Test charge",
                Instant.now(),
                LocalDate.of(2026, 5, 1),
                new com.klabis.common.users.UserId(UUID.randomUUID()),
                null
        );
    }

    private Transaction buildDepositTransaction() {
        return Transaction.reconstruct(
                new TransactionId(TX_UUID),
                TransactionType.DEPOSIT,
                Money.ofCzk(BigDecimal.valueOf(200)),
                "Test deposit",
                Instant.now(),
                LocalDate.of(2026, 5, 1),
                new com.klabis.common.users.UserId(UUID.randomUUID()),
                null
        );
    }

    private MemberAccount buildAccount() {
        return MemberAccount.openFor(MEMBER_ID);
    }
}
