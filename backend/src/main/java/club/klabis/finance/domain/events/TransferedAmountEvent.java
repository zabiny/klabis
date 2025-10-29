package club.klabis.finance.domain.events;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;

public class TransferedAmountEvent extends AccountEvent {
    private final MemberId from;
    private final MemberId to;
    private final MoneyAmount amount;


    public TransferedAmountEvent(MemberId from, MemberId to, MoneyAmount amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public MoneyAmount getAmount() {
        return amount;
    }

    public MemberId getFrom() {
        return from;
    }

    public MemberId getTo() {
        return to;
    }
}
