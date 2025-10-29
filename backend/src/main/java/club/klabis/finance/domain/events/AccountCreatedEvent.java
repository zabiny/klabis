package club.klabis.finance.domain.events;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;

public class AccountCreatedEvent extends AccountEvent {
    private final MemberId memberId;
    private final MoneyAmount initialBalance;

    public AccountCreatedEvent(MemberId memberId, MoneyAmount initialBalance) {
        this.memberId = memberId;
        this.initialBalance = initialBalance;
    }

    public MoneyAmount getInitialBalance() {
        return initialBalance;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
