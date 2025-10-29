package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import club.klabis.finance.domain.events.TransferedAmountEvent;
import club.klabis.finance.domain.events.WithdrawnAmountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.SingleEventsSource;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class Account extends SingleEventsSource {
    @Identity
    private final MemberId owner;
    private MoneyAmount balance;

    Account(MemberId owner, MoneyAmount initialBalance) {
        this.owner = owner;
        this.balance = initialBalance;
        this.andEvent(new AccountCreatedEvent(owner, initialBalance));
    }

    public void deposit(MoneyAmount amount) {
        this.balance = this.balance.add(amount);
        andEvent(new DepositedAmountEvent(owner, amount));
    }

    public boolean canWithdraw(MoneyAmount amount) {
        return amount.isLowerThan(this.balance);
    }

    public void withdraw(MoneyAmount amount) {
        this.balance = this.balance.subtract(amount);
        andEvent(new WithdrawnAmountEvent(owner, amount));
    }

    public MemberId getOwner() {
        return owner;
    }

    public MoneyAmount getBalance() {
        return balance;
    }

    @Override
    public void handleEvent(BaseEvent event) {
        switch (event) {
            case DepositedAmountEvent depositEvent -> deposit(depositEvent.getAmount());
            case WithdrawnAmountEvent withdrawnAmountEvent -> withdraw(withdrawnAmountEvent.getAmount());
            case TransferedAmountEvent transferEvent -> {
                if (owner.equals(transferEvent.getFrom())) {
                    withdraw(transferEvent.getAmount());
                } else if (owner.equals(transferEvent.getTo())) {
                    deposit(transferEvent.getAmount());
                }
            }
            default -> super.handleEvent(event);
        }
    }
}
