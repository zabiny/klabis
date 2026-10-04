package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import org.jspecify.annotations.Nullable;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianKind;
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
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(LegalGuardianGroup.class)
class LegalGuardianGroupController implements LegalGuardianGroupsApi {

    private final LegalGuardianGroupPort legalGuardianGroupService;
    private final Members members;
    private final AuthorizationEvaluator authorizationEvaluator;

    LegalGuardianGroupController(LegalGuardianGroupPort legalGuardianGroupService, Members members,
                                 AuthorizationEvaluator authorizationEvaluator) {
        this.legalGuardianGroupService = legalGuardianGroupService;
        this.members = members;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public ResponseEntity<List<LegalGuardianGroupSummaryResponse>> listLegalGuardianGroups() {
        List<LegalGuardianGroup> groups = legalGuardianGroupService.listGroups();

        Map<LegalGuardianGroupId, List<GuardianContact>> guardians = legalGuardianGroupService.listGuardiansOf(groups);

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream()
                .map(group -> toSummaryResponse(group, guardians.getOrDefault(group.getId(), List.of())))
                .toList());
    }

    // The LegalGuardianGroupResponse record is generated, but its minors array is
    // List<EntityModel<X>> and each item carries its own _links at runtime
    // which the generator cannot express on the payload. The controller assembles them.
    @Override
    public ResponseEntity<LegalGuardianGroupResponse> getLegalGuardianGroup(UUID id) {
        LegalGuardianGroup group = legalGuardianGroupService.getGroup(new LegalGuardianGroupId(id));

        HalResponseContext.setDomain(group);
        return ResponseEntity.ok(toGroupResponse(group));
    }

    @Override
    public ResponseEntity<List<LegalGuardianGroupGuardianResponse>> listLegalGuardianGroupGuardians(
            UUID id, CurrentUserData currentUser) {
        LegalGuardianGroup group = legalGuardianGroupService.getGroup(new LegalGuardianGroupId(id));
        GuardianListAccess.require(authorizationEvaluator, currentUser, group);

        List<GuardianContact> contacts = legalGuardianGroupService.listGuardians(group);
        HalResponseContext.setDomainList(contacts);
        return ResponseEntity.ok(contacts.stream().map(this::toGuardianResponse).toList());
    }

    @Override
    public ResponseEntity<Void> setLegalGuardianGroupGuardians(UUID id, SetLegalGuardiansRequest request) {
        legalGuardianGroupService.changeGroupGuardians(new LegalGuardianGroupId(id), GuardianInputMapper.toInputs(request.legalGuardians()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> setMemberLegalGuardians(UUID id, SetLegalGuardiansRequest request) {
        legalGuardianGroupService.setGuardiansOf(new MemberId(id), GuardianInputMapper.toInputs(request.legalGuardians()));
        return ResponseEntity.noContent().build();
    }

    private LegalGuardianGroupGuardianResponse toGuardianResponse(GuardianContact contact) {
        return LegalGuardianGroupGuardianResponseBuilder.builder()
                .userId(contact.userId().uuid())
                .firstName(contact.firstName())
                .lastName(contact.lastName())
                .email(contact.email())
                .phone(contact.phone())
                .build();
    }

    private LegalGuardianGroupSummaryResponse toSummaryResponse(LegalGuardianGroup group, List<GuardianContact> contacts) {
        return LegalGuardianGroupSummaryResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .guardians(contacts.stream()
                        .map(contact -> LegalGuardianGroupSummaryGuardianBuilder.builder()
                                .userId(contact.userId().uuid())
                                .firstName(contact.firstName())
                                .lastName(contact.lastName())
                                .build())
                        .toList())
                .minorCount(group.getMinors().size())
                .build();
    }

    private LegalGuardianGroupResponse toGroupResponse(LegalGuardianGroup group) {
        Map<MemberId, MemberDto> minorDetails = members.findByIds(group.getMinors().stream()
                .map(GroupMembership::memberId)
                .toList());
        List<EntityModel<LegalGuardianGroupMinorResponse>> minorModels = group.getMinors().stream()
                .sorted(Comparator.comparing(GroupMembership::joinedAt))
                .map(membership -> toMinorModel(membership, minorDetails.get(membership.memberId())))
                .toList();

        return LegalGuardianGroupResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .minors(minorModels)
                .build();
    }

    private EntityModel<LegalGuardianGroupMinorResponse> toMinorModel(GroupMembership<MemberId> membership,
                                                                      @Nullable MemberDto details) {
        MemberId memberId = membership.memberId();
        EntityModel<LegalGuardianGroupMinorResponse> model = EntityModel.of(
                LegalGuardianGroupMinorResponseBuilder.builder()
                        .memberId(memberId.uuid())
                        .firstName(details != null ? details.firstName() : null)
                        .lastName(details != null ? details.lastName() : null)
                        .registrationNumber(details != null ? details.registrationNumber() : null)
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
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(LegalGuardianGroupsApi.class).setLegalGuardianGroupGuardians(id, null),
                                LegalGuardianOptions.forLegalGuardiansField())))
                .ifPresent(dtoModel::add);

        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).listLegalGuardianGroupGuardians(id, null))
                .ifPresent(link -> dtoModel.add(link.withRel("legalGuardians")));

        klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).listLegalGuardianGroups())
                .ifPresent(link -> dtoModel.add(link.withRel(IanaLinkRelations.COLLECTION)));
    }
}

@MvcComponent
class LegalGuardianGroupGuardianPostprocessor extends ModelWithDomainPostprocessor<LegalGuardianGroupGuardianResponse, GuardianContact> {

    @Override
    public void process(EntityModel<LegalGuardianGroupGuardianResponse> dtoModel, GuardianContact contact) {
        UUID userId = contact.userId().uuid();
        if (contact.kind() == GuardianKind.LEGAL_GUARDIAN) {
            klabisLinkTo(methodOn(LegalGuardiansApi.class).getLegalGuardian(userId))
                    .ifPresent(link -> dtoModel.add(link.withRel("legalGuardian")));
        } else {
            klabisLinkTo(methodOn(MembersApi.class).getMember(userId, null))
                    .ifPresent(link -> dtoModel.add(link.withRel("member")));
        }
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
