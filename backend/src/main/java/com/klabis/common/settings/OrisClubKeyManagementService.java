package com.klabis.common.settings;

import org.springframework.stereotype.Service;

@Service
class OrisClubKeyManagementService implements OrisClubKeyManagementPort {

    private final OrisClubKeyPort orisClubKeyPort;

    OrisClubKeyManagementService(OrisClubKeyPort orisClubKeyPort) {
        this.orisClubKeyPort = orisClubKeyPort;
    }

    @Override
    public void store(String clubKey) {
        orisClubKeyPort.store(clubKey);
    }

    @Override
    public boolean isSet() {
        return orisClubKeyPort.isSet();
    }

    @Override
    public void clear() {
        orisClubKeyPort.clear();
    }
}
