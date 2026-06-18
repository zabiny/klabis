package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Service
class ChargeService implements ChargePort {

    private final MemberAccountRepository memberAccountRepository;

    ChargeService(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Transactional
    @Override
    public Transaction charge(ChargeCommand command) {
        MemberAccount account = memberAccountRepository.findById(command.memberId())
                .orElseThrow(() -> new MemberAccountNotFoundException(command.memberId()));
        Money amount = Money.ofCzk(command.amount());
        Transaction tx = account.charge(amount, command.note(), command.occurredAt(),
                Instant.now(), command.recordedBy());
        memberAccountRepository.save(account);
        return tx;
    }

    @Override
    public Transaction chargeMembershipFee(MemberId memberId, BigDecimal amount, int year) {
        return charge(new ChargeCommand(
                memberId,
                amount,
                LocalDate.now(),
                "Roční členský příspěvek " + year,
                SYSTEM_USER_ID));
    }
}
