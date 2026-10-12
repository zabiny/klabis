package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierOptionsPort;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(MembershipFeeTier.class)
class MembershipFeeTierController implements MembershipFeeTiersApi {

    private final MembershipFeeTierManagementPort managementPort;
    private final ConversionService conversionService;
    // toSummaryResponse is a plain interface method (not the Converter.convert override), so it
    // isn't reachable via ConversionService.convert(source, TargetClass) — that only dispatches
    // to the registered Converter<S,T> pair. Inject the generated Converter bean directly for it.
    private final MembershipFeeTierResponseConverter tierResponseConverter;

    MembershipFeeTierController(MembershipFeeTierManagementPort managementPort,
                                ConversionService conversionService,
                                MembershipFeeTierResponseConverter tierResponseConverter) {
        this.managementPort = managementPort;
        this.conversionService = conversionService;
        this.tierResponseConverter = tierResponseConverter;
    }

    @Override
    public ResponseEntity<Void> createTier(CreateMembershipFeeTierRequest request) {
        MembershipFeeTierManagementPort.CreateTierCommand command = MembershipFeesRequestMapper.toCommand(request);
        MembershipFeeTierId id = managementPort.createTier(command);
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTiersApi.class).getTier(id.value())).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<List<MembershipFeeTierSummaryResponse>> listTiers() {
        List<MembershipFeeTier> tiers = managementPort.listTiers();
        List<MembershipFeeTierSummaryResponse> items = tiers.stream()
                .map(tierResponseConverter::toSummaryResponse)
                .toList();

        HalResponseContext.setDomainList(tiers);
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<MembershipFeeTierResponse> getTier(
            UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        HalResponseContext.setDomain(tier);
        return ResponseEntity.ok(conversionService.convert(tier, MembershipFeeTierResponse.class));
    }

    @Override
    public ResponseEntity<Void> editTier(
            UUID id,
            EditMembershipFeeTierRequest request) {
        MembershipFeeTierManagementPort.EditTierCommand command = MembershipFeesRequestMapper.toCommand(request);
        managementPort.editTier(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<PaymentRuleResponse>> listRules(
            UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        List<PaymentRuleDomain> domains = tier.getRules().stream()
                .map(rule -> new PaymentRuleDomain(new MembershipFeeTierId(id), rule))
                .toList();
        List<PaymentRuleResponse> items = domains.stream()
                .map(d -> MembershipFeesResponseMapper.toResponse(d.rule()))
                .toList();

        HalResponseContext.setDomainList(domains);
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<PaymentRuleResponse> getRule(
            UUID id,
            UUID eventTypeId,
            String ranking) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        MembershipPaymentRule rule = tier.getRules().stream()
                .filter(r -> r.eventTypeId().value().equals(eventTypeId) && r.rankingShortName().equals(ranking))
                .findFirst()
                .orElseThrow(() -> new PaymentRuleNotFoundException(EventTypeReference.of(eventTypeId), ranking));
        HalResponseContext.setDomain(new PaymentRuleDomain(new MembershipFeeTierId(id), rule));
        return ResponseEntity.ok(MembershipFeesResponseMapper.toResponse(rule));
    }

    record PaymentRuleDomain(MembershipFeeTierId tierId, MembershipPaymentRule rule) {
    }

    @Override
    public ResponseEntity<Void> addRule(
            UUID id,
            AddPaymentRuleRequest request) {
        MembershipFeeTierManagementPort.AddRuleCommand command = new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipFeesRequestMapper.toDomain(request));
        managementPort.addRule(new MembershipFeeTierId(id), command);
        MembershipPaymentRule rule = command.rule();
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTiersApi.class)
                        .getRule(id, rule.eventTypeId().value(), rule.rankingShortName())).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<Void> editRule(
            UUID id,
            UUID eventTypeId,
            String ranking,
            EditPaymentRuleRequest request) {
        MembershipFeeTierManagementPort.EditRuleCommand command = new MembershipFeeTierManagementPort.EditRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking,
                MembershipFeesRequestMapper.toRuleValue(request, eventTypeId, ranking)
        );
        managementPort.editRule(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeRule(
            UUID id,
            UUID eventTypeId,
            String ranking) {
        MembershipFeeTierManagementPort.RemoveRuleCommand command = new MembershipFeeTierManagementPort.RemoveRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking
        );
        managementPort.removeRule(new MembershipFeeTierId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTier(
            UUID id) {
        managementPort.deleteTier(new MembershipFeeTierId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class MembershipFeeTierSummaryPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTierSummaryResponse, MembershipFeeTier> {

    @Override
    public void process(EntityModel<MembershipFeeTierSummaryResponse> dtoModel, MembershipFeeTier tier) {
        UUID id = tier.getId().value();
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getTier(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).editTier(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).deleteTier(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).listRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
    }
}

@MvcComponent
class MembershipFeeTierDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTierResponse, MembershipFeeTier> {

    @Override
    public void process(EntityModel<MembershipFeeTierResponse> dtoModel, MembershipFeeTier tier) {
        UUID id = tier.getId().value();
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getTier(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).editTier(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).deleteTier(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).listTiers())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).listRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
    }
}

@MvcComponent
class MembershipFeeTierListPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>>> {

    private final AuthorizationEvaluator authorizationEvaluator;
    private final FeeSelectionCampaignManagementPort campaignManagementPort;

    MembershipFeeTierListPostprocessor(AuthorizationEvaluator authorizationEvaluator,
                                       FeeSelectionCampaignManagementPort campaignManagementPort) {
        this.authorizationEvaluator = authorizationEvaluator;
        this.campaignManagementPort = campaignManagementPort;
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>> model) {
        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(FeeSelectionCampaignsApi.class).publishYear(null)))
                .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).createTier(null))));

        // getPublication/listPublications are readable by any authenticated caller; the campaign
        // links are management navigation and are offered to managers only, so the campaign lookup
        // is skipped for everyone else.
        if (authorizationEvaluator.has(Authority.MEMBERS_MANAGE)) {
            campaignManagementPort.findActiveCampaign().ifPresent(campaign ->
                    klabisLinkTo(methodOn(FeeSelectionCampaignsApi.class).getPublication(campaign.getId()
                            .value()))
                            .map(link -> link.withRel("activeCampaign"))
                            .ifPresent(model::add)
            );
            klabisLinkTo(methodOn(FeeSelectionCampaignsApi.class).listPublications("closed"))
                    .map(link -> link.withRel("pastCampaigns"))
                    .ifPresent(model::add);
        }
        return model;
    }

}

