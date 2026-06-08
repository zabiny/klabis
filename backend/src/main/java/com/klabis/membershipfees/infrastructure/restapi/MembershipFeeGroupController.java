package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
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

    MembershipFeeGroupController(FeeYearPublicationManagementPort managementPort) {
        this.managementPort = managementPort;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get membership fee group details with snapshot and member count")
    ResponseEntity<EntityModel<MembershipFeeGroupResponse>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {
        MembershipFeeGroup group = managementPort.getGroup(new MembershipFeeGroupId(id));
        return ResponseEntity.ok(entityModelWithDomain(MembershipFeeGroupResponse.from(group), group));
    }
}

@MvcComponent
class MembershipFeeGroupDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeGroupResponse, MembershipFeeGroup> {

    @Override
    public void process(EntityModel<MembershipFeeGroupResponse> dtoModel, MembershipFeeGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(id))
                .map(link -> link.withSelfRel())
                .ifPresent(dtoModel::add);
    }
}
