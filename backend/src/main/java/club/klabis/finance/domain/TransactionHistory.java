package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import club.klabis.finance.domain.events.TransferedAmountEvent;
import club.klabis.finance.domain.events.WithdrawnAmountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.Projector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


public class TransactionHistory implements Projector<List<TransactionHistory.TransactionItem>> {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionHistory.class);

    private final MemberId accountOwner;
    private List<TransactionItem> data = new ArrayList<>();

    public TransactionHistory(MemberId accountOwner) {
        this.accountOwner = accountOwner;
    }

    @Override
    public void project(BaseEvent event) {
        if (event instanceof AccountEvent accountEvent) {
            this.projectTyped(accountEvent);
        }
    }

    @Override
    public Optional<List<TransactionItem>> getResult() {
        return Optional.of(Collections.unmodifiableList(data));
    }

    private void projectTyped(AccountEvent accountEvent) {
        switch (accountEvent) {
            case DepositedAmountEvent deposit -> addIfBelongTo(deposit.getTo(), () -> TransactionItem.deposit(deposit));
            case WithdrawnAmountEvent withdaw ->
                    addIfBelongTo(withdaw.getFrom(), () -> TransactionItem.withdraw(withdaw));
            case TransferedAmountEvent transferEvent -> {
                addIfBelongTo(transferEvent.getFrom(), () -> TransactionItem.transferSource(transferEvent));
                addIfBelongTo(transferEvent.getTo(), () -> TransactionItem.transferTarget(transferEvent));
            }
            default -> {
                LOG.debug("Unhandled event type: {}", accountEvent);
            }
        }
    }

    private void addIfBelongTo(MemberId eventAccountId, Supplier<TransactionItem> transactionItem) {
        if (eventAccountId.equals(accountOwner)) {
            data.add(transactionItem.get());
        }
    }

    public List<TransactionItem> getData() {
        return Collections.unmodifiableList(data);
    }

    public record TransactionItem(LocalDate date, MoneyAmount amount, String note) {

        static TransactionItem withdraw(WithdrawnAmountEvent event) {
            return new TransactionItem(event.getCreatedAt().toLocalDate(), event.getAmount(), "Platba");
        }

        static TransactionItem deposit(DepositedAmountEvent event) {
            return new TransactionItem(event.getCreatedAt().toLocalDate(), event.getAmount(), "Vklad");
        }

        static TransactionItem transferSource(TransferedAmountEvent event) {
            return new TransactionItem(event.getCreatedAt().toLocalDate(), event.getAmount(), "Prevod na");
        }

        static TransactionItem transferTarget(TransferedAmountEvent event) {
            return new TransactionItem(event.getCreatedAt().toLocalDate(), event.getAmount(), "Prevod z");
        }
    }
}
