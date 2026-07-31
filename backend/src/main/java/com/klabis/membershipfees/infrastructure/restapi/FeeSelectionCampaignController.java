package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.application.CampaignStatusFilter;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.ManualCampaignClosePort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(FeeSelectionCampaign.class)
class FeeSelectionCampaignController implements FeeSelectionCampaignsApi {

    private final FeeSelectionCampaignManagementPort managementPort;
    private final MembershipFeeTierManagementPort levelManagementPort;
    private final ManualCampaignClosePort manualCampaignClosePort;

    FeeSelectionCampaignController(FeeSelectionCampaignManagementPort managementPort,
                                   MembershipFeeTierManagementPort levelManagementPort,
                                   ManualCampaignClosePort manualCampaignClosePort) {
        this.managementPort = managementPort;
        this.levelManagementPort = levelManagementPort;
        this.manualCampaignClosePort = manualCampaignClosePort;
    }

    @Override
    public ResponseEntity<Void> publishYear(PublishYearRequest request) {
        FeeSelectionCampaignId id = managementPort.publishYear(MembershipFeesRequestMapper.toCommand(request));
        return ResponseEntity.created(
                linkTo(methodOn(FeeSelectionCampaignsApi.class).getPublication(id.value())).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<java.util.Collection<FeeSelectionCampaignResponse>> listPublications(
            String status) {
        CampaignStatusFilter filter = status != null ? CampaignStatusFilter.valueOf(status.toUpperCase()) : CampaignStatusFilter.ALL;
        List<FeeSelectionCampaign> publications = managementPort.listPublications(filter);
        List<FeeSelectionCampaignResponse> items = publications.stream()
                .map(FeeSelectionCampaignResponse::from)
                .toList();

        List<HalFormsInlineOption> levelOptions = levelManagementPort.listTiers().stream()
                .map(level -> new HalFormsInlineOption(level.getId().value().toString(), level.getName()))
                .toList();
        FeeSelectionCampaignListPostprocessor.setLevelOptions(levelOptions);

        HalResponseContext.setDomainList(publications);
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<FeeSelectionCampaignResponse> getPublication(
            UUID id) {
        FeeSelectionCampaign publication = managementPort.getPublication(new FeeSelectionCampaignId(id));
        HalResponseContext.setDomain(publication);
        return ResponseEntity.ok(FeeSelectionCampaignResponse.from(publication));
    }

    @Override
    public ResponseEntity<FeeSelectionCampaignResponse> changeDeadline(
            UUID id, ChangeDeadlineRequest request) {
        FeeSelectionCampaign updated = managementPort.changeDeadline(new FeeSelectionCampaignId(id),
                MembershipFeesRequestMapper.toCommand(request));
        HalResponseContext.setDomain(updated);
        return ResponseEntity.ok(FeeSelectionCampaignResponse.from(updated));
    }

    @Override
    public ResponseEntity<Void> closeCampaign(UUID id) {
        manualCampaignClosePort.closeCampaign(new FeeSelectionCampaignId(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<java.util.Collection<MembershipFeeGroupResponse>> listGroupsForYear(
            Integer year) {
        List<MembershipFeeGroup> groups = managementPort.listGroupsForYear(year);
        List<MembershipFeeGroupResponse> items = groups.stream()
                .map(MembershipFeeGroupResponse::from)
                .toList();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(items);
    }
}

@MvcComponent
class FeeSelectionCampaignDetailsPostprocessor
        extends ModelWithDomainPostprocessor<FeeSelectionCampaignResponse, FeeSelectionCampaign> {

    private final Clock clock;

    FeeSelectionCampaignDetailsPostprocessor(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void process(EntityModel<FeeSelectionCampaignResponse> dtoModel, FeeSelectionCampaign publication) {
        UUID id = publication.getId().value();
        LocalDate today = LocalDate.now(clock);
        klabisLinkTo(methodOn(FeeSelectionCampaignsApi.class).getPublication(id))
                .map(link -> {
                    var self = link.withSelfRel();
                    if (!publication.isClosed(today)) {
                        self = self.andAffordances(klabisAfford(
                                methodOn(FeeSelectionCampaignsApi.class).changeDeadline(id, null)));
                        if (!publication.isProcessed()) {
                            self = self.andAffordances(klabisAfford(
                                    methodOn(FeeSelectionCampaignsApi.class).closeCampaign(id)));
                        }
                    }
                    return self;
                })
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(FeeSelectionCampaignsApi.class).listPublications(null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(FeeSelectionCampaignsApi.class).listGroupsForYear(publication.getYear()))
                .ifPresent(link -> dtoModel.add(link.withRel("levels")));
    }
}

// The level inline options are computed by the controller (which already holds
// MembershipFeeTierManagementPort) and handed over via a request attribute — @MvcComponent beans
// are discovered by @WebMvcTest's global component scan regardless of the controllers under test
// (see backend-patterns skill), so injecting the port here would break every unrelated
// @WebMvcTest slice in the app unless each one also mocked it.
@MvcComponent
class FeeSelectionCampaignListPostprocessor
        implements RepresentationModelProcessor<org.springframework.hateoas.CollectionModel<EntityModel<FeeSelectionCampaignResponse>>> {

    private static final String LEVEL_OPTIONS_ATTR = FeeSelectionCampaignListPostprocessor.class.getName() + ".levelOptions";

    static void setLevelOptions(List<HalFormsInlineOption> levelOptions) {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(LEVEL_OPTIONS_ATTR, levelOptions,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Override
    public org.springframework.hateoas.CollectionModel<EntityModel<FeeSelectionCampaignResponse>> process(
            org.springframework.hateoas.CollectionModel<EntityModel<FeeSelectionCampaignResponse>> model) {
        // Spring HATEOAS's RepresentationModelProcessorInvoker matches CollectionModel processors
        // by the outer CollectionModel type only, not by the EntityModel<T> content type, so every
        // registered CollectionModel processor in the app runs against every CollectionModel
        // response. The level-options attribute is set only by listPublications(), so its absence
        // here means this fired for some other endpoint and ifPresent is a no-op — this is a
        // deliberate guard, not an optional nicety.
        currentLevelOptions().ifPresent(levelOptions ->
                model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                        .andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(FeeSelectionCampaignsApi.class).publishYear(null),
                                Map.of("levelIds", levelOptions)))));
        return model;
    }

    @SuppressWarnings("unchecked")
    private static Optional<List<HalFormsInlineOption>> currentLevelOptions() {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object value = attrs.getAttribute(LEVEL_OPTIONS_ATTR,
                org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        return value instanceof List<?> list ? Optional.of((List<HalFormsInlineOption>) list) : Optional.empty();
    }
}
