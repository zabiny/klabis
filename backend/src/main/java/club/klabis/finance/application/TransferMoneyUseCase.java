package club.klabis.finance.application;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.Accounts;
import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;
import club.klabis.shared.config.ddd.UseCase;
import com.dpolach.eventsourcing.EventsRepository;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class TransferMoneyUseCase {

    private final EventsRepository eventsRepository;

    public TransferMoneyUseCase(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    @Transactional
    public void transferMoney(MemberId from, MemberId to, MoneyAmount amount) {
        Accounts accounts = eventsRepository.rebuild(new Accounts());

        Account fromAccount = accounts.getAccount(from)
                .orElseThrow(() -> new IllegalStateException("Source account not found"));
        accounts.getAccount(to)
                .orElseThrow(() -> new IllegalStateException("Target account not found"));

        fromAccount.transferTo(to, amount);

        eventsRepository.appendPendingEventsFrom(fromAccount);
    }

}
