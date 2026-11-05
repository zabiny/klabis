package com.klabis.finance.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.finance.application.MemberAccountNotFoundException;
import com.klabis.finance.application.TransactionNotFoundException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Finance error types")
class FinanceErrorTypesTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());
    private static final TransactionId TRANSACTION_ID = new TransactionId(UUID.randomUUID());

    @Test
    @DisplayName("an overdraft is reported with the amounts as number plus currency")
    void overdraftLimitExceeded() {
        OverdraftLimitExceededException ex = new OverdraftLimitExceededException(MEMBER_ID,
                Money.ofCzk(BigDecimal.valueOf(100)), Money.ofCzk(BigDecimal.valueOf(500)),
                Money.ofCzk(BigDecimal.valueOf(-200)));

        assertThat(ex.errorType()).isEqualTo("overdraft-limit-exceeded");
        assertThat(ex.messageParameters()).containsOnlyKeys("currentBalance", "chargeAmount", "overdraftLimit");
        assertThat(ex.messageParameters().get("chargeAmount"))
                .isEqualTo(Map.of("amount", new BigDecimal("500"), "currency", "CZK"));
        assertThat(ex.messageParameters().get("overdraftLimit"))
                .isEqualTo(Map.of("amount", new BigDecimal("-200"), "currency", "CZK"));
        assertThat(ex.memberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("reversing a reversed transaction names the amount, never the identifiers")
    void transactionAlreadyReversed() {
        TransactionAlreadyReversedException ex = new TransactionAlreadyReversedException(MEMBER_ID, TRANSACTION_ID,
                Money.ofCzk(BigDecimal.valueOf(250)));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.errorType()).isEqualTo("transaction-already-reversed");
        assertThat(ex.messageParameters()).containsOnlyKeys("amount");
        assertThat(ex.messageParameters().get("amount"))
                .isEqualTo(Map.of("amount", new BigDecimal("250"), "currency", "CZK"));
        assertThat(ex.memberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("an unknown account and an unknown transaction are 404 errors of their own type")
    void notFoundErrors() {
        MemberAccountNotFoundException account = new MemberAccountNotFoundException(MEMBER_ID);
        TransactionNotFoundException transaction = new TransactionNotFoundException(MEMBER_ID, TRANSACTION_ID);

        assertThat(account).isInstanceOf(ResourceNotFoundException.class).isInstanceOf(UserFacingError.class);
        assertThat(account.errorType()).isEqualTo("member-account-not-found");
        assertThat(account.memberId()).isEqualTo(MEMBER_ID);
        assertThat(account.messageParameters()).isEmpty();
        assertThat(transaction).isInstanceOf(ResourceNotFoundException.class).isInstanceOf(UserFacingError.class);
        assertThat(transaction.errorType()).isEqualTo("transaction-not-found");
        assertThat(transaction.memberId()).isEqualTo(MEMBER_ID);
        assertThat(transaction.messageParameters()).isEmpty();
    }
}
