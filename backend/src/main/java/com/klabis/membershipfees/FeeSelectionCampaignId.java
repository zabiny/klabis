package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record FeeSelectionCampaignId(UUID value) implements Identifier {

    public FeeSelectionCampaignId {
        if (value == null) {
            throw new IllegalArgumentException("FeeSelectionCampaignId value is required");
        }
    }


}
