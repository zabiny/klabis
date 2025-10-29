package club.klabis.finance.domain.events;

import com.dpolach.eventsourcing.BaseEvent;

public abstract class AccountEvent extends BaseEvent {

    public AccountEvent() {
        super(AccountEvent.class);
    }
}
