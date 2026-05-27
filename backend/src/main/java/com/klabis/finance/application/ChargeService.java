package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.OverdraftPolicy;
import com.klabis.finance.domain.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
class ChargeService implements ChargePort {

    private final MemberAccountRepository memberAccountRepository;
    private final OverdraftPolicy overdraftPolicy;

    ChargeService(MemberAccountRepository memberAccountRepository, FinanceProperties financeProperties) {
        this.memberAccountRepository = memberAccountRepository;
        this.overdraftPolicy = new OverdraftPolicy(Money.ofCzk(financeProperties.getOverdraftLimit()));
    }

    @Transactional
    @Override
    public Transaction charge(ChargeCommand command) {
        MemberAccount account = memberAccountRepository.findById(command.memberId())
                .orElseThrow(() -> new MemberAccountNotFoundException(command.memberId()));
        Money amount = Money.ofCzk(command.amount());
        Transaction tx = account.charge(amount, command.note(), command.occurredAt(),
                Instant.now(), command.recordedBy(), overdraftPolicy);
        memberAccountRepository.save(account);
        return tx;
    }
}
