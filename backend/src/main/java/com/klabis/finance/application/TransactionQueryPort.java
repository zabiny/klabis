package com.klabis.finance.application;

import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

@PrimaryPort
public interface TransactionQueryPort {

    record TransactionQuery(
            MemberId memberId,
            LocalDate occurredAtFrom,
            LocalDate occurredAtTo,
            TransactionType type,
            org.springframework.data.domain.Pageable pageable
    ) {}

    Page<Transaction> findTransactions(TransactionQuery query);

    Page<TransactionWithReversal> findTransactionsWithReversals(TransactionQuery query);

    Transaction findTransaction(MemberId memberId, TransactionId transactionId);
}
