package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.Authority;
import com.klabis.groups.infrastructure.restapi.*;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(TrainingGroup.class)
class TrainingGroupController implements TrainingGroupsApi {

    private final TrainingGroupManagementPort trainingGroupManagementService;
    private final AuthorizationEvaluator authorizationEvaluator;

    TrainingGroupController(TrainingGroupManagementPort trainingGroupManagementService,
                            AuthorizationEvaluator authorizationEvaluator) {
        this.trainingGroupManagementService = trainingGroupManagementService;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public ResponseEntity<Void> createTrainingGroup(CreateTrainingGroupRequest request) {

        AgeRange ageRange = new AgeRange(request.minAge(), request.maxAge());
        TrainingGroup.CreateTrainingGroup command = new TrainingGroup.CreateTrainingGroup(
                request.name(), new MemberId(request.trainerId()), ageRange);
        TrainingGroup group = trainingGroupManagementService.createTrainingGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(TrainingGroupsApi.class).getTrainingGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<List<TrainingGroupSummaryResponse>> listTrainingGroups() {

        List<TrainingGroup> groups = trainingGroupManagementService.listTrainingGroups();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream().map(this::toSummaryResponse).toList());
    }

    // The response record is hand-written because trainers/members are List<EntityModel<X>> — each
    // item carries its own _links/_templates, which the generator cannot express. Everything else,
    // the payload type included, comes from the spec.
    @Override
    public ResponseEntity<TrainingGroupResponse> getTrainingGroup(
            UUID id,
            @ActingUser CurrentUserData currentUser) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        TrainingGroup group = trainingGroupManagementService.getTrainingGroup(groupId);

        boolean hasTrainingAuthority = authorizationEvaluator.has(Authority.GROUPS_TRAINING);
        boolean isMember = currentUser.isMemberOf(group::hasMember);
        boolean isTrainer = currentUser.isMemberOf(group::hasTrainer);

        if (!hasTrainingAuthority && !isMember && !isTrainer) {
            throw new InsufficientAuthorityException("GROUPS:TRAINING or group membership required");
        }

        TrainingGroupResponse response = hasTrainingAuthority
                ? toTrainingGroupResponse(group)
                : buildLimitedGroupResponse(group);

        HalResponseContext.setDomain(group);
        HalResponseContext.setDeferredContext(TrainingGroupItemContext.class,
                () -> new TrainingGroupItemContext(group, hasTrainingAuthority));
        return ResponseEntity.ok(response);
    }

    private TrainingGroupResponse buildLimitedGroupResponse(TrainingGroup group) {
        return TrainingGroupResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .ageRange(null)
                .trainers(trainerModels(group))
                .members(null)
                .build();
    }

    @Override
    public ResponseEntity<Void> updateTrainingGroup(UUID id, UpdateTrainingGroupRequest request) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        var prefilled = trainingGroupManagementService.prefilledUpdateCommand(groupId);
        var command = UpdateTrainingGroupRequestMapper.toCommand(request, prefilled);
        trainingGroupManagementService.updateTrainingGroup(groupId, command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTrainingGroup(UUID id) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.deleteTrainingGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addTrainingGroupMember(UUID id, TrainingGroupAddMemberRequest request) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.addMemberToTrainingGroup(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeTrainingGroupMember(UUID id, UUID memberId) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        MemberId memberToRemove = new MemberId(memberId);
        trainingGroupManagementService.removeMemberFromTrainingGroup(groupId, memberToRemove);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addTrainer(UUID id, AddTrainerRequest request) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.addTrainer(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeTrainer(UUID id, UUID memberId) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.removeTrainer(groupId, new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }

    private TrainingGroupSummaryResponse toSummaryResponse(TrainingGroup group) {
        return TrainingGroupSummaryResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .minAge(group.getAgeRange().minAge())
                .maxAge(group.getAgeRange().maxAge())
                .memberCount(group.getMembers().size())
                .build();
    }

    private TrainingGroupResponse toTrainingGroupResponse(TrainingGroup group) {
        List<EntityModel<GroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> EntityModel.of(GroupMembershipResponseBuilder.builder()
                        .memberId(m.memberId().uuid())
                        .joinedAt(m.joinedAt())
                        .build()))
                .toList();

        return TrainingGroupResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .ageRange(AgeRangeResponseBuilder.builder()
                        .minAge(group.getAgeRange().minAge())
                        .maxAge(group.getAgeRange().maxAge())
                        .build())
                .trainers(trainerModels(group))
                .members(memberModels)
                .build();
    }

    private List<EntityModel<TrainerResponse>> trainerModels(TrainingGroup group) {
        return group.getTrainers().stream()
                .map(trainerId -> EntityModel.of(TrainerResponseBuilder.builder().memberId(trainerId.uuid()).build()))
                .toList();
    }
}

/**
 * The group a nested trainer or member item belongs to, published once per {@code getTrainingGroup}
 * request so the stateless item postprocessors can read it. Item remove affordances are offered only
 * to callers holding GROUPS:TRAINING; plain members get just the member link.
 */
record TrainingGroupItemContext(TrainingGroup group, boolean hasTrainingAuthority) {

    // Null outside the group-detail request: item postprocessors are global per item DTO type, so the
    // same DTO rendered elsewhere must pass through unchanged.
    static @Nullable TrainingGroupItemContext current() {
        return HalResponseContext.findContext(TrainingGroupItemContext.class).orElse(null);
    }
}

@MvcComponent
class TrainerItemPostprocessor implements RepresentationModelProcessor<EntityModel<TrainerResponse>> {

    @Override
    public EntityModel<TrainerResponse> process(EntityModel<TrainerResponse> model) {
        TrainingGroupItemContext context = TrainingGroupItemContext.current();
        if (context == null) {
            return model;
        }
        UUID groupUuid = context.group().getId().uuid();
        UUID trainerUuid = model.getContent().memberId();

        klabisLinkTo(methodOn(MembersApi.class).getMember(trainerUuid, null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (context.hasTrainingAuthority() && context.group().getTrainers().size() > 1) {
            klabisLinkTo(methodOn(TrainingGroupsApi.class).removeTrainer(groupUuid, trainerUuid))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class)
                                    .removeTrainer(groupUuid, trainerUuid)))));
        }
        return model;
    }
}

