package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record SiPolicy(SiUsage usage, boolean rentalAllowed) {

    public SiPolicy {
        Assert.notNull(usage, "SI usage must not be null");
        // Rental of a card nobody uses is meaningless; normalising here keeps UI and domain in agreement.
        rentalAllowed = rentalAllowed && usage != SiUsage.NOT_USED;
    }

    public static SiPolicy of(SiUsage usage, boolean rentalAllowed) {
        return new SiPolicy(usage, rentalAllowed);
    }

    // ORIS races normally offer rental, so this is also what imported and programmatically created events get.
    public static SiPolicy defaultPolicy() {
        return new SiPolicy(SiUsage.REQUIRED, true);
    }

    public void validate(SiCardNumber siCardNumber, boolean wantsToRentSi) {
        if (usage == SiUsage.NOT_USED) {
            return;
        }
        if (wantsToRentSi && !rentalAllowed) {
            throw new BusinessRuleViolationException("SI card rental is not allowed for this event") {
            };
        }
        if (usage == SiUsage.REQUIRED && siCardNumber == null && !wantsToRentSi) {
            throw new BusinessRuleViolationException("SI card number is required") {
            };
        }
    }
}
