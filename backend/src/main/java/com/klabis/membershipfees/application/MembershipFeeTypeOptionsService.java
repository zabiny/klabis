package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.ddd.annotation.Service;

import java.util.List;

@Service
class MembershipFeeTypeOptionsService implements MembershipFeeTypeOptionsPort {

    private final RankingOptionsPort rankingOptionsPort;
    private final EventTypeOptionsPort eventTypeOptionsPort;

    MembershipFeeTypeOptionsService(RankingOptionsPort rankingOptionsPort, EventTypeOptionsPort eventTypeOptionsPort) {
        this.rankingOptionsPort = rankingOptionsPort;
        this.eventTypeOptionsPort = eventTypeOptionsPort;
    }

    @Override
    public List<HalFormsInlineOption> listRankingOptions() {
        return rankingOptionsPort.listRankingOptions();
    }

    @Override
    public List<HalFormsInlineOption> listEventTypeOptions() {
        return eventTypeOptionsPort.listEventTypeOptions();
    }
}
