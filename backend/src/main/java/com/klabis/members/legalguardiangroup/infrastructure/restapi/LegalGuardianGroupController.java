package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.*;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(LegalGuardianGroup.class)
class LegalGuardianGroupController implements LegalGuardianGroupsApi {

    private final LegalGuardianGroupPort legalGuardianGroupService;

    LegalGuardianGroupController(LegalGuardianGroupPort legalGuardianGroupService) {
        this.legalGuardianGroupService = legalGuardianGroupService;
    }

    @Override
    public ResponseEntity<List<LegalGuardianGroupSummaryResponse>> listLegalGuardianGroups() {
        List<LegalGuardianGroup> groups = legalGuardianGroupService.listGroups();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream().map(this::toSummaryResponse).toList());
    }

    // The LegalGuardianGroupResponse record is generated, but its guardians/minors arrays are
    // List<EntityModel<X>> and each item carries its own _links at runtime — a "member" link on every row —
    // which the generator cannot express on the payload. The controller assembles them.
    @Override
    public ResponseEntity<LegalGuardianGroupResponse> getLegalGuardianGroup(UUID id) {
        LegalGuardianGroup group = legalGuardianGroupService.getGroup(new LegalGuardianGroupId(id));

        HalResponseContext.setDomain(group);
        return ResponseEntity.ok(toGroupResponse(group));
    }

    @Override
    public ResponseEntity<Void> setLegalGuardianGroupGuardians(UUID id, SetLegalGuardiansRequest request) {
        Set<UserId> guardians = request.legalGuardians().stream()
                .map(guardian -> new UserId(guardian.userId()))
                .collect(Collectors.toSet());

        legalGuardianGroupService.changeGroupGuardians(new LegalGuardianGroupId(id), guardians);
        return ResponseEntity.noContent().build();
    }

    private LegalGuardianGroupSummaryResponse toSummaryResponse(LegalGuardianGroup group) {
        return LegalGuardianGroupSummaryResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .minorCount(group.getMinors().size())
                .build();
    }

    private LegalGuardianGroupResponse toGroupResponse(LegalGuardianGroup group) {
        List<EntityModel<LegalGuardianGroupGuardianResponse>> guardianModels = group.getGuardians().stream()
                .map(guardianId -> {
                    EntityModel<LegalGuardianGroupGuardianResponse> model = EntityModel.of(
                            LegalGuardianGroupGuardianResponseBuilder.builder().userId(guardianId.uuid()).build());
                    klabisLinkTo(methodOn(MembersApi.class).getMember(guardianId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    return model;
                })
                .toList();

        List<EntityModel<LegalGuardianGroupMinorResponse>> minorModels = group.getMinors().stream()
                .sorted(Comparator.comparing(GroupMembership::joinedAt))
                .map(this::toMinorModel)
                .toList();

        return LegalGuardianGroupResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .guardians(guardianModels)
                .minors(minorModels)
                .build();
    }

    private EntityModel<LegalGuardianGroupMinorResponse> toMinorModel(GroupMembership<MemberId> membership) {
        MemberId memberId = membership.memberId();
        EntityModel<LegalGuardianGroupMinorResponse> model = EntityModel.of(
                LegalGuardianGroupMinorResponseBuilder.builder()
                        .memberId(memberId.uuid())
                        .joinedAt(membership.joinedAt())
                        .build());
        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        return model;
    }
}

@MvcComponent
class LegalGuardianGroupSummaryPostprocessor extends ModelWithDomainPostprocessor<LegalGuardianGroupSummaryResponse, LegalGuardianGroup> {

    @Override
    public void process(EntityModel<LegalGuardianGroupSummaryResponse> dtoModel, LegalGuardianGroup group) {
        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).getLegalGuardianGroup(group.getId().uuid()))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
    }
}

@MvcComponent
class LegalGuardianGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<LegalGuardianGroupResponse, LegalGuardianGroup> {

    @Override
    public void process(EntityModel<LegalGuardianGroupResponse> dtoModel, LegalGuardianGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).getLegalGuardianGroup(id))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(
                                methodOn(LegalGuardianGroupsApi.class).setLegalGuardianGroupGuardians(id, null))))
                .ifPresent(dtoModel::add);

        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).listLegalGuardianGroups())
                .ifPresent(link -> dtoModel.add(link.withRel(IanaLinkRelations.COLLECTION)));
    }
}

@MvcComponent
class LegalGuardianGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).listLegalGuardianGroups())
                .ifPresent(link -> model.add(link.withRel("legalGuardianGroups")));
        return model;
    }
}
