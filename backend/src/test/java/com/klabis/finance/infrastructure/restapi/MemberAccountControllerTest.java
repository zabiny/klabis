package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private MemberAccountRepository memberAccountRepository;

    @Nested
    @DisplayName("POST /api/members/{id}/account/transactions")
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
        @DisplayName("returns 200 with deposit affordance for FINANCE:MANAGE")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.FINANCE_MANAGE})
        void shouldReturnAccountWithDepositAffordanceForFinanceManager() throws Exception {
            MemberAccount account = buildAccount();
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.deposit").exists());
        }

        @Test
        @DisplayName("returns 200 without deposit affordance for plain member viewing own account")
        @WithKlabisMockUser(memberId = "11111111-1111-1111-1111-111111111111")
        void shouldReturnAccountWithoutDepositAffordanceForRegularMember() throws Exception {
            MemberAccount account = buildAccount();
            when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));

            mockMvc.perform(get("/api/members/{id}/account", MEMBER_UUID)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.deposit").doesNotExist());
        }
    }

    private String validDepositBody() {
        return """
                {
                    "type": "DEPOSIT",
                    "amount": 200.00,
                    "occurredAt": "2026-05-01",
                    "note": "Test deposit"
                }
                """;
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