@MvcComponent
class PaymentRuleDetailsPostprocessor
        extends ModelWithDomainPostprocessor<PaymentRuleResponse, MembershipFeeTierController.PaymentRuleDomain> {

    private static final List<String> RULE_TYPE_OPTIONS = List.of("PERCENTAGE", "FIXED_AMOUNT");

    @Override
    public void process(EntityModel<PaymentRuleResponse> dtoModel,
                        MembershipFeeTierController.PaymentRuleDomain domain) {
        UUID tierId = domain.tierId().value();
        UUID eventTypeId = domain.rule().eventTypeId().value();
        String ranking = domain.rule().rankingShortName();
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getRule(tierId, eventTypeId, ranking))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(MembershipFeeTiersApi.class).editRule(tierId,
                                        eventTypeId,
                                        ranking,
                                        null),
                                Map.of("ruleType", HalFormsOptionsDef.values(RULE_TYPE_OPTIONS))))
                        .andAffordances(klabisAfford(
                                methodOn(MembershipFeeTiersApi.class).removeRule(tierId, eventTypeId, ranking))))
                .ifPresent(dtoModel::add);
        dtoModel.add(Link.of("/api/event-types/" + eventTypeId, "eventType"));
    }
}

// The tier id is read off the current request's resolved @PathVariable map, since
// CollectionModel<EntityModel<PaymentRuleResponse>> carries no reference back to the tier when the
// list is empty. Same pattern as EventRegistrationController.RegistrationListPostprocessor.
@MvcComponent
class MembershipFeeTierListRulesPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>>> {

    private final MembershipFeeTierOptionsPort optionsPort;

    MembershipFeeTierListRulesPostprocessor(MembershipFeeTierOptionsPort optionsPort) {
        this.optionsPort = optionsPort;
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>> model) {
        Optional<UUID> tierId = currentTierId();
        if (tierId.isPresent()) {
            model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, link -> (Link) link
                    .andAffordances(klabisAffordWithOptions(
                            methodOn(MembershipFeeTiersApi.class).addRule(tierId.get(), null),
                            Map.of("ruleType", HalFormsOptionsDef.values(List.of("PERCENTAGE", "FIXED_AMOUNT")),
                                    "rankingShortName", new HalFormsOptionsDef.Inline(optionsPort.listRankingOptions()),
                                    "eventTypeId", new HalFormsOptionsDef.Inline(optionsPort.listEventTypeOptions())))));
        }
        return model;
    }

    private static Optional<UUID> currentTierId() {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object variables = attrs.getAttribute(
                org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        if (!(variables instanceof Map<?, ?> pathVariables)) {
            return Optional.empty();
        }
        Object id = pathVariables.get("id");
        return id != null ? Optional.of(UUID.fromString(id.toString())) : Optional.empty();
    }
}

@MvcComponent
class MembershipFeesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    private final AuthorizationEvaluator authorizationEvaluator;

    MembershipFeesRootPostprocessor(AuthorizationEvaluator authorizationEvaluator) {
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        // listTiers is readable by any authenticated caller, but the navigation entry is an
        // administration screen, so it is offered to managers only.
        if (!authorizationEvaluator.has(Authority.MEMBERS_MANAGE)) {
            return model;
        }
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).listTiers())
                .ifPresent(link -> model.add(link.withRel("membership-fees")));
        return model;
    }
}
