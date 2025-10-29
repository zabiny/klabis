package club.klabis.finance.domain.events;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;

public class WithdrawnAmountEvent extends AccountEvent {
    private final MemberId from;
    private final MoneyAmount amount;

    public WithdrawnAmountEvent(MemberId from, MoneyAmount amount) {
        this.from = from;
        this.amount = amount;
    }

    public MoneyAmount getAmount() {
        return amount;
    }

    public MemberId getFrom() {
        return from;
    }
}
