package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.domain.EventCategory;
import com.klabis.events.domain.EventCategoryId;
import com.klabis.events.domain.Money;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Memento for a single row in events.event_categories child table.
 * <p>
 * The category's own {@link EventCategoryId} is used as the primary key (not a synthetic
 * position/sequence), so that renaming a category on update preserves its identity and,
 * with it, every {@code EventRegistration} that already points at it by id.
 */
@Table(schema = "events", value = "event_categories")
class EventCategoryMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("oris_id")
    private String orisId;

    @Column("name")
    private String name;

    @Column("fee_amount")
    private BigDecimal feeAmount;

    @Column("fee_currency")
    private String feeCurrency;

    protected EventCategoryMemento() {
    }

    static EventCategoryMemento from(EventCategory category) {
        EventCategoryMemento memento = new EventCategoryMemento();
        memento.id = category.id().value();
        memento.orisId = category.orisId();
        memento.name = category.name();
        category.fee().ifPresent(fee -> {
            memento.feeAmount = fee.amount();
            memento.feeCurrency = fee.currency().getCurrencyCode();
        });
        return memento;
    }

    EventCategory toEventCategory() {
        Money fee = feeAmount != null ? Money.of(feeAmount, Currency.getInstance(feeCurrency)) : null;
        return new EventCategory(new EventCategoryId(id), orisId, name, fee);
    }
}
