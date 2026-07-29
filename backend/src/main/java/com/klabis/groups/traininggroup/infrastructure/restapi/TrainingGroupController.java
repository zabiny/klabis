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
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
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
    public ResponseEntity<Void> createTrainingGroup(@RequestBody CreateTrainingGroupRequest request) {

        AgeRange ageRange = new AgeRange(request.minAge(), request.maxAge());
        TrainingGroup.CreateTrainingGroup command = new TrainingGroup.CreateTrainingGroup(
                request.name(), new MemberId(request.trainerId()), ageRange);
        TrainingGroup group = trainingGroupManagementService.createTrainingGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(TrainingGroupController.class).getTrainingGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<Collection<TrainingGroupSummaryResponse>> listTrainingGroups() {

        List<TrainingGroup> groups = trainingGroupManagementService.listTrainingGroups();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream().map(this::toSummaryResponse).toList());
    }

    // Excluded from generation — see groups.yaml header comment and the comment on this operation
    // there: the response embeds trainers/members as arrays of independently link-carrying items,
    // a shape HalResponseContext cannot reproduce. Kept hand-written, same precedent as
    // EventController.getEvent / MembershipFeeGroupController.getGroup.
    @Override
    public ResponseEntity<RepresentationModel<?>> getTrainingGroup(
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
        var model = entityModelWithDomain(response, group);

        if (hasTrainingAuthority) {
            klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups())
                    .ifPresent(link -> model.add(link.withRel("collection")));
        }

        return ResponseEntity.ok(model);
    }

    private TrainingGroupResponse buildLimitedGroupResponse(TrainingGroup group, UUID groupUuid) {
        List<EntityModel<TrainerResponse>> trainerModels = group.getTrainers().stream()
                .map(trainerId -> {
                    EntityModel<TrainerResponse> model = EntityModel.of(new TrainerResponse(trainerId.uuid()));
                    klabisLinkTo(methodOn(MemberController.class).getMember(trainerId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    return model;
                })
                .toList();

        return new TrainingGroupResponse(
                group.getId(), group.getName(), null, trainerModels, null);
    }

    @Override
    public ResponseEntity<Void> updateTrainingGroup(UUID id, @RequestBody UpdateTrainingGroupRequest request) {

        TrainingGroupId groupId = new TrainingGroupId(id);
        UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                request.name(),
                request.ageRangeDomain(),
                request.trainers().map(trainers -> trainers == null ? null : new HashSet<>(trainers))
        );
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
    public ResponseEntity<Void> addTrainingGroupMember(UUID id, @RequestBody TrainingGroupAddMemberRequest request) {

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
    public ResponseEntity<Void> addTrainer(UUID id, @RequestBody AddTrainerRequest request) {

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
                    klabisLinkTo(methodOn(MemberController.class).getMember(trainerId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    if (hasTrainingAuthority && trainerIds.size() > 1) {
                        klabisLinkTo(methodOn(TrainingGroupController.class).removeTrainer(groupUuid, trainerId.uuid()))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
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
        klabisLinkTo(methodOn(MemberController.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);

        boolean memberIsTrainer = trainerIds.contains(memberId);
        if (hasTrainingAuthority && !memberIsTrainer) {
            klabisLinkTo(methodOn(TrainingGroupController.class)
                    .removeTrainingGroupMember(groupUuid, memberId.uuid()))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
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
        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).updateTrainingGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).deleteTrainingGroup(id)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainingGroupMember(id, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainer(id, null))))
                .ifPresent(dtoModel::add);
    }
}

@MvcComponent
class TrainingGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups())
                .ifPresent(link -> model.add(link.withRel("training-groups")));
        return model;
    }
}

@MvcComponent
class TrainingGroupSummaryPostprocessor extends ModelWithDomainPostprocessor<TrainingGroupSummaryResponse, TrainingGroup> {

    @Override
    public void process(EntityModel<TrainingGroupSummaryResponse> dtoModel, TrainingGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(id, null))
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
                .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).createTrainingGroup(null))));
        return model;
    }
}
