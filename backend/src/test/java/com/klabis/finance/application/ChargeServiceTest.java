package com.klabis.finance.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeService")
class ChargeServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("1200.00");
    private static final int YEAR = 2026;

    @Mock
    private MemberAccountRepository memberAccountRepository;

    private ChargeService service;

    @BeforeEach
    void setUp() {
        service = new ChargeService(memberAccountRepository);
    }

    @Test
    @DisplayName("chargeMembershipFee charges the correct account with the given amount")
    void chargeMembershipFeeChargesCorrectAccount() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        verify(memberAccountRepository).findById(MEMBER_ID);
    }

    @Test
    @DisplayName("chargeMembershipFee sets amount to Money.ofCzk of the given BigDecimal")
    void chargeMembershipFeeUsesCorrectAmount() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(FEE_AMOUNT).negate());
    }

    @Test
    @DisplayName("chargeMembershipFee sets occurredAt to today")
    void chargeMembershipFeeUsesCurrentDate() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        assertThat(account.getTransactions()).hasSize(1);
        assertThat(account.getTransactions().get(0).getOccurredAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("chargeMembershipFee sets note to 'Roční členský příspěvek <year>'")
    void chargeMembershipFeeUsesCorrectNote() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        assertThat(account.getTransactions().get(0).getNote()).isEqualTo("Roční členský příspěvek 2026");
    }

    @Test
    @DisplayName("chargeMembershipFee sets recordedBy to SYSTEM_USER_ID")
    void chargeMembershipFeeUsesSystemUserId() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        assertThat(account.getTransactions().get(0).getRecordedBy()).isEqualTo(ChargePort.SYSTEM_USER_ID);
    }

    @Test
    @DisplayName("chargeMembershipFee saves the account after charging")
    void chargeMembershipFeeSavesAccount() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        verify(memberAccountRepository).save(account);
    }

    @Test
    @DisplayName("chargeMembershipFee returns the resulting Transaction")
    void chargeMembershipFeeReturnsTransaction() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.chargeMembershipFee(MEMBER_ID, FEE_AMOUNT, YEAR);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(Money.ofCzk(FEE_AMOUNT).negate());
    }
}
