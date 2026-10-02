package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.jmolecules.ddd.annotation.Service;

import java.util.List;

@Service
class MembershipFeeTierOptionsService implements MembershipFeeTierOptionsPort {

    private final RankingOptionsPort rankingOptionsPort;
    private final EventTypeOptionsPort eventTypeOptionsPort;

    MembershipFeeTierOptionsService(RankingOptionsPort rankingOptionsPort, EventTypeOptionsPort eventTypeOptionsPort) {
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
