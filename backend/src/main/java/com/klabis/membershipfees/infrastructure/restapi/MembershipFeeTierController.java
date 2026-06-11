package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.RankingOptionsPort;
import com.klabis.membershipfees.domain.DuplicatePaymentRuleException;
import com.klabis.membershipfees.domain.MembershipFeeTier;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/membership-fee-tiers", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "MembershipFeeTiers", description = "Membership fee tier catalog management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(MembershipFeeTier.class)
class MembershipFeeTierController {

    private final MembershipFeeTierManagementPort managementPort;

    MembershipFeeTierController(MembershipFeeTierManagementPort managementPort) {
        this.managementPort = managementPort;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Create a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> createTier(@Valid @RequestBody CreateMembershipFeeTierRequest request) {
        MembershipFeeTierManagementPort.CreateTierCommand command = request.toCommand();
        MembershipFeeTierId id = managementPort.createTier(command);
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTierController.class).getTier(id.value())).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List all membership fee tiers")
    ResponseEntity<CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>>> listTiers() {
        List<MembershipFeeTier> tiers = managementPort.listTiers();
        List<EntityModel<MembershipFeeTierSummaryResponse>> items = tiers.stream()
                .map(this::buildSummaryModel)
                .toList();

        CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listTiers())
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTierController.class).createTier(null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get membership fee tier details")
    ResponseEntity<EntityModel<MembershipFeeTierResponse>> getTier(
            @Parameter(description = "Tier UUID") @PathVariable UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        MembershipFeeTierResponse response = MembershipFeeTierResponse.from(tier);
        return ResponseEntity.ok(entityModelWithDomain(response, tier));
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Edit a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> editTier(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Valid @RequestBody EditMembershipFeeTierRequest request) {
        MembershipFeeTierManagementPort.EditTierCommand command = request.toCommand();
        managementPort.editTier(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/rules", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Add a payment rule to a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addRule(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Valid @RequestBody AddPaymentRuleRequest request) {
        MembershipFeeTierManagementPort.AddRuleCommand command = new MembershipFeeTierManagementPort.AddRuleCommand(
                request.toDomain());
        managementPort.addRule(new MembershipFeeTierId(id), command);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Delete a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> deleteTier(
            @Parameter(description = "Tier UUID") @PathVariable UUID id) {
        managementPort.deleteTier(new MembershipFeeTierId(id));
        return ResponseEntity.noContent().build();
    }

    private EntityModel<MembershipFeeTierSummaryResponse> buildSummaryModel(MembershipFeeTier tier) {
        UUID tierId = tier.getId().value();
        MembershipFeeTierSummaryResponse summary = MembershipFeeTierSummaryResponse.from(tier);
        EntityModel<MembershipFeeTierSummaryResponse> model = EntityModel.of(summary);
        klabisLinkTo(methodOn(MembershipFeeTierController.class).getTier(tierId))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }
}

@MvcComponent
class MembershipFeeTierDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTierResponse, MembershipFeeTier> {

    private final RankingOptionsPort rankingOptionsPort;

    MembershipFeeTierDetailsPostprocessor(RankingOptionsPort rankingOptionsPort) {
        this.rankingOptionsPort = rankingOptionsPort;
    }

    @Override
    public void process(EntityModel<MembershipFeeTierResponse> dtoModel, MembershipFeeTier tier) {
        UUID id = tier.getId().value();
        List<HalFormsInlineOption> rankingOptions = rankingOptionsPort.listRankingOptions();
        klabisLinkTo(methodOn(MembershipFeeTierController.class).getTier(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTierController.class).editTier(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTierController.class).deleteTier(id)))
                        .andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(MembershipFeeTierController.class).addRule(id, null),
                                Map.of("rankingShortName", rankingOptions))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listTiers())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

@MvcComponent
class MembershipFeeTiersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listTiers())
                .ifPresent(link -> model.add(link.withRel("membership-fee-tiers")));
        return model;
    }
}
