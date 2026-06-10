package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.FeeYearPublication;
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
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithPromptedOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/fee-year-publications", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "FeeYearPublications", description = "Publishing fee levels for a calendar year")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
@ExposesResourceFor(FeeYearPublication.class)
class FeeYearPublicationController {

    private final FeeYearPublicationManagementPort managementPort;
    private final MembershipFeeLevelManagementPort levelManagementPort;

    FeeYearPublicationController(FeeYearPublicationManagementPort managementPort,
                                  MembershipFeeLevelManagementPort levelManagementPort) {
        this.managementPort = managementPort;
        this.levelManagementPort = levelManagementPort;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Publish fee levels for a calendar year (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> publishYear(@Valid @RequestBody PublishYearRequest request) {
        FeeYearPublicationId id = managementPort.publishYear(request.toCommand());
        return ResponseEntity.created(
                linkTo(methodOn(FeeYearPublicationController.class).getPublication(id.value())).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List all fee year publications")
    ResponseEntity<CollectionModel<EntityModel<FeeYearPublicationResponse>>> listPublications() {
        List<FeeYearPublication> publications = managementPort.listPublications();
        List<EntityModel<FeeYearPublicationResponse>> items = publications.stream()
                .map(this::buildSummaryModel)
                .toList();

        List<HalFormsInlineOption> levelOptions = levelManagementPort.listLevels().stream()
                .map(level -> new HalFormsInlineOption(level.getId().value().toString(), level.getName()))
                .toList();

        CollectionModel<EntityModel<FeeYearPublicationResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FeeYearPublicationController.class).listPublications())
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithPromptedOptions(
                                methodOn(FeeYearPublicationController.class).publishYear(null),
                                Map.of("levelIds", levelOptions)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee year publication details")
    ResponseEntity<EntityModel<FeeYearPublicationResponse>> getPublication(
            @Parameter(description = "Publication UUID") @PathVariable UUID id) {
        FeeYearPublication publication = managementPort.getPublication(new FeeYearPublicationId(id));
        FeeYearPublicationResponse response = FeeYearPublicationResponse.from(publication);
        return ResponseEntity.ok(entityModelWithDomain(response, publication));
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
        klabisLinkTo(methodOn(FeeYearPublicationController.class).listGroupsForYear(year))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return ResponseEntity.ok(model);
    }

    private EntityModel<FeeYearPublicationResponse> buildSummaryModel(FeeYearPublication publication) {
        UUID publicationId = publication.getId().value();
        FeeYearPublicationResponse response = FeeYearPublicationResponse.from(publication);
        EntityModel<FeeYearPublicationResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(FeeYearPublicationController.class).getPublication(publicationId))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }
}

@MvcComponent
class FeeYearPublicationDetailsPostprocessor
        extends ModelWithDomainPostprocessor<FeeYearPublicationResponse, FeeYearPublication> {

    @Override
    public void process(EntityModel<FeeYearPublicationResponse> dtoModel, FeeYearPublication publication) {
        UUID id = publication.getId().value();
        klabisLinkTo(methodOn(FeeYearPublicationController.class).getPublication(id))
                .map(link -> link.withSelfRel())
                .ifPresent(dtoModel::add);
        klabisLinkTo(methodOn(FeeYearPublicationController.class).listPublications())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
        klabisLinkTo(methodOn(FeeYearPublicationController.class).listGroupsForYear(publication.getYear()))
                .ifPresent(link -> dtoModel.add(link.withRel("levels")));
    }
}

@MvcComponent
class FeeYearPublicationsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(FeeYearPublicationController.class).listPublications())
                .ifPresent(link -> model.add(link.withRel("fee-year-publications")));
        return model;
    }
}
