package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;

@SecondaryPort
public interface EventTypeOptionsPort {

    /**
     * Returns event type options available for assignment to payment rules.
     * Each option carries the event type UUID as value and its name as human-readable prompt.
     * Returns an empty list when no event types are configured.
     */
    List<HalFormsInlineOption> listEventTypeOptions();
}
