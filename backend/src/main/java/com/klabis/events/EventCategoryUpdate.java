package com.klabis.events;

import com.klabis.events.domain.Money;

/**
 * One requested category on an event update: a new category when {@code id} is null, otherwise the
 * existing category with that id renamed and repriced.
 * <p>
 * It carries intent rather than a resolved {@link EventCategory} because category identity is
 * resolved by the aggregate that owns the categories.
 */
public record EventCategoryUpdate(EventCategoryId id, String name, Money fee) {
}
