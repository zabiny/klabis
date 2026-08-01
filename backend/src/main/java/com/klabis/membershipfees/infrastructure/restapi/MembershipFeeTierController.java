package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.EventTypeOptionsPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.RankingOptionsPort;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RankingOptionsPort rankingOptionsPort;
    private final EventTypeOptionsPort eventTypeOptionsPort;
    private final FeeSelectionCampaignManagementPort campaignManagementPort;

    MembershipFeeTierController(MembershipFeeTierManagementPort managementPort,
                                RankingOptionsPort rankingOptionsPort,
                                EventTypeOptionsPort eventTypeOptionsPort,
                                FeeSelectionCampaignManagementPort campaignManagementPort) {
        this.managementPort = managementPort;
        this.rankingOptionsPort = rankingOptionsPort;
        this.eventTypeOptionsPort = eventTypeOptionsPort;
        this.campaignManagementPort = campaignManagementPort;
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
    public ResponseEntity<java.util.Collection<MembershipFeeTierSummaryResponse>> listTiers() {
        List<MembershipFeeTier> tiers = managementPort.listTiers();
        List<MembershipFeeTierSummaryResponse> items = tiers.stream()
                .map(MembershipFeeTierSummaryResponse::from)
                .toList();

        // isAdmin() gate mirrors the original controller's behaviour: the extra links are only
        // computed (and campaignManagementPort queried) for callers with MEMBERS:MANAGE.
        Optional<FeeSelectionCampaign> activeCampaign = isAdmin()
                ? campaignManagementPort.findActiveCampaign()
                : Optional.empty();
        MembershipFeeTierListPostprocessor.setActiveCampaign(activeCampaign);

        HalResponseContext.setDomainList(tiers);
        return ResponseEntity.ok(items);
    }

    private boolean isAdmin() {
        return SecuritySpelEvaluator.hasAuthority(
                SecurityContextHolder.getContext().getAuthentication(),
                Authority.MEMBERS_MANAGE);
    }

    @Override
    public ResponseEntity<MembershipFeeTierResponse> getTier(
            UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        HalResponseContext.setDomain(tier);
        return ResponseEntity.ok(MembershipFeeTierResponse.from(tier));
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
    public ResponseEntity<java.util.Collection<MembershipFeeTierResponse.PaymentRuleResponse>> listRules(
            UUID id) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        List<PaymentRuleDomain> domains = tier.getRules().stream()
                .map(rule -> new PaymentRuleDomain(new MembershipFeeTierId(id), rule))
                .toList();
        List<MembershipFeeTierResponse.PaymentRuleResponse> items = domains.stream()
                .map(d -> MembershipFeeTierResponse.PaymentRuleResponse.from(d.rule()))
                .toList();

        MembershipFeeTierListRulesPostprocessor.setOptions(
                rankingOptionsPort.listRankingOptions(), eventTypeOptionsPort.listEventTypeOptions());
        HalResponseContext.setDomainList(domains);
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<MembershipFeeTierResponse.PaymentRuleResponse> getRule(
            UUID id,
            UUID eventTypeId,
            String ranking) {
        MembershipFeeTier tier = managementPort.getTier(new MembershipFeeTierId(id));
        MembershipPaymentRule rule = tier.getRules().stream()
                .filter(r -> r.eventTypeId().value().equals(eventTypeId) && r.rankingShortName().equals(ranking))
                .findFirst()
                .orElseThrow(() -> new PaymentRuleNotFoundException(EventTypeReference.of(eventTypeId), ranking));
        HalResponseContext.setDomain(new PaymentRuleDomain(new MembershipFeeTierId(id), rule));
        return ResponseEntity.ok(MembershipFeeTierResponse.PaymentRuleResponse.from(rule));
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

// The active campaign is looked up by the controller (which already holds
// FeeSelectionCampaignManagementPort) and handed over via a request attribute, rather than
// injecting the port here — @MvcComponent beans are discovered by @WebMvcTest's global component
// scan regardless of the controllers under test (see backend-patterns skill), so adding a new
// port dependency here would break every unrelated @WebMvcTest slice in the app unless each one
// also mocked it.
@MvcComponent
class MembershipFeeTierListPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>>> {

    private static final String ACTIVE_CAMPAIGN_ATTR = MembershipFeeTierListPostprocessor.class.getName() + ".activeCampaign";

    static void setActiveCampaign(Optional<FeeSelectionCampaign> activeCampaign) {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(ACTIVE_CAMPAIGN_ATTR, activeCampaign,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierSummaryResponse>> model) {
        // The activeCampaign attribute is what carries listTiers()'s campaign state here; the model
        // itself cannot.
        //
        // The early return is defensive, not a live path: listTiers() is the only producer of this
        // collection type and always sets the attribute, and this is NOT a type-dispatch guard —
        // RepresentationModelProcessorInvoker resolves the full generic signature, so this processor
        // never sees another endpoint's CollectionModel<EntityModel<Y>>. It earns its keep only if a
        // second producer of this type appears, or the processor runs outside a request.
        Optional<Optional<FeeSelectionCampaign>> activeCampaignAttr = currentActiveCampaignAttr();
        if (activeCampaignAttr.isEmpty()) {
            return model;
        }

        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(FeeSelectionCampaignsApi.class).publishYear(null)))
                .andAffordances(klabisAfford(methodOn(MembershipFeeTiersApi.class).createTier(null))));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (SecuritySpelEvaluator.hasAuthority(auth, Authority.MEMBERS_MANAGE)) {
            activeCampaignAttr.get().ifPresent(campaign ->
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

    @SuppressWarnings("unchecked")
    private static Optional<Optional<FeeSelectionCampaign>> currentActiveCampaignAttr() {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object value = attrs.getAttribute(ACTIVE_CAMPAIGN_ATTR,
                org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        return value instanceof Optional<?> opt ? Optional.of((Optional<FeeSelectionCampaign>) opt) : Optional.empty();
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
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).getRule(tierId, eventTypeId, ranking))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(MembershipFeeTiersApi.class).editRule(tierId,
                                        eventTypeId,
                                        ranking,
                                        null),
                                Map.of("ruleType", RULE_TYPE_OPTIONS)))
                        .andAffordances(klabisAfford(
                                methodOn(MembershipFeeTiersApi.class).removeRule(tierId, eventTypeId, ranking))))
                .ifPresent(dtoModel::add);
        dtoModel.add(Link.of("/api/event-types/" + eventTypeId, "eventType"));
    }
}

