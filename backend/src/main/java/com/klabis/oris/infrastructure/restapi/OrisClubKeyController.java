package com.klabis.oris.infrastructure.restapi;

import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.oris.ClubKeyStateResponse;
import com.klabis.oris.ClubKeyStateResponseBuilder;
import com.klabis.oris.OrisClubKeyApi;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The club-key resource never discloses the key itself (design.md D9/D10) — every response is
 * built from {@link OrisClubKeyManagementPort#isSet()} alone, and the port carries no getter to begin with.
 */
@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class OrisClubKeyController implements OrisClubKeyApi {

    private final OrisClubKeyManagementPort orisClubKeyManagementPort;

    OrisClubKeyController(OrisClubKeyManagementPort orisClubKeyManagementPort) {
        this.orisClubKeyManagementPort = orisClubKeyManagementPort;
    }

    @Override
    public ResponseEntity<ClubKeyStateResponse> getClubKeyState() {
        ClubKeyStateResponse response = currentState();
        HalResponseContext.setDomain(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubKeyStateResponse> setClubKey(com.klabis.oris.SetClubKeyRequest request) {
        orisClubKeyManagementPort.store(request.clubKey());
        ClubKeyStateResponse response = currentState();
        HalResponseContext.setDomain(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> clearClubKey() {
        orisClubKeyManagementPort.clear();
        return ResponseEntity.noContent().build();
    }

    private ClubKeyStateResponse currentState() {
        return ClubKeyStateResponseBuilder.builder()
                .isSet(orisClubKeyManagementPort.isSet())
                .build();
    }
}
