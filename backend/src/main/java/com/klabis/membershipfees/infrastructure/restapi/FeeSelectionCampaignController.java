package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/fee-selection-campaigns", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "FeeSelectionCampaigns", description = "Publishing fee levels for a calendar year")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(FeeSelectionCampaign.class)
class FeeSelectionCampaignController {

    private final FeeSelectionCampaignManagementPort managementPort;
    private final MembershipFeeTierManagementPort levelManagementPort;

    FeeSelectionCampaignController(FeeSelectionCampaignManagementPort managementPort,
                                   MembershipFeeTierManagementPort levelManagementPort) {
        this.managementPort = managementPort;
        this.levelManagementPort = levelManagementPort;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Publish fee levels for a calendar year (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> publishYear(@Valid @RequestBody PublishYearRequest request) {
        FeeSelectionCampaignId id = managementPort.publishYear(request.toCommand());
        return ResponseEntity.created(
                linkTo(methodOn(FeeSelectionCampaignController.class).getPublication(id.value())).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List fee year publications, optionally filtered by status")
    ResponseEntity<CollectionModel<EntityModel<FeeSelectionCampaignResponse>>> listPublications(
            @Parameter(description = "Filter by status: 'closed' returns only past campaigns") @RequestParam(required = false) String status) {
        List<FeeSelectionCampaign> publications = "closed".equals(status)
                ? managementPort.listClosedPublications()
                : managementPort.listPublications();
        List<EntityModel<FeeSelectionCampaignResponse>> items = publications.stream()
                .map(this::buildSummaryModel)
                .toList();

        List<HalFormsInlineOption> levelOptions = levelManagementPort.listTiers().stream()
                .map(level -> new HalFormsInlineOption(level.getId().value().toString(), level.getName()))
                .toList();

        CollectionModel<EntityModel<FeeSelectionCampaignResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).listPublications(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(FeeSelectionCampaignController.class).publishYear(null),
                                Map.of("levelIds", levelOptions)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee year publication details")
    ResponseEntity<EntityModel<FeeSelectionCampaignResponse>> getPublication(
            @Parameter(description = "Publication UUID") @PathVariable UUID id) {
        FeeSelectionCampaign publication = managementPort.getPublication(new FeeSelectionCampaignId(id));
        FeeSelectionCampaignResponse response = FeeSelectionCampaignResponse.from(publication);
        return ResponseEntity.ok(entityModelWithDomain(response, publication));
    }

    @PatchMapping(value = "/{id}/deadline", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Change voting deadline of an active campaign (requires MEMBERS:MANAGE)")
    ResponseEntity<EntityModel<FeeSelectionCampaignResponse>> changeDeadline(
            @PathVariable UUID id, @Valid @RequestBody ChangeDeadlineRequest request) {
        FeeSelectionCampaign updated = managementPort.changeDeadline(new FeeSelectionCampaignId(id), request.toCommand());
        FeeSelectionCampaignResponse response = FeeSelectionCampaignResponse.from(updated);
        return ResponseEntity.ok(entityModelWithDomain(response, updated));
    }

    @GetMapping("/{year}/levels")
    @Operation(summary = "List published fee groups for a given year")
    ResponseEntity<CollectionModel<EntityModel<MembershipFeeGroupResponse>>> listGroupsForYear(
            @Parameter(description = "Calendar year") @PathVariable int year) {
        List<MembershipFeeGroup> groups = managementPort.listGroupsForYear(year);
        List<EntityModel<MembershipFeeGroupResponse>> items = groups.stream()
                .map(group -> {
                    MembershipFeeGroupResponse response = MembershipFeeGroupResponse.from(group);
                    EntityModel<MembershipFeeGroupResponse> model = EntityModel.of(response);
                    klabisLinkTo(methodOn(MembershipFeeGroupController.class).getGroup(group.getId().value()))
                            .ifPresent(link -> model.add(link.withSelfRel()));
                    return model;
                })
                .toList();

        CollectionModel<EntityModel<MembershipFeeGroupResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).listGroupsForYear(year))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return ResponseEntity.ok(model);
    }

    private EntityModel<FeeSelectionCampaignResponse> buildSummaryModel(FeeSelectionCampaign publication) {
        UUID publicationId = publication.getId().value();
        FeeSelectionCampaignResponse response = FeeSelectionCampaignResponse.from(publication);
        EntityModel<FeeSelectionCampaignResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).getPublication(publicationId))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
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
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).getPublication(id))
                .map(link -> {
                    var self = link.withSelfRel();
                    if (!publication.isClosed(today)) {
                        self = self.andAffordances(klabisAfford(
                                methodOn(FeeSelectionCampaignController.class).changeDeadline(id, null)));
                    }
                    return self;
                })
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).listPublications(null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(FeeSelectionCampaignController.class).listGroupsForYear(publication.getYear()))
                .ifPresent(link -> dtoModel.add(link.withRel("levels")));
    }
}

