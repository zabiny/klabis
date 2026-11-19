package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.membershipfees.MembershipFeeTypeId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTypeOptionsPort;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.Affordance;
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
@ExposesResourceFor(MembershipFeeType.class)
class MembershipFeeTypeController implements MembershipFeeTypesApi {

    private final MembershipFeeTypeManagementPort managementPort;
    private final ConversionService conversionService;
    // toSummaryResponse is a plain interface method (not the Converter.convert override), so it
    // isn't reachable via ConversionService.convert(source, TargetClass) — that only dispatches
    // to the registered Converter<S,T> pair. Inject the generated Converter bean directly for it.
    private final MembershipFeeTypeResponseConverter typeResponseConverter;

    MembershipFeeTypeController(MembershipFeeTypeManagementPort managementPort,
                                ConversionService conversionService,
                                MembershipFeeTypeResponseConverter typeResponseConverter) {
        this.managementPort = managementPort;
        this.conversionService = conversionService;
        this.typeResponseConverter = typeResponseConverter;
    }

    @Override
    public ResponseEntity<Void> createFeeType(CreateMembershipFeeTypeRequest request) {
        MembershipFeeTypeManagementPort.CreateTypeCommand command = MembershipFeesRequestMapper.toCommand(request);
        MembershipFeeTypeId id = managementPort.createFeeType(command);
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTypesApi.class).getFeeType(id.value())).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<List<MembershipFeeTypeSummaryResponse>> listFeeTypes() {
        List<MembershipFeeType> types = managementPort.listFeeTypes();
        List<MembershipFeeTypeSummaryResponse> items = types.stream()
                .map(typeResponseConverter::toSummaryResponse)
                .toList();

        HalResponseContext.setDomainList(types);
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<MembershipFeeTypeResponse> getFeeType(
            UUID id) {
        MembershipFeeType type = managementPort.getFeeType(new MembershipFeeTypeId(id));
        HalResponseContext.setDomain(type);
        return ResponseEntity.ok(conversionService.convert(type, MembershipFeeTypeResponse.class));
    }

    @Override
    public ResponseEntity<Void> editFeeType(
            UUID id,
            EditMembershipFeeTypeRequest request) {
        MembershipFeeTypeManagementPort.EditTypeCommand command = MembershipFeesRequestMapper.toCommand(request);
        managementPort.editFeeType(new MembershipFeeTypeId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<PaymentRuleResponse>> listRules(
            UUID id) {
        MembershipFeeType type = managementPort.getFeeType(new MembershipFeeTypeId(id));
        List<PaymentRuleDomain> domains = type.getRules().stream()
                .map(rule -> new PaymentRuleDomain(new MembershipFeeTypeId(id), rule))
                .toList();
        List<PaymentRuleResponse> items = domains.stream()
                .map(d -> MembershipFeesResponseMapper.toResponse(d.rule()))
                .toList();

        HalResponseContext.setDomainList(domains);
        HalResponseContext.setContext(new TypeRulesListing(type.getId()));
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<PaymentRuleResponse> getRule(
            UUID id,
            UUID eventTypeId,
            String ranking) {
        MembershipFeeType type = managementPort.getFeeType(new MembershipFeeTypeId(id));
        MembershipPaymentRule rule = type.getRules().stream()
                .filter(r -> r.eventTypeId().value().equals(eventTypeId) && r.rankingShortName().equals(ranking))
                .findFirst()
                .orElseThrow(() -> new PaymentRuleNotFoundException(EventTypeReference.of(eventTypeId), ranking));
        HalResponseContext.setDomain(new PaymentRuleDomain(new MembershipFeeTypeId(id), rule));
        return ResponseEntity.ok(MembershipFeesResponseMapper.toResponse(rule));
    }

    record PaymentRuleDomain(MembershipFeeTypeId typeId, MembershipPaymentRule rule) {
    }

    record TypeRulesListing(MembershipFeeTypeId typeId) {
    }

    @Override
    public ResponseEntity<Void> addRule(
            UUID id,
            AddPaymentRuleRequest request) {
        MembershipFeeTypeManagementPort.AddRuleCommand command = new MembershipFeeTypeManagementPort.AddRuleCommand(
                MembershipFeesRequestMapper.toDomain(request));
        managementPort.addRule(new MembershipFeeTypeId(id), command);
        MembershipPaymentRule rule = command.rule();
        return ResponseEntity.created(
                linkTo(methodOn(MembershipFeeTypesApi.class)
                        .getRule(id, rule.eventTypeId().value(), rule.rankingShortName())).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<Void> editRule(
            UUID id,
            UUID eventTypeId,
            String ranking,
            EditPaymentRuleRequest request) {
        MembershipFeeTypeManagementPort.EditRuleCommand command = new MembershipFeeTypeManagementPort.EditRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking,
                MembershipFeesRequestMapper.toRuleValue(request, eventTypeId, ranking)
        );
        managementPort.editRule(new MembershipFeeTypeId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeRule(
            UUID id,
            UUID eventTypeId,
            String ranking) {
        MembershipFeeTypeManagementPort.RemoveRuleCommand command = new MembershipFeeTypeManagementPort.RemoveRuleCommand(
                EventTypeReference.of(eventTypeId),
                ranking
        );
        managementPort.removeRule(new MembershipFeeTypeId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteFeeType(
            UUID id) {
        managementPort.deleteFeeType(new MembershipFeeTypeId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class MembershipFeeTypeSummaryPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTypeSummaryResponse, MembershipFeeType> {

    @Override
    public void process(EntityModel<MembershipFeeTypeSummaryResponse> dtoModel, MembershipFeeType type) {
        UUID id = type.getId().value();
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).getFeeType(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTypesApi.class).editFeeType(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTypesApi.class).deleteFeeType(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).listRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
    }
}

@MvcComponent
class MembershipFeeTypeDetailsPostprocessor
        extends ModelWithDomainPostprocessor<MembershipFeeTypeResponse, MembershipFeeType> {

    @Override
    public void process(EntityModel<MembershipFeeTypeResponse> dtoModel, MembershipFeeType type) {
        UUID id = type.getId().value();
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).getFeeType(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTypesApi.class).editFeeType(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembershipFeeTypesApi.class).deleteFeeType(id))))
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).listFeeTypes())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).listRules(id))
                .ifPresent(link -> dtoModel.add(link.withRel("rules")));
    }
}

@MvcComponent
class MembershipFeeTypeListPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTypeSummaryResponse>>> {

    private final AuthorizationEvaluator authorizationEvaluator;
    private final FeeSelectionCampaignManagementPort campaignManagementPort;
    private final PublishYearAffordance publishYearAffordance;

    MembershipFeeTypeListPostprocessor(AuthorizationEvaluator authorizationEvaluator,
                                       FeeSelectionCampaignManagementPort campaignManagementPort,
                                       PublishYearAffordance publishYearAffordance) {
        this.authorizationEvaluator = authorizationEvaluator;
        this.campaignManagementPort = campaignManagementPort;
        this.publishYearAffordance = publishYearAffordance;
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTypeSummaryResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<MembershipFeeTypeSummaryResponse>> model) {
        boolean manager = authorizationEvaluator.has(Authority.MEMBERS_MANAGE);
        // getPublication/listPublications are readable by any authenticated caller; the campaign
        // lookup and the publishYear template are management concerns, so both are skipped for
        // everyone else.
        Optional<FeeSelectionCampaign> activeCampaign = manager
                ? campaignManagementPort.findActiveCampaign()
                : Optional.empty();
        List<Affordance> publishYear = manager
                ? publishYearAffordance.affordances(activeCampaign.isPresent())
                : List.of();

        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(publishYear)
                .andAffordances(klabisAfford(methodOn(MembershipFeeTypesApi.class).createFeeType(null))));

        if (manager) {
            activeCampaign.ifPresent(campaign ->
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
        extends ModelWithDomainPostprocessor<PaymentRuleResponse, Object> {

    private static final List<String> RULE_TYPE_OPTIONS = List.of("PERCENTAGE", "FIXED_AMOUNT");

    @Override
    public void process(EntityModel<PaymentRuleResponse> dtoModel, Object source) {
        if (!(source instanceof MembershipFeeTypeController.PaymentRuleDomain domain)) {
            return;
        }
        UUID typeId = domain.typeId().value();
        UUID eventTypeId = domain.rule().eventTypeId().value();
        String ranking = domain.rule().rankingShortName();
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).getRule(typeId, eventTypeId, ranking))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(MembershipFeeTypesApi.class).editRule(typeId,
                                        eventTypeId,
                                        ranking,
                                        null),
                                Map.of("ruleType", HalFormsOptionsDef.values(RULE_TYPE_OPTIONS))))
                        .andAffordances(klabisAfford(
                                methodOn(MembershipFeeTypesApi.class).removeRule(typeId, eventTypeId, ranking))))
                .ifPresent(dtoModel::add);
        dtoModel.add(Link.of("/api/event-types/" + eventTypeId, "eventType"));
    }
}

