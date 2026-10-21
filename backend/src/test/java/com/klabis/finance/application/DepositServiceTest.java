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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositService")
class DepositServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());
    private static final UserId MANAGER = new UserId(UUID.randomUUID());

    @Mock
    private MemberAccountRepository memberAccountRepository;

    private DepositService service;

    @BeforeEach
    void setUp() {
        service = new DepositService(memberAccountRepository);
    }

    private DepositPort.DepositCommand deposit(String amount) {
        return new DepositPort.DepositCommand(MEMBER_ID, new BigDecimal(amount), LocalDate.now(), "note", MANAGER);
    }

    @Test
    @DisplayName("positive deposit increases the balance and is saved")
    void positiveDepositIncreasesBalance() {
        MemberAccount account = MemberAccount.openFor(MEMBER_ID);
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));

        service.deposit(deposit("250.00"));

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(new BigDecimal("250.00")));
        verify(memberAccountRepository).save(account);
    }

    @Test
    @DisplayName("deposit to unknown account throws MemberAccountNotFoundException")
    void unknownAccountThrows() {
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(deposit("10")))
                .isInstanceOf(MemberAccountNotFoundException.class);
    }
}
