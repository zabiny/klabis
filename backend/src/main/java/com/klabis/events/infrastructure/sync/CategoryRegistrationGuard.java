package com.klabis.events.infrastructure.sync;

import com.klabis.events.EventCategory;
import com.klabis.events.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Purely local guard over the {@link Event} aggregate: logs a warning when an incoming ORIS sync
 * would drop a category that still has registrations. No ORIS types, no persistence — it just
 * inspects the aggregate and the incoming category list.
 */
@Component
class CategoryRegistrationGuard {

    private static final Logger log = LoggerFactory.getLogger(CategoryRegistrationGuard.class);

    void warnIfSyncRemovesCategoriesWithRegistrations(Event event, List<EventCategory> incomingCategories) {
        if (event.getRegistrations().isEmpty()) {
            return;
        }
        Set<String> incomingOrisIds = incomingCategories.stream()
                .map(EventCategory::orisId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Long> affectedCounts = event.getRegistrations().stream()
                .filter(r -> r.categoryId() != null)
                .map(r -> event.findCategory(r.categoryId()).orElse(null))
                .filter(category -> category != null && category.orisId() != null && !incomingOrisIds.contains(category.orisId()))
                .collect(Collectors.groupingBy(EventCategory::name, Collectors.counting()));
        if (!affectedCounts.isEmpty()) {
            log.warn("ORIS sync for event {} will remove categories that have existing registrations: {}",
                    event.getId(), affectedCounts);
        }
    }
}
