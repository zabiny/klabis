package com.klabis.finance.domain;

import com.klabis.members.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberAccountRepository {

    MemberAccount save(MemberAccount account);

    Optional<MemberAccount> findById(MemberId memberId);

    Optional<Money> findBalanceById(MemberId memberId);

    Optional<Transaction> findReversalOf(TransactionId transactionId);

    boolean existsById(MemberId memberId);

    Page<Transaction> findTransactions(MemberId memberId, LocalDate occurredAtFrom,
                                       LocalDate occurredAtTo, TransactionType type, Pageable pageable);
}
