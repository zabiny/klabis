package club.klabis.domain.appusers.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.domain.appusers.ApplicationUser;

@org.jmolecules.event.annotation.DomainEvent
public class ApplicationUserEnableStatusChanged extends AggregateDomainEvent<ApplicationUser> {
    public ApplicationUserEnableStatusChanged(ApplicationUser aggregate) {
        super(aggregate);
    }

    public ApplicationUser.Id getUserId() {
        return getAggregate().getId();
    }

    public boolean isUserDisabled() {
        return getAggregate().isDisabled();
    }

    public boolean isUserEnabled() {
        return !isUserDisabled();
    }
}
