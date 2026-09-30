package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.UserId;
import com.klabis.members.infrastructure.restapi.LegalGuardianResponse;
import com.klabis.members.infrastructure.restapi.LegalGuardianResponseBuilder;
import com.klabis.members.infrastructure.restapi.LegalGuardiansApi;
import com.klabis.members.infrastructure.restapi.UpdateLegalGuardianRequest;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianProfile;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class LegalGuardianController implements LegalGuardiansApi {

    private final LegalGuardianPort legalGuardianService;

    LegalGuardianController(LegalGuardianPort legalGuardianService) {
        this.legalGuardianService = legalGuardianService;
    }

    @Override
    public ResponseEntity<LegalGuardianResponse> getLegalGuardian(UUID userId) {
        LegalGuardianProfile profile = legalGuardianService.get(new UserId(userId));
        LegalGuardian guardian = profile.guardian();

        HalResponseContext.setDomain(guardian);
        return ResponseEntity.ok(LegalGuardianResponseBuilder.builder()
                .userId(guardian.getId().uuid())
                .loginName(profile.loginName())
                .firstName(guardian.getFirstName())
                .lastName(guardian.getLastName())
                .email(guardian.getEmail().value())
                .phone(guardian.getPhone().value())
                .build());
    }

    @Override
    public ResponseEntity<Void> updateLegalGuardian(UUID userId, UpdateLegalGuardianRequest request) {
        legalGuardianService.update(new UserId(userId), new LegalGuardian.UpdateLegalGuardian(
                request.firstName(), request.lastName(), request.email(), request.phone()));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class LegalGuardianDetailsPostprocessor extends ModelWithDomainPostprocessor<LegalGuardianResponse, LegalGuardian> {

    @Override
    public void process(EntityModel<LegalGuardianResponse> dtoModel, LegalGuardian guardian) {
        UUID id = guardian.getId().uuid();
        klabisLinkTo(methodOn(LegalGuardiansApi.class).getLegalGuardian(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(LegalGuardiansApi.class).updateLegalGuardian(id, null))))
                .ifPresent(dtoModel::add);
    }
}
