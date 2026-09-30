package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberId;
import com.klabis.members.application.MemberAccountActivationPort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class MemberAccountActivationController implements MemberAccountActivationApi {

    private final MemberAccountActivationPort accountActivationPort;

    MemberAccountActivationController(MemberAccountActivationPort accountActivationPort) {
        this.accountActivationPort = accountActivationPort;
    }

    @Override
    public ResponseEntity<Void> sendMemberAccountActivation(UUID id) {
        accountActivationPort.sendAccountActivation(new MemberId(id));
        return ResponseEntity.noContent().build();
    }
}
