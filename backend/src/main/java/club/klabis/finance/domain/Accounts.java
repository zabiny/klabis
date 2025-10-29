package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.TransferedAmountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.CompositeEventsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Accounts extends CompositeEventsSource<Account> {

    private static final Logger LOG = LoggerFactory.getLogger(Accounts.class);

    public Accounts() {
        this(Collections.emptyList());
    }

    public Accounts(Collection<Account> accounts) {
        super(accounts);
    }

    public Account createAccount(MemberId memberId, MoneyAmount initialBalance) {
        Account result = new Account(memberId, initialBalance);
        addItem(result);
        return result;
    }

    public Optional<Account> getAccount(MemberId memberId) {
        return streamItems().filter(it -> it.getOwner().equals(memberId)).reduce((acc1, acc2) -> {
            throw new IllegalStateException("Multiple accounts found for memberId %s".formatted(memberId));
        });
    }

    public Account getAccountOrThrow(MemberId memberId) {
        return getAccount(memberId).orElseThrow(() -> new IllegalStateException("Account for member %s not found".formatted(
                memberId.value())));
    }

    public void transferMoney(MemberId from, MemberId to, MoneyAmount amount) {
        Account fromAccount = getAccount(from)
                .orElseThrow(() -> new IllegalStateException("Source account not found"));
        Account targetAccount = getAccount(to)
                .orElseThrow(() -> new IllegalStateException("Target account not found"));

        if (MoneyAmount.ZERO.equals(amount)) {
            throw new IllegalStateException("Cannot transfer zero money amount");
        }
        if (!fromAccount.canWithdraw(amount)) {
            throw new IllegalStateException("Insufficient funds in source account");
        }

        andEvent(new TransferedAmountEvent(from, to, amount));
    }

    @Override
    public void handleEvent(BaseEvent event) {
        switch (event) {
            case AccountCreatedEvent createAccountEvent -> createAccount(createAccountEvent.getMemberId(),
                    createAccountEvent.getInitialBalance());
            default -> super.handleEvent(event);
        }
    }

}
