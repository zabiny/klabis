package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.EventTypeOptionsPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.RankingOptionsPort;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import com.klabis.membershipfees.domain.PaymentRuleNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final RankingOptionsPort rankingOptionsPort;
    private final EventTypeOptionsPort eventTypeOptionsPort;
    private final FeeSelectionCampaignManagementPort campaignManagementPort;
    private final Clock clock;

    MembershipFeeTierController(MembershipFeeTierManagementPort managementPort,
                                RankingOptionsPort rankingOptionsPort,
                                EventTypeOptionsPort eventTypeOptionsPort,
                                FeeSelectionCampaignManagementPort campaignManagementPort,
                                Clock clock) {
        this.managementPort = managementPort;
        this.rankingOptionsPort = rankingOptionsPort;
        this.eventTypeOptionsPort = eventTypeOptionsPort;
        this.campaignManagementPort = campaignManagementPort;
        this.clock = clock;
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

        if (isAdmin()) {
            Optional<FeeSelectionCampaign> activeCampaign = campaignManagementPort.findActiveCampaign(LocalDate.now(clock));
            activeCampaign.ifPresent(campaign ->
                    klabisLinkTo(methodOn(FeeSelectionCampaignController.class).getPublication(campaign.getId().value()))
                            .map(link -> link.withRel("activeCampaign"))
                            .ifPresent(model::add)
            );
            klabisLinkTo(methodOn(FeeSelectionCampaignController.class).listPublications("closed"))
                    .map(link -> link.withRel("pastCampaigns"))
                    .ifPresent(model::add);
        }

        return ResponseEntity.ok(model);
    }

    private boolean isAdmin() {
        return SecuritySpelEvaluator.hasAuthority(
                SecurityContextHolder.getContext().getAuthentication(),
                Authority.MEMBERS_MANAGE);
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

    @GetMapping("/{id}/rules")
    @Operation(summary = "List payment rules for a membership fee tier")
    ResponseEntity<CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>>> listRules(
            @Parameter(description = "Tier UUID") @PathVariable UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        List<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> items = tier.getRules().stream()
                .map(rule -> entityModelWithDomain(
                        MembershipFeeTierResponse.PaymentRuleResponse.from(rule),
                        new PaymentRuleDomain(new MembershipFeeTierId(id), rule)))
                .toList();
        List<HalFormsInlineOption> rankingOptions = rankingOptionsPort.listRankingOptions();
        List<HalFormsInlineOption> eventTypeOptions = eventTypeOptionsPort.listEventTypeOptions();
        CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listRules(id))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithMixedOptions(
                                methodOn(MembershipFeeTierController.class).addRule(id, null),
                                Map.of("ruleType", List.of("PERCENTAGE", "FIXED_AMOUNT")),
                                Map.of("rankingShortName", rankingOptions, "eventTypeId", eventTypeOptions)))));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}/rules/{eventTypeId}/{ranking}")
    @Operation(summary = "Get a payment rule detail from a membership fee tier")
    ResponseEntity<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> getRule(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Parameter(description = "Event type UUID") @PathVariable UUID eventTypeId,
            @Parameter(description = "Ranking short name") @PathVariable String ranking) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        MembershipPaymentRule rule = tier.getRules().stream()
                .filter(r -> r.eventTypeId().value().equals(eventTypeId) && r.rankingShortName().equals(ranking))
                .findFirst()
                .orElseThrow(() -> new PaymentRuleNotFoundException(EventTypeReference.of(eventTypeId), ranking));
        return ResponseEntity.ok(entityModelWithDomain(
                MembershipFeeTierResponse.PaymentRuleResponse.from(rule),
                new PaymentRuleDomain(new MembershipFeeTierId(id), rule)));
    }

    record PaymentRuleDomain(MembershipFeeTierId tierId, MembershipPaymentRule rule) {}

    @PostMapping(value = "/{id}/rules", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Add a payment rule to a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addRule(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Valid @RequestBody AddPaymentRuleRequest request) {
        MembershipFeeTierManagementPort.AddRuleCommand command = new MembershipFeeTierManagementPort.AddRuleCommand(
                request.toDomain());
        managementPort.addRule(new MembershipFeeTierId(id), command);
        MembershipPaymentRule rule = command.rule();
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTierController.class)
                        .getRule(id, rule.eventTypeId().value(), rule.rankingShortName())).toUri()
        ).build();
    }

    @PatchMapping(value = "/{id}/rules/{eventTypeId}/{ranking}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Edit a payment rule's value on a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> editRule(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Parameter(description = "Event type UUID") @PathVariable UUID eventTypeId,
            @Parameter(description = "Ranking short name") @PathVariable String ranking,
            @Valid @RequestBody EditPaymentRuleRequest request) {
        MembershipFeeTierManagementPort.EditRuleCommand command = new MembershipFeeTierManagementPort.EditRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking,
                request.toRuleValue(eventTypeId, ranking)
        );
        managementPort.editRule(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/rules/{eventTypeId}/{ranking}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Remove a payment rule from a membership fee tier (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> removeRule(
            @Parameter(description = "Tier UUID") @PathVariable UUID id,
            @Parameter(description = "Event type UUID") @PathVariable UUID eventTypeId,
            @Parameter(description = "Ranking short name") @PathVariable String ranking) {
        MembershipFeeTierManagementPort.RemoveRuleCommand command = new MembershipFeeTierManagementPort.RemoveRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking
        );
        managementPort.removeRule(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
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

    @Override
    public void process(EntityModel<MembershipFeeTierResponse> dtoModel, MembershipFeeTier tier) {
        UUID id = tier.getId().value();
        klabisLinkTo(methodOn(MembershipFeeTierController.class).getTier(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTierController.class).editTier(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTierController.class).deleteTier(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listTiers())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
    }
}

@MvcComponent
class PaymentRuleDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTierResponse.PaymentRuleResponse, MembershipFeeTierController.PaymentRuleDomain> {

    private static final List<String> RULE_TYPE_OPTIONS = List.of("PERCENTAGE", "FIXED_AMOUNT");

    @Override
    public void process(EntityModel<MembershipFeeTierResponse.PaymentRuleResponse> dtoModel,
                        MembershipFeeTierController.PaymentRuleDomain domain) {
        UUID tierId = domain.tierId().value();
        UUID eventTypeId = domain.rule().eventTypeId().value();
        String ranking = domain.rule().rankingShortName();
        klabisLinkTo(methodOn(MembershipFeeTierController.class).getRule(tierId, eventTypeId, ranking))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(MembershipFeeTierController.class).editRule(tierId, eventTypeId, ranking, null),
                                Map.of("ruleType", RULE_TYPE_OPTIONS)))
                        .andAffordances(klabisAfford(
                                methodOn(MembershipFeeTierController.class).removeRule(tierId, eventTypeId, ranking))))
                .ifPresent(dtoModel::add);
        dtoModel.add(Link.of("/api/event-types/" + eventTypeId, "eventType"));
    }
}

@MvcComponent
class MembershipFeesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Authority.MEMBERS_MANAGE.getValue()))) {
            return model;
        }
        klabisLinkTo(methodOn(MembershipFeeTierController.class).listTiers())
                .ifPresent(link -> model.add(link.withRel("membership-fees")));
        return model;
    }
}

