package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse.FamilyGroupResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse.TrainingGroupResponse;
import com.klabis.members.domain.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members ", description = "Member registration and management API")
@ExposesResourceFor(Member.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
public class MemberController {

    private final ManagementPort managementService;
    private final MemberRepository memberRepository;
    private final PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler;
    private final MemberMapper memberMapper;
    private final TrainingGroupProvider trainingGroupProvider;
    private final FamilyGroupProvider familyGroupProvider;

    public MemberController(
            ManagementPort managementService,
            MemberRepository memberRepository,
            PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler,
            MemberMapper memberMapper,
            TrainingGroupProvider trainingGroupProvider,
            FamilyGroupProvider familyGroupProvider) {
        this.managementService = managementService;
        this.memberRepository = memberRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.memberMapper = memberMapper;
        this.trainingGroupProvider = trainingGroupProvider;
        this.familyGroupProvider = familyGroupProvider;
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @OwnerVisible
    @Operation(
            summary = "Update member information (partial update)",
            description = "Updates member information with PATCH semantics (partial update). " +
                          "Admin (MEMBERS:MANAGE) or owner can call this endpoint. " +
                          "Field-level authorization enforces which fields each role may submit. " +
                          "Only provided fields are updated; null/missing fields keep existing values."
    )
    @ApiResponse(responseCode = "204", description = "Member updated successfully")
    public ResponseEntity<Void> updateMember(
            @Parameter(description = "Member UUID") @OwnerId @PathVariable UUID id,
            @Parameter(description = "Partial update request - only include fields to update")
            @Valid @RequestBody UpdateMemberRequest request,
            @CurrentUser CurrentUserData currentUser) {

        MemberId memberId = new MemberId(id);
        var command = UpdateMemberRequestMapper.toCommand(request, currentUser.userId());
        Member updatedMember = managementService.updateMember(memberId, command);

        List<String> warnings = updatedMember.birthNumberConsistencyWarnings();
        if (!warnings.isEmpty()) {
            return ResponseEntity.noContent()
                    .header("X-Warnings", warnings.toArray(String[]::new))
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resume")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(
            summary = "Resume suspended member membership",
            description = "Resumes a suspended member's membership. " +
                          "Requires MEMBERS:MANAGE authority (admin-only). " +
                          "Sets active status to true and records resume timestamp and user who performed resume."
    )
    @ApiResponse(responseCode = "204", description = "Membership resumed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid resume request (e.g., member is already active)")
    @ApiResponse(responseCode = "403", description = "Forbidden - user lacks MEMBERS:MANAGE authority")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Void> resumeMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @CurrentUser UserId currentUserId) {

        var command = new Member.ResumeMembership(currentUserId);
        managementService.resumeMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged(), null)).toUri())
                .build();
    }

    @PostMapping(value = "/{id}/suspend", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(
            summary = "Suspend member membership",
            description = "Suspends a member's membership with a specified reason. " +
                          "Requires MEMBERS:MANAGE authority (admin-only). " +
                          "Sets active status to false and records suspension details including timestamp and user who performed suspension."
    )
    @ApiResponse(responseCode = "204", description = "Membership suspended successfully")
    @ApiResponse(responseCode = "400", description = "Invalid suspension request (e.g., already suspended)")
    @ApiResponse(responseCode = "403", description = "Forbidden - user lacks MEMBERS:MANAGE authority")
    @ApiResponse(responseCode = "404", description = "Member not found")
    @ApiResponse(responseCode = "409", description = "Member is the sole owner of one or more groups — designate a successor before suspension")
    public ResponseEntity<Void> suspendMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @Parameter(description = "Suspension request")
            @Valid @RequestBody @NotNull SuspendMembershipRequest request,
            @CurrentUser UserId currentUserId) {

        var command = new Member.SuspendMembership(
                currentUserId,
                request.reason(),
                request.note().orElse(null)
        );

        managementService.suspendMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged(), null)).toUri())
                .build();
    }

    @GetMapping(value = "/options", produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @Transactional(readOnly = true)
    @HasAuthority(Authority.MEMBERS_READ)
    public ResponseEntity<List<MemberOptionResponse>> listMemberOptions() {
        List<MemberOptionResponse> options = memberRepository.findAll(MemberFilter.activeOnly()).stream()
                .map(member -> new MemberOptionResponse(
                        member.getId().uuid().toString(),
                        "%s %s (%s)".formatted(member.getFirstName(), member.getLastName(), member.getRegistrationNumber().getValue())
                ))
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping
    @Transactional(readOnly = true)
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "List members with pagination and sorting",
            description = "Retrieves a paginated list of registered members with summary information (firstName, lastName, registrationNumber). " +
                          "Supports pagination (page, size) and sorting (sort=field,direction). " +
                          "Default: page=0, size=10, sort=lastName,asc. " +
                          "Allowed sort fields: firstName, lastName, registrationNumber. " +
                          "Returns HATEOAS links for navigation including pagination links (first, last, next, prev). " +
                          "Access is restricted to active members only - terminated members will receive 403 Forbidden."
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of members retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - user is not an active member")
    public ResponseEntity<PagedModel<EntityModel<MemberSummaryResponse>>> listMembers(
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) @ParameterObject Pageable pageable,
            @CurrentUser CurrentUserData currentUser) {

        validateSortFields(pageable.getSort());

        MemberFilter filter = currentUser.hasAuthority(Authority.MEMBERS_MANAGE)
                ? MemberFilter.all()
                : MemberFilter.activeOnly();

        Page<Member> memberPage = memberRepository.findAll(filter, pageable);

        PagedModel<EntityModel<MemberSummaryResponse>> pagedModel = pagedResourcesAssembler.toModel(
                memberPage.map(memberMapper::toSummaryResponse),
                response -> buildSummaryModel(response)
        );

        pagedModel.mapLink(IanaLinkRelations.SELF, oldLink -> buildCollectionSelfLink(pageable).orElse(oldLink));

        return ResponseEntity.ok(pagedModel);
    }

    private TrainingGroupResponse buildTrainingGroupResponse(TrainingGroupProvider.TrainingGroupData data) {
        List<TrainingGroupResponse.OwnerResponse> ownerResponses = memberRepository
                .findAllByIds(data.ownerIds()).stream()
                .map(owner -> new TrainingGroupResponse.OwnerResponse(
                        owner.getFirstName() + " " + owner.getLastName(),
                        owner.getEmail() != null ? owner.getEmail().value() : null))
                .toList();
        return new TrainingGroupResponse(data.groupName(), ownerResponses);
    }

    private FamilyGroupResponse buildFamilyGroupResponse(FamilyGroupProvider.FamilyGroupData data) {
        List<TrainingGroupResponse.OwnerResponse> ownerResponses = memberRepository
                .findAllByIds(data.ownerIds()).stream()
                .map(owner -> new TrainingGroupResponse.OwnerResponse(
                        owner.getFirstName() + " " + owner.getLastName(),
                        owner.getEmail() != null ? owner.getEmail().value() : null))
                .toList();
        return new FamilyGroupResponse(data.groupName(), ownerResponses);
    }

    private EntityModel<MemberSummaryResponse> buildSummaryModel(MemberSummaryResponse response) {
        UUID memberId = response.id().uuid();
        EntityModel<MemberSummaryResponse> model = EntityModel.of(response);

        buildMemberSelfLink(memberId, Boolean.TRUE.equals(response.active())).ifPresent(model::add);

        return model;
    }

    private java.util.Optional<Link> buildMemberSelfLink(UUID memberId, boolean isActive) {
        return klabisLinkTo(methodOn(MemberController.class).getMember(memberId, null)).map(selfLinkBuilder -> {
            var selfLink = selfLinkBuilder.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(memberId, null, null)));
            if (isActive) {
                selfLink = selfLink.andAffordances(
                        klabisAfford(methodOn(MemberController.class).suspendMember(memberId, null, null)));
            } else {
                selfLink = selfLink.andAffordances(
                        klabisAfford(methodOn(MemberController.class).resumeMember(memberId, null)));
            }
            return (Link) selfLink;
        });
    }

    private java.util.Optional<Link> buildCollectionSelfLink(Pageable pageable) {
        return klabisLinkTo(methodOn(MemberController.class).listMembers(pageable, null)).map(selfLinkBuilder ->
                (Link) selfLinkBuilder.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(null, null, null)))
                        .andAffordances(klabisAfford(methodOn(RegistrationController.class).registerMember(null, null)))
        );
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("firstName", "lastName", "registrationNumber");

    private void validateSortFields(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + ALLOWED_SORT_FIELDS
                        ),
                        null);
            }
        }
    }

    @GetMapping("/{id}")
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "Get member by ID",
            description = "Retrieves detailed member information by ID including personal information, " +
                          "contact details, and guardian information if applicable. Returns HATEOAS links for navigation."
    )
    @ApiResponse(responseCode = "200", description = "Member found")
    public ResponseEntity<EntityModel<MemberDetailsResponse>> getMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        MemberId memberId = new MemberId(id);
        Member member = managementService.getMemberAndRecordView(memberId, currentUser.userId(),
                currentUser.hasAuthority(Authority.MEMBERS_MANAGE));

        TrainingGroupResponse trainingGroupResponse = trainingGroupProvider.findTrainingGroupForMember(memberId)
                .map(data -> buildTrainingGroupResponse(data))
                .orElse(null);
        FamilyGroupResponse familyGroupResponse = familyGroupProvider.findFamilyGroupForMember(memberId)
                .map(data -> buildFamilyGroupResponse(data))
                .orElse(null);
        MemberDetailsResponse response = MemberDetailsResponseBuilder.builder(memberMapper.toDetailsResponse(member))
                .trainingGroup(trainingGroupResponse)
                .familyGroup(familyGroupResponse)
                .build();
        EntityModel<MemberDetailsResponse> entityModel = EntityModel.of(response);

        buildMemberSelfLink(id, member.isActive()).ifPresent(entityModel::add);

        klabisLinkTo(methodOn(MemberController.class).listMembers(
                org.springframework.data.domain.PageRequest.of(0, 10), null
        )).ifPresent(link -> entityModel.add(link.withRel("collection")));

        return ResponseEntity.ok(entityModel);
    }

}

@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
