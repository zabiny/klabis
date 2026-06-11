package com.klabis.membershipfees.infrastructure;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.LevelListEntry;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.membershipfees.application.RankingOptionsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Component
class OrisRankingOptionsAdapter implements RankingOptionsPort {

    private static final Logger log = LoggerFactory.getLogger(OrisRankingOptionsAdapter.class);

    private final Optional<OrisApiClient> orisApiClient;

    OrisRankingOptionsAdapter(Optional<OrisApiClient> orisApiClient) {
        this.orisApiClient = orisApiClient;
    }

    @Override
    public List<HalFormsInlineOption> listRankingOptions() {
        if (orisApiClient.isEmpty()) {
            log.debug("ORIS client not configured, returning empty ranking options");
            return Collections.emptyList();
        }
        try {
            List<HalFormsInlineOption> options = orisApiClient.get().listLevels().payload()
                    .map(levels -> levels.values().stream()
                            .map(OrisRankingOptionsAdapter::toInlineOption)
                            .toList())
                    .orElse(Collections.emptyList());
            log.debug("ORIS ranking options loaded: {} entries", options.size());
            return options;
        } catch (RuntimeException e) {
            log.warn("ORIS level list unavailable, returning empty ranking options", e);
            return Collections.emptyList();
        }
    }

    private static HalFormsInlineOption toInlineOption(LevelListEntry entry) {
        String prompt = (entry.descriptionCZ() != null && !entry.descriptionCZ().isBlank())
                ? entry.descriptionCZ()
                : entry.name();
        return new HalFormsInlineOption(entry.id(), prompt);
    }
}
