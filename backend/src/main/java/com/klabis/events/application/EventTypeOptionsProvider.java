package com.klabis.events.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;

/**
 * Secondary port for fetching event type options from the events module.
 * <p>
 * Allows consuming modules (e.g. membershipfees) to read available event types
 * as inline options for HAL+FORMS affordances, without depending on the events
 * module's primary port {@link EventTypeManagementPort}.
 */
@SecondaryPort
public interface EventTypeOptionsProvider {

    /**
     * Returns event type options sorted by their display order.
     * Each option carries the event type UUID as value and its name as human-readable prompt.
     * Returns an empty list when no event types are configured.
     */
    List<HalFormsInlineOption> listEventTypeOptions();
}
