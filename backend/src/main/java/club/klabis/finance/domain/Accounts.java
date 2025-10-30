package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.CompositeEventsSource;
import com.dpolach.eventsourcing.Projector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @deprecated Replace with Projector adapter which will define "Create" events and all other will delegate to Account (EventSourcedAggregateRoot)
 */
@Deprecated
public class Accounts extends CompositeEventsSource<Account> implements Projector<Account> {

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

    @Override
    public void handleEvent(BaseEvent event) {
        switch (event) {
            case AccountCreatedEvent createAccountEvent -> createAccount(createAccountEvent.getMemberId(),
                    createAccountEvent.getInitialBalance());
            default -> super.handleEvent(event);
        }
    }

    @Override
    public void project(BaseEvent event) {
        this.apply(event);
    }
}
