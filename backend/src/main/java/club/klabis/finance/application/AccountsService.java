package club.klabis.finance.application;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.TransactionHistory;
import club.klabis.members.MemberId;

import java.util.List;
import java.util.Optional;

public interface AccountsService {
    Optional<Account> getAccountForMember(MemberId ownerId);

    List<TransactionHistory.TransactionItem> getTransactionHistory(MemberId ownerId);

    void deposit(MemberId owner, DepositAction depositAction);
}
