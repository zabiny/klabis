package com.klabis.members.infrastructure.restapi;

import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUser;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementService;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import com.klabis.common.mvc.MvcComponent;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Member resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for member management.
 * Produces HAL+FORMS media type for hypermedia support.
 */
@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members ", description = "Member registration and management API")
@ExposesResourceFor(Member.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class MemberController {

    private final ManagementService managementService;
    private final MemberRepository memberRepository;
    private final PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler;
    private final MemberMapper memberMapper;

    public MemberController(
            ManagementService managementService,
            MemberRepository memberRepository,
            PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler,
            MemberMapper memberMapper) {
        this.managementService = managementService;
        this.memberRepository = memberRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.memberMapper = memberMapper;
    }

    /**
     * Update member information (partial update).
     * <p>
     * PATCH /api/members/{id}
     * <p>
     * Supports dual authorization model:
     * <ul>
     *   <li><b>Self-edit:</b> Authenticated members can update their own information (email, phone, address, dietaryRestrictions)</li>
     *   <li><b>Admin edit:</b> Users with MEMBERS:UPDATE authority can update any member and all fields</li>
     * </ul>
     * <p>
     * <b>PATCH Semantics:</b> Only fields provided (non-null in request) are updated.
     * Null values in request are ignored (field keeps existing value).
     * <p>
     * <b>Field Access Control:</b>
     * <ul>
     *   <li>Member-editable: email, phone, address, dietaryRestrictions</li>
     *   <li>Admin-only: firstName, lastName, dateOfBirth, gender, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup</li>
     * </ul>
     *
     * @param id             member ID
     * @param request        partial update request (only fields to update should be provided)
     * @param authentication Spring Security authentication for authority checking
     * @return 200 OK with updated member resource
     */
    @PatchMapping(value = "/{id}", consumes = "application/json")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update member information (partial update)",
            description = "Updates member information with PATCH semantics (partial update). " +
                          "Supports dual authorization: members can edit their own information (limited fields), " +
                          "users with MEMBERS:UPDATE authority can edit any member (all fields). " +
                          "Only provided fields are updated; null/missing fields keep existing values. " +
                          "Member-editable fields: email, phone, address, dietaryRestrictions. " +
                          "Admin-only fields: firstName, lastName, dateOfBirth, gender, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup."
    )
    @ApiResponse(responseCode = "204", description = "Member updated successfully")
    public ResponseEntity<Void> updateMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @Parameter(description = "Partial update request - only include fields to update")
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication authentication) {

        MemberId memberId = new MemberId(id);
        if (hasAuthority(authentication, Authority.MEMBERS_UPDATE)) {
            var adminCommand = UpdateMemberRequestMapper.toAdminCommand(request);
            managementService.updateMember(memberId, adminCommand);
        } else {
            UserId currentUserId = extractUserId(authentication);
            if (!currentUserId.uuid().equals(id)) {
                throw new ErrorResponseException(HttpStatus.FORBIDDEN,
                        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                                "You can only edit your own information"), null);
            }
            var selfCommand = UpdateMemberRequestMapper.toSelfUpdateCommand(request);
            managementService.updateMember(memberId, selfCommand);
        }
        return ResponseEntity.noContent().build();
    }

    private boolean hasAuthority(Authentication authentication, Authority requiredAuthority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(requiredAuthority.getValue()));
    }

    private UserId extractUserId(Authentication authentication) {
        if (authentication instanceof com.klabis.common.security.KlabisJwtAuthenticationToken token) {
            return token.getUserId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication, got: " + authentication.getClass()
                .getName());
    }

    @PostMapping("/{id}/resume")
    @HasAuthority(Authority.MEMBERS_UPDATE)
    @Operation(
            summary = "Resume suspended member membership",
            description = "Resumes a suspended member's membership. " +
                          "Requires MEMBERS:UPDATE authority (admin-only). " +
                          "Sets active status to true and records resume timestamp and user who performed resume."
    )
    @ApiResponse(responseCode = "204", description = "Membership resumed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid resume request (e.g., member is already active)")
    @ApiResponse(responseCode = "403", description = "Forbidden - user lacks MEMBERS:UPDATE authority")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Void> resumeMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @CurrentUser UserId currentUserId) {

        var command = new Member.ResumeMembership(currentUserId);
        managementService.resumeMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged())).toUri())
                .build();
    }

    /**
     * Suspend a member's membership.
     * <p>
     * POST /api/members/{id}/suspend
     * <p>
     * Admin-only endpoint for suspending a member's membership.
     * Requires MEMBERS:UPDATE authority.
     * <p>
     * Sets member's active status to false and records suspension details.
     * Publishes MemberSuspendedEvent for integration with other modules.
     *
     * @param id      member ID
     * @param request suspension request with reason and optional note
     * @param currentUserId the user performing the suspension
     * @return 204 No Content on success
     */
    @PostMapping(value = "/{id}/suspend", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_UPDATE)
    @Operation(
            summary = "Suspend member membership",
            description = "Suspends a member's membership with a specified reason. " +
                          "Requires MEMBERS:UPDATE authority (admin-only). " +
                          "Sets active status to false and records suspension details including timestamp and user who performed suspension."
    )
    @ApiResponse(responseCode = "204", description = "Membership suspended successfully")
    @ApiResponse(responseCode = "400", description = "Invalid suspension request (e.g., already suspended)")
    @ApiResponse(responseCode = "403", description = "Forbidden - user lacks MEMBERS:UPDATE authority")
    @ApiResponse(responseCode = "404", description = "Member not found")
    @ApiResponse(responseCode = "409", description = "Conflict - concurrent modification detected")
    public ResponseEntity<Void> suspendMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @Parameter(description = "Suspension request")
            @Valid @RequestBody SuspendMembershipRequest request,
            @CurrentUser UserId currentUserId) {

        var command = new Member.SuspendMembership(
                currentUserId,
                request.reason(),
                request.note().orElse(null)
        );

        managementService.suspendMember(new MemberId(id), command);
        return ResponseEntity.noContent()
                .location(linkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged())).toUri())
                .build();
    }

    /**
     * List members with pagination and sorting.
     * <p>
     * GET /api/members?page=0&size=10&sort=lastName,asc
     *
     * @param pageable pagination and sorting parameters (page, size, sort)
     * @return paginated collection of member summaries with HATEOAS links and page metadata
     */
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
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) @ParameterObject Pageable pageable) {

        validateSortFields(pageable.getSort());

        Page<Member> memberPage = memberRepository.findAll(pageable);

        PagedModel<EntityModel<MemberSummaryResponse>> pagedModel = pagedResourcesAssembler.toModel(
                memberPage.map(memberMapper::toSummaryResponse),
                response -> {
                    EntityModel<MemberSummaryResponse> model = EntityModel.of(response);
                    model.add(klabisLinkTo(methodOn(MemberController.class).getMember(response.id())).withSelfRel());
                    return model;
                }
        );

        pagedModel.mapLink(IanaLinkRelations.SELF,
                oldLink -> klabisLinkTo(methodOn(MemberController.class).listMembers(pageable)).withSelfRel()
                        .andAffordances(klabisAfford(methodOn(RegistrationController.class).registerMember(null)))
        );

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Validates that all sort fields are in the allowed list.
     *
     * @param sort the sort specification to validate
     * @throws IllegalArgumentException if any sort field is not allowed
     */
    private void validateSortFields(Sort sort) {
        final var ALLOWED_SORT_FIELDS = java.util.Set.of("firstName", "lastName", "registrationNumber");

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

    /**
     * Get member by ID.
     * <p>
     * GET /api/members/{id}
     *
     * @param id member ID
     * @return member resource with full details
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "Get member by ID",
            description = "Retrieves detailed member information by ID including personal information, " +
                          "contact details, and guardian information if applicable. Returns HATEOAS links for navigation."
    )
    @ApiResponse(responseCode = "200", description = "Member found")
    public ResponseEntity<EntityModel<MemberDetailsResponse>> getMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id) {

        MemberId memberId = new MemberId(id);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        MemberDetailsResponse response = memberMapper.toDetailsResponse(member);
        EntityModel<MemberDetailsResponse> entityModel = EntityModel.of(response);

        if (member.isActive()) {
            entityModel.add(
                    klabisLinkTo(methodOn(MemberController.class).getMember(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(id,
                                    (UpdateMemberRequest) null,
                                    null)))
                            .andAffordances(klabisAfford(methodOn(MemberController.class).suspendMember(id,
                                    (SuspendMembershipRequest) null,
                                    null)))
            );
        } else {
            entityModel.add(
                    klabisLinkTo(methodOn(MemberController.class).getMember(id)).withSelfRel()
                            .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(id,
                                    (UpdateMemberRequest) null,
                                    null)))
                            .andAffordances(klabisAfford(methodOn(MemberController.class).resumeMember(id, null)))
            );
        }

        entityModel.add(klabisLinkTo(methodOn(MemberController.class).listMembers(
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).withRel("collection"));

        return ResponseEntity.ok(entityModel);
    }

}

@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(klabisLinkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged())).withRel(
                "members"));
        return model;
    }
}