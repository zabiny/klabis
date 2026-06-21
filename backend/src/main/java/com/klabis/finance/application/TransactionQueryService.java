package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.members.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    public Page<TransactionWithReversal> findTransactionsWithReversals(TransactionQuery query) {
        Page<Transaction> page = findTransactions(query);
        List<TransactionId> pageIds = page.getContent().stream().map(Transaction::getId).toList();
        Map<TransactionId, TransactionId> reversalsByOriginal = memberAccountRepository.findReversalsOf(pageIds);
        List<TransactionWithReversal> enriched = page.getContent().stream()
                .map(tx -> {
                    TransactionId reversalId = reversalsByOriginal.get(tx.getId());
                    return reversalId != null
                            ? TransactionWithReversal.withReversal(tx, reversalId)
                            : TransactionWithReversal.withoutReversal(tx);
                })
                .toList();
        return new PageImpl<>(enriched, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction findTransaction(MemberId memberId, TransactionId transactionId) {
        MemberAccount account = memberAccountRepository.findById(memberId)
                .orElseThrow(() -> new MemberAccountNotFoundException(memberId));
        return account.getTransactions().stream()
                .filter(tx -> tx.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
