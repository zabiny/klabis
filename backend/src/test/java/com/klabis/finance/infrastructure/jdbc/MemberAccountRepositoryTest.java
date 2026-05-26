package com.klabis.finance.infrastructure.jdbc;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
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

    @Test
    @DisplayName("should persist a new MemberAccount with zero balance and reload it")
    void shouldPersistAndReloadMemberAccountWithZeroBalance() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        MemberAccount account = MemberAccount.openFor(memberId);

        memberAccountRepository.save(account);

        Optional<MemberAccount> reloaded = memberAccountRepository.findById(memberId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(memberId);
        assertThat(reloaded.get().getBalance()).isEqualTo(Money.zero());
    }
}
