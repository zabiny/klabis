package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.AccountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.Projector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

public class AccountProjector implements Projector<Account> {

    private static final Logger LOG = LoggerFactory.getLogger(AccountProjector.class);

    private Account result;
    private final MemberId accountToRebuild;

    public AccountProjector(MemberId accountToRebuild) {
        this.accountToRebuild = accountToRebuild;
    }

    @Override
    public void project(BaseEvent event) {
        if (event instanceof AccountEvent accountEvent) {
            this.applyTyped(accountEvent);
        }
    }

    @Override
    public void completed() {
        if (result != null) {
            result.clearPendingEvents();
        }
    }

    public void applyTyped(AccountEvent accountEvent) {
        switch (accountEvent) {
            case AccountCreatedEvent accountCreatedEvent -> setResultIfApplicable(accountCreatedEvent.getMemberId(),
                    () -> new Account(accountCreatedEvent.getMemberId(), accountCreatedEvent.getInitialBalance()));
            default -> result.apply(accountEvent);
        }
    }

    private void setResultIfApplicable(MemberId eventAccountId, Supplier<Account> accountSupplier) {
        if (!eventAccountId.equals(accountToRebuild)) {
            return;
        }
        if (result == null) {
            result = accountSupplier.get();
        } else {
            throw new IllegalStateException(
                    "Account already set - are there duplicate events for creation of one account?");
        }
    }

    @Override
    public Optional<Account> getResult() {
        return Optional.ofNullable(result);
    }
}
