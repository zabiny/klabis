package com.klabis.finance.infrastructure.jdbc;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberAccount JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})
)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MemberAccountRepositoryTest {

    @Autowired
    private MemberAccountRepository memberAccountRepository;

    private final MemberId memberId = new MemberId(UUID.randomUUID());
    private final UserId financeManager = new UserId(UUID.randomUUID());

    @Test
    @DisplayName("should persist a new MemberAccount with zero balance and reload it")
    void shouldPersistAndReloadMemberAccountWithZeroBalance() {
        MemberAccount account = MemberAccount.openFor(memberId);

        memberAccountRepository.save(account);

        Optional<MemberAccount> reloaded = memberAccountRepository.findById(memberId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(memberId);
        assertThat(reloaded.get().getBalance()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("should persist a MemberAccount with one deposit and reload transactions intact")
    void shouldPersistAndReloadMemberAccountWithDeposit() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Money depositAmount = Money.ofCzk(BigDecimal.valueOf(250));
        account.deposit(depositAmount, "test deposit", LocalDate.now(), Instant.now(), financeManager);

        memberAccountRepository.save(account);

        Optional<MemberAccount> reloaded = memberAccountRepository.findById(memberId);
        assertThat(reloaded).isPresent();
        MemberAccount reloadedAccount = reloaded.get();
        assertThat(reloadedAccount.getBalance()).isEqualTo(depositAmount);
        assertThat(reloadedAccount.getTransactions()).hasSize(1);
        Transaction tx = reloadedAccount.getTransactions().get(0);
        assertThat(tx.getAmount()).isEqualTo(depositAmount);
        assertThat(tx.getNote()).isEqualTo("test deposit");
        assertThat(tx.getRecordedBy()).isEqualTo(financeManager);
    }

    @Test
    @DisplayName("should persist multiple deposits and reload all transactions with correct balance")
    void shouldPersistAndReloadMultipleDeposits() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Money amount1 = Money.ofCzk(BigDecimal.valueOf(100));
        Money amount2 = Money.ofCzk(BigDecimal.valueOf(350));
        account.deposit(amount1, "first", LocalDate.now(), Instant.now(), financeManager);
        account.deposit(amount2, "second", LocalDate.now(), Instant.now(), financeManager);

        memberAccountRepository.save(account);

        Optional<MemberAccount> reloaded = memberAccountRepository.findById(memberId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(450)));
        assertThat(reloaded.get().getTransactions()).hasSize(2);
    }
}
