package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/invitations", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Invitations", description = "Invitation management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
class PendingInvitationsController {

    private final FreeGroupManagementPort membersGroupManagementService;

    PendingInvitationsController(FreeGroupManagementPort membersGroupManagementService) {
        this.membersGroupManagementService = membersGroupManagementService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List current user's pending invitations across all groups")
    ResponseEntity<CollectionModel<EntityModel<PendingInvitationResponse>>> getPendingInvitations(
            @ActingMember MemberId actingMember) {

        List<PendingInvitationView> views = membersGroupManagementService.getPendingInvitationsForMember(actingMember);
        List<EntityModel<PendingInvitationResponse>> items = views.stream()
                .map(InvitationModelBuilder::buildFromView)
                .toList();

        CollectionModel<EntityModel<PendingInvitationResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(PendingInvitationsController.class).getPendingInvitations(null))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return ResponseEntity.ok(model);
    }
}