// The type id comes from HalResponseContext, since CollectionModel<EntityModel<PaymentRuleResponse>>
// carries no reference back to the type when the list is empty. PaymentRuleResponse is shared with
// the fee group rules snapshot, which must not get type rule management affordances, so the
// context entry published by the type endpoint is what scopes this postprocessor.
@MvcComponent
class MembershipFeeTypeListRulesPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>>> {

    private final MembershipFeeTypeOptionsPort optionsPort;

    MembershipFeeTypeListRulesPostprocessor(MembershipFeeTypeOptionsPort optionsPort) {
        this.optionsPort = optionsPort;
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<PaymentRuleResponse>> model) {
        Optional<UUID> typeId = currentTypeId();
        if (typeId.isPresent()) {
            model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, link -> (Link) link
                    .andAffordances(klabisAffordWithOptions(
                            methodOn(MembershipFeeTypesApi.class).addRule(typeId.get(), null),
                            Map.of("ruleType", HalFormsOptionsDef.values(List.of("PERCENTAGE", "FIXED_AMOUNT")),
                                    "rankingShortName", new HalFormsOptionsDef.Inline(optionsPort.listRankingOptions()),
                                    "eventTypeId", new HalFormsOptionsDef.Inline(optionsPort.listEventTypeOptions())))));
        }
        return model;
    }

    private static Optional<UUID> currentTypeId() {
        return HalResponseContext.findContext(MembershipFeeTypeController.TypeRulesListing.class)
                .map(listing -> listing.typeId().value());
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
        // listFeeTypes is readable by any authenticated caller, but the navigation entry is an
        // administration screen, so it is offered to managers only.
        if (!authorizationEvaluator.has(Authority.MEMBERS_MANAGE)) {
            return model;
        }
        klabisLinkTo(methodOn(MembershipFeeTypesApi.class).listFeeTypes())
                .ifPresent(link -> model.add(link.withRel("membership-fees")));
        return model;
    }
}
