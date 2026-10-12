package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.infrastructure.restapi.InvitationsApi;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponse;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// PendingInvitationResponse items here never carry the owner-only "self"+cancelInvitation
// affordance that the same DTO carries when embedded inside GroupResponse — see
// EntityModelPendingInvitationResponseForInvitationsList in groups.yaml. Links are added by
// PendingInvitationPostprocessor below, keyed off the domain PendingInvitationView.

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class PendingInvitationsController implements InvitationsApi {

    private final FreeGroupManagementPort membersGroupManagementService;
    private final ConversionService conversionService;

    PendingInvitationsController(FreeGroupManagementPort membersGroupManagementService, ConversionService conversionService) {
        this.membersGroupManagementService = membersGroupManagementService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<PendingInvitationResponse>> getPendingInvitations(
            @ActingMember MemberId actingMember) {

        List<PendingInvitationView> views = membersGroupManagementService.getPendingInvitationsForMember(actingMember);

        HalResponseContext.setDomainList(views);
        return ResponseEntity.ok(views.stream()
                .map(view -> conversionService.convert(view, PendingInvitationResponse.class))
                .toList());
    }
}

@MvcComponent
class PendingInvitationPostprocessor
        extends ModelWithDomainPostprocessor<PendingInvitationResponse, PendingInvitationView> {

    @Override
    public void process(EntityModel<PendingInvitationResponse> dtoModel, PendingInvitationView view) {
        InvitationLinks.addRecipientLinks(dtoModel, view.groupId().uuid(), view.invitation());
    }
}
