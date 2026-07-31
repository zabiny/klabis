package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.application.UpdateTrainingGroupCommand;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MembersApi;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static java.util.stream.Collectors.toSet;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "TrainingGroups", description = "Training group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
@ExposesResourceFor(TrainingGroup.class)
class TrainingGroupController implements TrainingGroupsApi {

    private final TrainingGroupManagementPort trainingGroupManagementService;

    TrainingGroupController(TrainingGroupManagementPort trainingGroupManagementService) {
        this.trainingGroupManagementService = trainingGroupManagementService;
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
    public ResponseEntity<Collection<TrainingGroupSummaryResponse>> listTrainingGroups() {

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

        boolean hasTrainingAuthority = currentUser.hasAuthority(Authority.GROUPS_TRAINING);
        boolean isMember = currentUser.isMemberOf(group::hasMember);
        boolean isTrainer = currentUser.isMemberOf(group::hasTrainer);

        if (!hasTrainingAuthority && !isMember && !isTrainer) {
            throw new InsufficientAuthorityException("GROUPS:TRAINING or group membership required");
        }

        TrainingGroupResponse response = hasTrainingAuthority
                ? toTrainingGroupResponse(group, id, true)
                : buildLimitedGroupResponse(group, id);

        HalResponseContext.setDomain(group);
        return ResponseEntity.ok(response);
    }

    private TrainingGroupResponse buildLimitedGroupResponse(TrainingGroup group, UUID groupUuid) {
        List<EntityModel<TrainerResponse>> trainerModels = group.getTrainers().stream()
                .map(trainerId -> {
                    EntityModel<TrainerResponse> model = EntityModel.of(new TrainerResponse(trainerId.uuid()));
                    klabisLinkTo(methodOn(MembersApi.class).getMember(trainerId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    return model;
                })
                .toList();

        return new TrainingGroupResponse(
                group.getId(), group.getName(), null, trainerModels, null);
    }

    @Override
    public ResponseEntity<Void> updateTrainingGroup(UUID id, UpdateTrainingGroupRequest request) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                request.name(),
                request.ageRange().map(TrainingGroupController::toAgeRange),
                request.trainers().map(TrainingGroupController::toMemberIds)
        );
        trainingGroupManagementService.updateTrainingGroup(groupId, command);
        return ResponseEntity.noContent().build();
    }

    /**
     * Both converters forward an explicit null instead of dereferencing it, so the domain's own
     * Assert rejects it as a 400. Mapping it here would NPE into a 500 — {@code map} applies the
     * mapper on presence, not on nullness.
     */
    private static AgeRange toAgeRange(AgeRangeRequest request) {
        return request == null ? null : new AgeRange(request.minAge(), request.maxAge());
    }

    private static Set<MemberId> toMemberIds(List<UUID> trainers) {
        return trainers == null ? null : trainers.stream().map(MemberId::new).collect(toSet());
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
        return new TrainingGroupSummaryResponse(
                group.getId(), group.getName(),
                group.getAgeRange().minAge(), group.getAgeRange().maxAge(),
                group.getMembers().size());
    }

    private TrainingGroupResponse toTrainingGroupResponse(TrainingGroup group, UUID groupUuid, boolean hasTrainingAuthority) {
        Set<MemberId> trainerIds = group.getTrainers();

        List<EntityModel<TrainerResponse>> trainerModels = trainerIds.stream()
                .map(trainerId -> {
                    EntityModel<TrainerResponse> model = EntityModel.of(new TrainerResponse(trainerId.uuid()));
                    klabisLinkTo(methodOn(MembersApi.class).getMember(trainerId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    if (hasTrainingAuthority && trainerIds.size() > 1) {
                        klabisLinkTo(methodOn(TrainingGroupsApi.class).removeTrainer(groupUuid, trainerId.uuid()))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class)
                                                .removeTrainer(groupUuid, trainerId.uuid())))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<GroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid, hasTrainingAuthority, trainerIds))
                .toList();

        return new TrainingGroupResponse(
                group.getId(), group.getName(),
                new AgeRangeResponse(group.getAgeRange().minAge(), group.getAgeRange().maxAge()),
                trainerModels, memberModels);
    }

    private EntityModel<GroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean hasTrainingAuthority, Set<MemberId> trainerIds) {

        MemberId memberId = membership.memberId();
        GroupMembershipResponse response = new GroupMembershipResponse(memberId.uuid(), membership.joinedAt());
        EntityModel<GroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);

        boolean memberIsTrainer = trainerIds.contains(memberId);
        if (hasTrainingAuthority && !memberIsTrainer) {
            klabisLinkTo(methodOn(TrainingGroupsApi.class)
                    .removeTrainingGroupMember(groupUuid, memberId.uuid()))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class)
                                    .removeTrainingGroupMember(groupUuid, memberId.uuid())))));
        }

        return model;
    }
}

@MvcComponent
class TrainingGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<TrainingGroupResponse, TrainingGroup> {

    @Override
    public void process(EntityModel<TrainingGroupResponse> dtoModel, TrainingGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(TrainingGroupsApi.class).getTrainingGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).updateTrainingGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).deleteTrainingGroup(id)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).addTrainingGroupMember(id, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).addTrainer(id, null))))
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
                .andAffordances(klabisAfford(methodOn(TrainingGroupsApi.class).createTrainingGroup(null))));
        return model;
    }
}
