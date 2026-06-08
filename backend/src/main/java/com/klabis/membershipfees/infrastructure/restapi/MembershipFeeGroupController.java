package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/membership-fee-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MembershipFeeGroups", description = "Published fee level group details API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(MembershipFeeGroup.class)
class MembershipFeeGroupController {

    private final FeeYearPublicationManagementPort managementPort;
    private final AdminFeeAssignmentPort adminFeeAssignmentPort;

    MembershipFeeGroupController(FeeYearPublicationManagementPort managementPort,
                                 AdminFeeAssignmentPort adminFeeAssignmentPort) {
        this.managementPort = managementPort;
        this.adminFeeAssignmentPort = adminFeeAssignmentPort;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get membership fee group details with snapshot and member count")
    ResponseEntity<EntityModel<MembershipFeeGroupResponse>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));
        return ResponseEntity.ok(entityModelWithDomain(MembershipFeeGroupResponse.from(group), group));
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Edit yearly fee and payment rules of a published level (requires MEMBERS:MANAGE, only while EDITABLE)")
    ResponseEntity<Void> editSnapshot(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody EditGroupSnapshotRequest request) {
        managementPort.editGroupSnapshot(new MembershipFeeGroupId(id), request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{groupId}/members/{memberId}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Assign a member to a fee group (admin emergency assignment, requires MEMBERS:MANAGE)")
    ResponseEntity<Void> assignMember(
            @Parameter(description = "Fee group UUID") @PathVariable UUID groupId,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @Valid @RequestBody AdminAssignMemberRequest request,
            @ActingMember MemberId actingAdmin) {

        adminFeeAssignmentPort.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                actingAdmin,
                new MemberId(memberId),
                new MembershipFeeGroupId(groupId),
                request.year()));

        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class MembershipFeeGroupDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeGroupResponse, MembershipFeeGroup> {

    @Override
    public void process(EntityModel<MembershipFeeGroupResponse> dtoModel, MembershipFeeGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(id))
                .map(link -> {
                    var self = link.withSelfRel()
                            .andAffordances(klabisAfford(
                                    methodOn(MembershipFeeGroupController.class).assignMember(id, null, null, null)));
                    if (group.getStatus() == PublishedLevelStatus.EDITABLE) {
                        self = self.andAffordances(klabisAfford(
                                methodOn(MembershipFeeGroupController.class).editSnapshot(id, null)));
                    }
                    return self;
                })
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).getLevel(group.getSourceLevelId().uuid()))
                .ifPresent(link -> dtoModel.add(link.withRel("sourceLevel")));
    }
}
