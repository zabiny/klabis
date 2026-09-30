package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.members.infrastructure.restapi.LegalGuardianOptionResponse;
import com.klabis.members.infrastructure.restapi.LegalGuardianOptionResponseBuilder;
import com.klabis.members.infrastructure.restapi.LegalGuardianOptionResponseKind;
import com.klabis.members.infrastructure.restapi.LegalGuardianOptionsApi;
import com.klabis.members.legalguardian.application.GuardianCandidate;
import com.klabis.members.legalguardian.application.GuardianCandidatesPort;
import com.klabis.members.legalguardian.application.GuardianKind;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class LegalGuardianOptionsController implements LegalGuardianOptionsApi {

    private final GuardianCandidatesPort guardianCandidatesService;

    LegalGuardianOptionsController(GuardianCandidatesPort guardianCandidatesService) {
        this.guardianCandidatesService = guardianCandidatesService;
    }

    @Override
    public ResponseEntity<List<LegalGuardianOptionResponse>> listLegalGuardianOptions(String q) {
        return ResponseEntity.ok(guardianCandidatesService.findCandidates(q).stream()
                .map(LegalGuardianOptionsController::toResponse)
                .toList());
    }

    private static LegalGuardianOptionResponse toResponse(GuardianCandidate candidate) {
        return LegalGuardianOptionResponseBuilder.builder()
                .value(candidate.userId().uuid().toString())
                .prompt(candidate.displayName())
                .kind(candidate.kind() == GuardianKind.MEMBER
                        ? LegalGuardianOptionResponseKind.MEMBER
                        : LegalGuardianOptionResponseKind.LEGAL_GUARDIAN)
                .registrationNumber(candidate.registrationNumber())
                .email(candidate.email())
                .build();
    }
}
