package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
class ReverseService implements ReversePort {

    private final MemberAccountRepository memberAccountRepository;

    ReverseService(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Transactional
    @Override
    public Transaction reverse(ReverseCommand command) {
        MemberAccount account = memberAccountRepository.findById(command.memberId())
                .orElseThrow(() -> new MemberAccountNotFoundException(command.memberId()));
        Transaction reversal = account.reverse(command.transactionId(), command.note(),
                command.occurredAt(), Instant.now(), command.recordedBy());
        memberAccountRepository.save(account);
        return reversal;
    }
}