// The tier id is read off the current request's resolved @PathVariable map, since
// CollectionModel<EntityModel<PaymentRuleResponse>> carries no reference back to the tier when the
// list is empty. Same pattern as EventRegistrationController.RegistrationListPostprocessor.
// The ranking/event-type inline options are computed by the controller (which already holds
// RankingOptionsPort/EventTypeOptionsPort) and handed over via a request attribute — see the
// comment on MembershipFeeTierListPostprocessor for why these ports are not injected here.
@MvcComponent
class MembershipFeeTierListRulesPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>>> {

    private static final String OPTIONS_ATTR = MembershipFeeTierListRulesPostprocessor.class.getName() + ".options";

    record RuleOptions(List<HalFormsInlineOption> rankingOptions, List<HalFormsInlineOption> eventTypeOptions) {
    }

    static void setOptions(List<HalFormsInlineOption> rankingOptions, List<HalFormsInlineOption> eventTypeOptions) {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(OPTIONS_ATTR, new RuleOptions(rankingOptions, eventTypeOptions),
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTierResponse.PaymentRuleResponse>> model) {
        Optional<UUID> tierId = currentTierId();
        Optional<RuleOptions> options = currentOptions();
        if (tierId.isPresent() && options.isPresent()) {
            model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, link -> (Link) link
                    .andAffordances(klabisAffordWithMixedOptions(
                            methodOn(MembershipFeeTiersApi.class).addRule(tierId.get(), null),
                            Map.of("ruleType", List.of("PERCENTAGE", "FIXED_AMOUNT")),
                            Map.of("rankingShortName", options.get().rankingOptions(),
                                    "eventTypeId", options.get().eventTypeOptions()))));
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

    private static Optional<RuleOptions> currentOptions() {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object value = attrs.getAttribute(OPTIONS_ATTR,
                org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        return value instanceof RuleOptions ro ? Optional.of(ro) : Optional.empty();
    }
}

@MvcComponent
class MembershipFeesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!SecuritySpelEvaluator.hasAuthority(auth, Authority.MEMBERS_MANAGE)) {
            return model;
        }
        klabisLinkTo(methodOn(MembershipFeeTiersApi.class).listTiers())
                .ifPresent(link -> model.add(link.withRel("membership-fees")));
        return model;
    }
}
