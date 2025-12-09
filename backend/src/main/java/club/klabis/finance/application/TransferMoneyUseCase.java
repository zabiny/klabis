package club.klabis.finance.application;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.AccountProjector;
import club.klabis.finance.domain.MoneyAmount;
import club.klabis.finance.domain.events.TransferedAmountEvent;
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
        Account fromAccount = eventsRepository.project(new AccountProjector(from))
                .orElseThrow(() -> new IllegalStateException("Source account not found"));
        Account toAccount = eventsRepository.project(new AccountProjector(to))
                .orElseThrow(() -> new IllegalStateException("Target account not found"));

        if (MoneyAmount.ZERO.equals(amount)) {
            throw new IllegalStateException("Cannot transfer zero money amount");
        } else if (!fromAccount.canWithdraw(amount)) {
            throw new IllegalStateException("Insufficient funds on source account");
        }

        eventsRepository.appendEvent(new TransferedAmountEvent(fromAccount.getOwner(), toAccount.getOwner(), amount));
    }

}
