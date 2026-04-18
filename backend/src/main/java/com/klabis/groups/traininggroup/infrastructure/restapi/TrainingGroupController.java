package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.application.UpdateTrainingGroupCommand;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import org.springframework.hateoas.server.ExposesResourceFor;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/training-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "TrainingGroups", description = "Training group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
@ExposesResourceFor(TrainingGroup.class)
class TrainingGroupController {

    private final TrainingGroupManagementPort trainingGroupManagementService;

    TrainingGroupController(TrainingGroupManagementPort trainingGroupManagementService) {
        this.trainingGroupManagementService = trainingGroupManagementService;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Create a training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> createTrainingGroup(
            @Valid @RequestBody CreateTrainingGroupRequest request,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        AgeRange ageRange = new AgeRange(request.minAge(), request.maxAge());
        TrainingGroup.CreateTrainingGroup command = new TrainingGroup.CreateTrainingGroup(
                request.name(), new MemberId(request.trainerId()), ageRange);
        TrainingGroup group = trainingGroupManagementService.createTrainingGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(TrainingGroupController.class).getTrainingGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "List all training groups (requires GROUPS:TRAINING)")
    ResponseEntity<CollectionModel<EntityModel<TrainingGroupSummaryResponse>>> listTrainingGroups(
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        List<TrainingGroup> groups = trainingGroupManagementService.listTrainingGroups();
        List<EntityModel<TrainingGroupSummaryResponse>> items = groups.stream()
                .map(this::buildTrainingGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<TrainingGroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).createTrainingGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get training group details")
    ResponseEntity<EntityModel<TrainingGroupResponse>> getTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
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
            klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
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
                group.getId(), group.getName(), null, null, trainerModels, null);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Update a training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> updateTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTrainingGroupRequest request,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        UpdateTrainingGroupCommand command = new UpdateTrainingGroupCommand(
                request.name(),
                request.ageRangeDomain(),
                request.trainerUuids().map(uuids -> uuids.stream()
                        .map(MemberId::new)
                        .collect(Collectors.toSet()))
        );
        trainingGroupManagementService.updateTrainingGroup(groupId, command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Delete a training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> deleteTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.deleteTrainingGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/members", consumes = "application/json")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Add a member to training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> addTrainingGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.addMemberToTrainingGroup(groupId, request.toMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Remove a member from training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> removeTrainingGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        MemberId memberToRemove = new MemberId(memberId);
        trainingGroupManagementService.removeMemberFromTrainingGroup(groupId, memberToRemove);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/trainers", consumes = "application/json")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Add a trainer to training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> addTrainer(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddTrainerRequest request,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.addTrainer(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/trainers/{memberId}")
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "Remove a trainer from training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> removeTrainer(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Trainer member UUID") @PathVariable UUID memberId,
            @ActingUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        TrainingGroupId groupId = new TrainingGroupId(id);
        trainingGroupManagementService.removeTrainer(groupId, new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }

    private EntityModel<TrainingGroupSummaryResponse> buildTrainingGroupSummaryModel(TrainingGroup group) {
        UUID groupId = group.getId().uuid();
        TrainingGroupSummaryResponse response = new TrainingGroupSummaryResponse(
                group.getId(), group.getName(),
                group.getAgeRange().minAge(), group.getAgeRange().maxAge(),
                group.getMembers().size());
        EntityModel<TrainingGroupSummaryResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
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
                        klabisLinkTo(methodOn(TrainingGroupController.class).removeTrainer(groupUuid, trainerId.uuid(), null))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
                                                .removeTrainer(groupUuid, trainerId.uuid(), null)))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<GroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid, hasTrainingAuthority, trainerIds))
                .toList();

        return new TrainingGroupResponse(
                group.getId(), group.getName(),
                group.getAgeRange().minAge(), group.getAgeRange().maxAge(),
                trainerModels, memberModels);
    }

    private EntityModel<GroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean hasTrainingAuthority, Set<MemberId> trainerIds) {

        MemberId memberId = MemberId.fromUserId(membership.userId());
        GroupMembershipResponse response = new GroupMembershipResponse(memberId.uuid(), membership.joinedAt());
        EntityModel<GroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MemberController.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);

        boolean memberIsTrainer = trainerIds.contains(memberId);
        if (hasTrainingAuthority && !memberIsTrainer) {
            klabisLinkTo(methodOn(TrainingGroupController.class)
                    .removeTrainingGroupMember(groupUuid, memberId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
                                    .removeTrainingGroupMember(groupUuid, memberId.uuid(), null)))));
        }

        return model;
    }

    private void requireTrainingAuthority(CurrentUserData currentUser) {
        if (!currentUser.hasAuthority(Authority.GROUPS_TRAINING)) {
            throw new InsufficientAuthorityException("GROUPS:TRAINING");
        }
    }
}

@MvcComponent
class TrainingGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<TrainingGroupResponse, TrainingGroup> {

    @Override
    public void process(EntityModel<TrainingGroupResponse> dtoModel, TrainingGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).updateTrainingGroup(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).deleteTrainingGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainingGroupMember(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainer(id, null, null))))
                .ifPresent(dtoModel::add);
    }
}

@MvcComponent
class TrainingGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
                .ifPresent(link -> model.add(link.withRel("training-groups")));
        return model;
    }
}
