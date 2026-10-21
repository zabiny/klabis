package com.klabis.finance.application;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionAlreadyReversedException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReverseService")
class ReverseServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());
    private static final UserId MANAGER = new UserId(UUID.randomUUID());

    @Mock
    private MemberAccountRepository memberAccountRepository;

    private ReverseService service;
    private MemberAccount account;

    @BeforeEach
    void setUp() {
        service = new ReverseService(memberAccountRepository);
        account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
    }

    private Money czk(String amount) {
        return Money.ofCzk(new BigDecimal(amount));
    }

    private Transaction deposit(String amount) {
        return account.deposit(czk(amount), "d", LocalDate.now(), Instant.now(), MANAGER);
    }

    private Transaction charge(String amount) {
        return account.charge(czk(amount), "c", LocalDate.now(), Instant.now(), MANAGER);
    }

    private Transaction reverse(Transaction tx) {
        return service.reverse(new ReversePort.ReverseCommand(MEMBER_ID, tx.getId(), "storno", LocalDate.now(), MANAGER));
    }

    @Test
    @DisplayName("reversing a charge raises the balance back")
    void reversingChargeRaisesBalance() {
        deposit("500");
        Transaction charge = charge("200");

        reverse(charge);

        assertThat(account.getBalance()).isEqualTo(czk("500"));
        verify(memberAccountRepository).save(account);
    }

    @Test
    @DisplayName("reversing a transaction twice is rejected and the second reversal is not saved")
    void doubleReversalIsRejected() {
        Transaction deposit = deposit("500");
        reverse(deposit);

        assertThatThrownBy(() -> reverse(deposit))
                .isInstanceOf(TransactionAlreadyReversedException.class);

        assertThat(account.getTransactions()).hasSize(2);
        assertThat(account.getBalance()).isEqualTo(czk("0"));
        verify(memberAccountRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    @DisplayName("a reversal can itself be reversed (storno of storno) and restores the original balance")
    void reversalOfReversalIsAllowed() {
        Transaction deposit = deposit("500");
        Transaction reversal = reverse(deposit);

        reverse(reversal);

        assertThat(account.getBalance()).isEqualTo(czk("500"));
        assertThat(account.getTransactions()).hasSize(3);
    }

    @Test
    @DisplayName("reversal of a deposit is allowed even when it pushes balance below overdraft limit")
    void reversalIgnoresOverdraftLimit() {
        Transaction deposit = deposit("100");
        charge("10000");

        reverse(deposit);

        assertThat(account.getBalance()).isEqualTo(czk("-10000"));
    }

    @Test
    @DisplayName("reversal of unknown transaction is rejected and nothing is saved")
    void unknownTransactionIsRejected() {
        Transaction foreign = MemberAccount.openFor(new MemberId(UUID.randomUUID()))
                .deposit(czk("1"), "x", LocalDate.now(), Instant.now(), MANAGER);

        assertThatThrownBy(() -> reverse(foreign)).isInstanceOf(IllegalArgumentException.class);

        verify(memberAccountRepository, never()).save(any());
    }
}
