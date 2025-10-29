package club.klabis.finance.domain.events;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;

public class DepositedAmountEvent extends AccountEvent {
    private final MemberId to;
    private final MoneyAmount amount;

    public DepositedAmountEvent(MemberId to, MoneyAmount amount) {
        this.to = to;
        this.amount = amount;
    }

    public MoneyAmount getAmount() {
        return amount;
    }

    public MemberId getTo() {
        return to;
    }
}