@MvcComponent
class TrainingGroupMembershipItemPostprocessor implements RepresentationModelProcessor<EntityModel<GroupMembershipResponse>> {

    @Override
    public EntityModel<GroupMembershipResponse> process(EntityModel<GroupMembershipResponse> model) {
        TrainingGroupItemContext context = TrainingGroupItemContext.current();
        if (context == null) {
            return model;
        }
        UUID groupUuid = context.group().getId().uuid();
        UUID memberUuid = model.getContent().memberId();

        klabisLinkTo(methodOn(MembersApi.class).getMember(memberUuid, null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (context.hasTrainingAuthority()) {
            klabisLinkTo(methodOn(TrainingGroupsApi.class).removeTrainingGroupMember(groupUuid, memberUuid))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class)
                                    .removeTrainingGroupMember(groupUuid, memberUuid)))));
        }
        return model;
    }
}

@MvcComponent
class TrainingGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<TrainingGroupResponse, TrainingGroup> {

    @Override
    public void process(EntityModel<TrainingGroupResponse> dtoModel, TrainingGroup group) {
        UUID id = group.getId().uuid();
        HalFormsOptionsDef.Remote memberOptions = HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions());
        klabisLinkTo(methodOn(TrainingGroupsApi.class).getTrainingGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(TrainingGroupsApi.class).updateTrainingGroup(id, null),
                                Map.of("trainers", memberOptions)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).deleteTrainingGroup(id)))
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(TrainingGroupsApi.class).addTrainingGroupMember(id, null),
                                Map.of("memberId", memberOptions)))
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(TrainingGroupsApi.class).addTrainer(id, null),
                                Map.of("memberId", memberOptions))))
                .ifPresent(dtoModel::add);

        // klabisLinkTo omits this for callers without GROUPS:TRAINING, which is the authority
        // listTrainingGroups requires — the same condition the controller used to check by hand.
        klabisLinkTo(methodOn(TrainingGroupsApi.class).listTrainingGroups())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

@MvcComponent
class TrainingGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(TrainingGroupsApi.class).listTrainingGroups())
                .ifPresent(link -> model.add(link.withRel("training-groups")));
        return model;
    }
}

@MvcComponent
class TrainingGroupSummaryPostprocessor extends ModelWithDomainPostprocessor<TrainingGroupSummaryResponse, TrainingGroup> {

    @Override
    public void process(EntityModel<TrainingGroupSummaryResponse> dtoModel, TrainingGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(TrainingGroupsApi.class).getTrainingGroup(id, null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
    }
}

// The self link itself is built by HalResponseBodyAdvice from the current request; this processor
// only contributes the create affordance, which stays authorization-sensitive via klabisAfford.
@MvcComponent
class TrainingGroupListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<TrainingGroupSummaryResponse>>> {

    @Override
    public CollectionModel<EntityModel<TrainingGroupSummaryResponse>> process(
            CollectionModel<EntityModel<TrainingGroupSummaryResponse>> model) {
        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (org.springframework.hateoas.Link) selfLink
                .andAffordances(klabisAffordWithOptions(
                        methodOn(TrainingGroupsApi.class).createTrainingGroup(null),
                        Map.of("trainerId", HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions())))));
        return model;
    }
}
