package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;

@SecondaryPort
public interface RankingOptionsPort {

    /**
     * Returns ORIS ranking options available for assignment to payment rules.
     * Each option carries the ranking short name (e.g. "A", "WRE") as value
     * and a human-readable Czech prompt.
     * Returns an empty list when ORIS integration is not active or unavailable.
     */
    List<HalFormsInlineOption> listRankingOptions();
}
