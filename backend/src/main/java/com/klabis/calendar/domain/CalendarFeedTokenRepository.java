package com.klabis.calendar.domain;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface CalendarFeedTokenRepository {

    CalendarFeedToken save(CalendarFeedToken token);

    Optional<CalendarFeedToken> findByUserId(UserId userId);

    /**
     * Finds all tokens whose lookup prefix matches the given value.
     * Used for indexed pre-filter before bcrypt comparison.
     */
    List<CalendarFeedToken> findByTokenLookup(String tokenLookup);
}
