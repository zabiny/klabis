package com.klabis.finance.application;

import com.klabis.finance.domain.Transaction;
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
            String type,
            org.springframework.data.domain.Pageable pageable
    ) {}

    Page<Transaction> findTransactions(TransactionQuery query);
}
