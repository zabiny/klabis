package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.application.AdminFeeAssignmentPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/membership-fee-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MembershipFeeGroups", description = "Published fee level group details API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(MembershipFeeGroup.class)
class MembershipFeeGroupController {

    private final FeeSelectionCampaignManagementPort managementPort;
    private final AdminFeeAssignmentPort adminFeeAssignmentPort;
    private final Members members;
    private final MembershipFeeGroupDetailsPostprocessor groupDetailsPostprocessor;

    MembershipFeeGroupController(FeeSelectionCampaignManagementPort managementPort,
                                 AdminFeeAssignmentPort adminFeeAssignmentPort,
                                 Members members,
                                 MembershipFeeGroupDetailsPostprocessor groupDetailsPostprocessor) {
        this.managementPort = managementPort;
        this.adminFeeAssignmentPort = adminFeeAssignmentPort;
        this.members = members;
        this.groupDetailsPostprocessor = groupDetailsPostprocessor;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get membership fee group details with snapshot and member count")
    ResponseEntity<RepresentationModel<?>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));
        EntityModel<MembershipFeeGroupResponse> entityModel = entityModelWithDomain(MembershipFeeGroupResponse.from(group), group);
        groupDetailsPostprocessor.process(entityModel, group);

        List<MembershipFeeGroupResponse.MemberInGroupResponse> groupMembers = buildGroupMembers(group);
        RepresentationModel<?> model = HalModelBuilder.halModelOf(entityModel)
                .embed(groupMembers, MembershipFeeGroupResponse.MemberInGroupResponse.class)
                .build();
        return ResponseEntity.ok(model);
    }

    private List<MembershipFeeGroupResponse.MemberInGroupResponse> buildGroupMembers(MembershipFeeGroup group) {
        Set<FeeGroupMembership> memberships = group.getMemberships();
        if (memberships.isEmpty()) {
            return List.of();
        }
        Set<MemberId> memberIds = memberships.stream().map(FeeGroupMembership::memberId).collect(Collectors.toSet());
        Map<MemberId, MemberDto> memberDtos = members.findByIds(memberIds);
        return memberships.stream()
                .map(membership -> MembershipFeeGroupResponse.MemberInGroupResponse.from(membership, memberDtos.get(membership.memberId())))
                .toList();
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

    @GetMapping("/{id}/rules")
    @Operation(summary = "List payment rules snapshot for a membership fee group")
    ResponseEntity<CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>>> listGroupRules(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));
        var items = group.getRulesSnapshot().stream()
                .map(rule -> EntityModel.of(MembershipFeeTierResponse.PaymentRuleResponse.from(rule)))
                .toList();
        CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(MembershipFeeGroupController.class).listGroupRules(id))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return ResponseEntity.ok(model);
    }

    @PostMapping(value = "/{groupId}/members", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Assign a member to a fee group (admin emergency assignment, requires MEMBERS:MANAGE)")
    ResponseEntity<Void> assignMember(
            @Parameter(description = "Fee group UUID") @PathVariable UUID groupId,
            @Valid @RequestBody AdminAssignMemberRequest request,
            @ActingMember MemberId actingAdmin) {

        adminFeeAssignmentPort.assignLevel(new AdminFeeAssignmentPort.AssignFeeLevel(
                actingAdmin,
                new MemberId(request.memberId()),
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
        UUID id = group.getId().value();
        klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(id))
                .map(link -> {
                    var self = link.withSelfRel()
                            .andAffordances(klabisAfford(
                                    methodOn(MembershipFeeGroupController.class).assignMember(id, null, null)));
                    if (group.getStatus() == PublishedLevelStatus.EDITABLE) {
                        self = self.andAffordances(klabisAfford(
                                methodOn(MembershipFeeGroupController.class).editSnapshot(id, null)));
                    }
                    return self;
                })
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeGroupController.class).listGroupRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
        klabisLinkTo(methodOn(MembershipFeeTierController.class).getTier(group.getSourceLevelId().value()))
                .ifPresent(link -> dtoModel.add(link.withRel("sourceLevel")));
    }

}
