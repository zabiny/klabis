package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/membership-fee-levels", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MembershipFeeLevels", description = "Membership fee level catalog management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(MembershipFeeLevel.class)
class MembershipFeeLevelController {

    private final MembershipFeeLevelManagementPort managementPort;

    MembershipFeeLevelController(MembershipFeeLevelManagementPort managementPort) {
        this.managementPort = managementPort;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Create a membership fee level (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> createLevel(@Valid @RequestBody CreateMembershipFeeLevelRequest request) {
        MembershipFeeLevelManagementPort.CreateLevelCommand command = request.toCommand();
        MembershipFeeLevelId id = managementPort.createLevel(command);
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeLevelController.class).getLevel(id.uuid())).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List all membership fee levels")
    ResponseEntity<CollectionModel<EntityModel<MembershipFeeLevelSummaryResponse>>> listLevels() {
        List<MembershipFeeLevel> levels = managementPort.listLevels();
        List<EntityModel<MembershipFeeLevelSummaryResponse>> items = levels.stream()
                .map(this::buildSummaryModel)
                .toList();

        CollectionModel<EntityModel<MembershipFeeLevelSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).listLevels())
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeLevelController.class).createLevel(null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get membership fee level details")
    ResponseEntity<EntityModel<MembershipFeeLevelResponse>> getLevel(
            @Parameter(description = "Level UUID") @PathVariable UUID id) {
        MembershipFeeLevel level = managementPort.getLevel(new MembershipFeeLevelId(id));
        MembershipFeeLevelResponse response = MembershipFeeLevelResponse.from(level);
        return ResponseEntity.ok(entityModelWithDomain(response, level));
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Edit a membership fee level (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> editLevel(
            @Parameter(description = "Level UUID") @PathVariable UUID id,
            @Valid @RequestBody EditMembershipFeeLevelRequest request) {
        MembershipFeeLevelManagementPort.EditLevelCommand command = request.toCommand();
        managementPort.editLevel(new MembershipFeeLevelId(id), command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Delete a membership fee level (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> deleteLevel(
            @Parameter(description = "Level UUID") @PathVariable UUID id) {
        managementPort.deleteLevel(new MembershipFeeLevelId(id));
        return ResponseEntity.noContent().build();
    }

    private EntityModel<MembershipFeeLevelSummaryResponse> buildSummaryModel(MembershipFeeLevel level) {
        UUID levelId = level.getId().uuid();
        MembershipFeeLevelSummaryResponse summary = MembershipFeeLevelSummaryResponse.from(level);
        EntityModel<MembershipFeeLevelSummaryResponse> model = EntityModel.of(summary);
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).getLevel(levelId))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }
}

@MvcComponent
class MembershipFeeLevelDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeLevelResponse, MembershipFeeLevel> {

    @Override
    public void process(EntityModel<MembershipFeeLevelResponse> dtoModel, MembershipFeeLevel level) {
        UUID id = level.getId().uuid();
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).getLevel(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeLevelController.class).editLevel(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeLevelController.class).deleteLevel(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).listLevels())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

@MvcComponent
class MembershipFeeLevelsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembershipFeeLevelController.class).listLevels())
                .ifPresent(link -> model.add(link.withRel("membership-fee-levels")));
        return model;
    }
}
