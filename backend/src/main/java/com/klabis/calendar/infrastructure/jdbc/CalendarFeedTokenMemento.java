package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarFeedToken;
import com.klabis.common.users.UserId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "calendar", value = "calendar_feed_token")
class CalendarFeedTokenMemento implements Persistable<UUID> {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("token_hash")
    private String tokenHash;

    @Column("token_lookup")
    private String tokenLookup;

    @Column("last_set_at")
    private Instant lastSetAt;

    @Transient
    private boolean isNew;

    protected CalendarFeedTokenMemento() {
    }

    static CalendarFeedTokenMemento from(CalendarFeedToken token) {
        Assert.notNull(token, "CalendarFeedToken must not be null");

        CalendarFeedTokenMemento m = new CalendarFeedTokenMemento();
        m.userId = token.getUserId().uuid();
        m.tokenHash = token.getTokenHash();
        m.tokenLookup = token.getTokenLookup();
        m.lastSetAt = token.getLastSetAt();
        m.isNew = token.isNew();
        return m;
    }

    CalendarFeedToken toDomain() {
        return CalendarFeedToken.reconstruct(
                new UserId(userId),
                tokenHash,
                tokenLookup,
                lastSetAt
        );
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
