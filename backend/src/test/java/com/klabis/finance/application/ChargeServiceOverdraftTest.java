package com.klabis.finance.application;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
@DisplayName("ChargeService - overdraft and amount rules")
class ChargeServiceOverdraftTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());
    private static final UserId MANAGER = new UserId(UUID.randomUUID());

    @Mock
    private MemberAccountRepository memberAccountRepository;

    private ChargeService service;
    private MemberAccount account;

    @BeforeEach
    void setUp() {
        service = new ChargeService(memberAccountRepository);
        account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
    }

    private ChargePort.ChargeCommand charge(String amount) {
        return new ChargePort.ChargeCommand(MEMBER_ID, new BigDecimal(amount), LocalDate.now(), "note", MANAGER);
    }

    @Test
    @DisplayName("finance manager charge is recorded even when the balance goes far below any overdraft limit")
    void managerChargeIsNotLimitedByOverdraft() {
        service.charge(charge("1000000"));

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(new BigDecimal("-1000000")));
        verify(memberAccountRepository).save(account);
    }

    @Test
    @DisplayName("yearly membership fee is recorded on an account already deep in overdraft")
    void yearlyFeeIsNotLimitedByOverdraft() {
        service.charge(charge("100000"));

        service.chargeMembershipFee(MEMBER_ID, new BigDecimal("300"), 2026);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(new BigDecimal("-100300")));
    }

    @Test
    @DisplayName("zero charge is rejected and nothing is saved")
    void zeroChargeIsRejected() {
        assertThatThrownBy(() -> service.charge(charge("0"))).isInstanceOf(IllegalArgumentException.class);

        verify(memberAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("negative charge is rejected and nothing is saved")
    void negativeChargeIsRejected() {
        assertThatThrownBy(() -> service.charge(charge("-5"))).isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getTransactions()).isEmpty();
        verify(memberAccountRepository, never()).save(any());
    }
}
