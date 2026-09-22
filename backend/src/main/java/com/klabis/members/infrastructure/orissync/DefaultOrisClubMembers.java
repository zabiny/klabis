package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.common.OrisIntegrationComponent;
import org.jmolecules.architecture.hexagonal.Application;

import java.util.Map;

/**
 * The only implementation of {@link OrisClubMembers} (design.md D9): reads the key
 * from {@link InMemoryOrisClubKeyAdapter} itself — never taking it as a parameter —
 * so a caller has no way to obtain it and no way to leak it.
 */
@OrisIntegrationComponent
@Application
class DefaultOrisClubMembers implements OrisClubMembers {

    private final OrisApiClient orisApiClient;
    private final InMemoryOrisClubKeyAdapter clubKeyPort;

    DefaultOrisClubMembers(OrisApiClient orisApiClient, InMemoryOrisClubKeyAdapter clubKeyPort) {
        this.orisApiClient = orisApiClient;
        this.clubKeyPort = clubKeyPort;
    }

    @Override
    public Map<String, ClubMember> listClubMembers() {
        if (!clubKeyPort.isSet()) {
            throw new ClubKeyNotSetException();
        }
        return orisApiClient.getClubUserList(clubKeyPort.currentKey()).payload().orElseGet(Map::of);
    }
}
