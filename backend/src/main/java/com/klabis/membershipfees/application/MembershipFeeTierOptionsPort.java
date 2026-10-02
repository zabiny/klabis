package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface MembershipFeeTierOptionsPort {

    /**
     * Returns ORIS ranking options available for assignment to payment rules.
     * Returns an empty list when ORIS integration is not active or unavailable.
     */
    List<HalFormsInlineOption> listRankingOptions();

    /**
     * Returns event type options available for assignment to payment rules.
     * Returns an empty list when no event types are configured.
     */
    List<HalFormsInlineOption> listEventTypeOptions();
}
