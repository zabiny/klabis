package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.members.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransactionQueryService implements TransactionQueryPort {

    private final MemberAccountRepository memberAccountRepository;

    TransactionQueryService(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> findTransactions(TransactionQuery query) {
        return memberAccountRepository.findTransactions(
                query.memberId(),
                query.occurredAtFrom(),
                query.occurredAtTo(),
                query.type(),
                query.pageable()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction findTransaction(MemberId memberId, TransactionId transactionId) {
        MemberAccount account = memberAccountRepository.findById(memberId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return account.getTransactions().stream()
                .filter(tx -> tx.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
