package club.klabis.finance.application;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.AccountProjector;
import club.klabis.finance.domain.TransactionHistory;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.EventsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class AccountsEventSourcedService implements AccountsService {

    private final EventsRepository eventsRepository;

    public AccountsEventSourcedService(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    @Override
    public Optional<Account> getAccountForMember(MemberId ownerId) {
        return eventsRepository.project(new AccountProjector(ownerId));
    }

    @Override
    public List<TransactionHistory.TransactionItem> getTransactionHistory(MemberId ownerId) {
        return eventsRepository.project(new TransactionHistory(ownerId)).orElse(List.of());
    }

    @Override
    public void deposit(MemberId owner, DepositAction depositAction) {
        getAccountForMember(owner).map(depositAction::apply)
                .ifPresentOrElse(eventsRepository::appendPendingEventsFrom, () -> {
                    throw new AccountNotFoundException(owner);
                });

    }
}
