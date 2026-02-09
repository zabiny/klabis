package com.klabis.members.management;

import com.klabis.common.root.RootModel;
import com.klabis.members.Member;
import com.klabis.users.Authority;
import com.klabis.users.authorization.HasAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * REST controller for Member resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for member management.
 * Produces HAL+FORMS media type for hypermedia support.
 */
@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members", description = "Member registration and management API")
@ExposesResourceFor(Member.class)
class MemberController {

    private static final String MEMBERS_READ = "MEMBERS:READ";

    private final ManagementService managementService;
    private final PagedResourcesAssembler<MemberSummaryDTO> pagedResourcesAssembler;

    public MemberController(
            ManagementService managementService,
            PagedResourcesAssembler<MemberSummaryDTO> pagedResourcesAssembler) {
        this.managementService = managementService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
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
     * @param id      member ID
     * @param request partial update request (only fields to update should be provided)
     * @param auth    OAuth2 authentication token (optional, for authorization)
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
                          "Admin-only fields: firstName, lastName, dateOfBirth, gender, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup. " +
                          "Returns HATEOAS links for resource navigation.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member updated successfully",
                    content = @Content(
                            mediaType = MediaTypes.HAL_FORMS_JSON_VALUE,
                            schema = @Schema(implementation = MemberDetailsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error - invalid request data or empty update",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions (editing other member without admin permission, or accessing admin-only fields)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Member not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - concurrent update (optimistic locking failure)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<EntityModel<MemberDetailsResponse>> updateMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @Parameter(description = "Partial update request - only include fields to update")
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication auth) {

        // Call service directly with request object (authorization and field filtering done in service)
        UUID updatedMemberId = managementService.updateMember(id, request);

        // Load updated member
        MemberDetailsDTO memberDTO = managementService.getMember(updatedMemberId);

        // Map to response
        MemberDetailsResponse response = mapToResponse(memberDTO);

        // Create entity model with HATEOAS links
        EntityModel<MemberDetailsResponse> entityModel = EntityModel.of(response);

        // Add self link
        entityModel.add(
                linkTo(methodOn(MemberController.class).getMember(updatedMemberId)).withSelfRel()
                        // Add edit link (indicates the edit capability is available)
                        .andAffordance(afford(methodOn(MemberController.class).updateMember(updatedMemberId, null, null)))
        );

        // Add collection link
        entityModel.add(linkTo(methodOn(MemberController.class).listMembers(
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).withRel("collection"));

        return ResponseEntity.ok(entityModel);
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
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "List members with pagination and sorting",
            description = "Retrieves a paginated list of registered members with summary information (firstName, lastName, registrationNumber). " +
                          "Supports pagination (page, size) and sorting (sort=field,direction). " +
                          "Default: page=0, size=10, sort=lastName,asc. " +
                          "Allowed sort fields: firstName, lastName, registrationNumber. " +
                          "Returns HATEOAS links for navigation including pagination links (first, last, next, prev).",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of members retrieved successfully",
                    content = @Content(
                            mediaType = MediaTypes.HAL_FORMS_JSON_VALUE,
                            schema = @Schema(implementation = MemberSummaryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - invalid sort field or page parameters",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions (requires MEMBERS:READ)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<PagedModel<EntityModel<MemberSummaryResponse>>> listMembers(
            @Parameter(description = "Pagination parameters: page (default=0), size (default=10, max=100), sort (default=lastName,asc). " +
                                     "Example: ?page=0&size=20&sort=lastName,asc&sort=firstName,asc")
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {

        // Validate sort fields
        validateSortFields(pageable.getSort());

        // Call service directly
        Page<MemberSummaryDTO> page = managementService.listMembers(pageable);

        // Convert to PagedModel using assembler
        PagedModel<EntityModel<MemberSummaryResponse>> pagedModel = pagedResourcesAssembler.toModel(
                page,
                dto -> {
                    MemberSummaryResponse response = new MemberSummaryResponse(
                            dto.id(),
                            dto.firstName(),
                            dto.lastName(),
                            dto.registrationNumber()
                    );
                    EntityModel<MemberSummaryResponse> model = EntityModel.of(response);
                    // Add self link to individual member
                    model.add(linkTo(methodOn(MemberController.class).getMember(dto.id())).withSelfRel());
                    return model;
                }
        );

        pagedModel.add(linkTo(methodOn(MemberController.class).listMembers(pageable)).withSelfRel()
                .andAffordance(afford(methodOn(RegistrationController.class).registerMember(null)))
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
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                        ". Allowed fields: " + ALLOWED_SORT_FIELDS
                );
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
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "Get member by ID",
            description = "Retrieves detailed member information by ID including personal information, " +
                          "contact details, and guardian information if applicable. Returns HATEOAS links for navigation.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member found",
                    content = @Content(
                            mediaType = MediaTypes.HAL_FORMS_JSON_VALUE,
                            schema = @Schema(implementation = MemberDetailsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Member not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions (requires MEMBERS:READ)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<EntityModel<MemberDetailsResponse>> getMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id) {

        // Call service directly
        MemberDetailsDTO memberDTO = managementService.getMember(id);

        // Map to response
        MemberDetailsResponse response = mapToResponse(memberDTO);

        // Create entity model with HATEOAS links
        EntityModel<MemberDetailsResponse> entityModel = EntityModel.of(response);

        // Add self link
        entityModel.add(
                linkTo(methodOn(MemberController.class).getMember(id)).withSelfRel()
                        // Add edit link (indicates the edit capability is available)
                        .andAffordance(afford(methodOn(MemberController.class).updateMember(id, null, null)))
        );

        // Add collection link
        entityModel.add(linkTo(methodOn(MemberController.class).listMembers(
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).withRel("collection"));

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Maps MemberDetailsDTO to MemberDetailsResponse.
     * <p>
     * Note: This is a simple field-by-field mapping since the DTOs have identical structure.
     * GuardianDTO and AddressResponse are shared across layers to eliminate duplication.
     *
     * @param dto the application layer DTO
     * @return the presentation layer response
     */
    private MemberDetailsResponse mapToResponse(MemberDetailsDTO dto) {
        return new MemberDetailsResponse(
                dto.id(),
                dto.registrationNumber(),
                dto.firstName(),
                dto.lastName(),
                dto.dateOfBirth(),
                dto.nationality(),
                dto.gender(),
                dto.email(),
                dto.phone(),
                dto.address(),
                dto.guardian(),
                dto.active(),
                dto.chipNumber(),
                dto.identityCard(),
                dto.medicalCourse(),
                dto.trainerLicense(),
                dto.drivingLicenseGroup(),
                dto.dietaryRestrictions()
        );
    }
}

@Component
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(linkTo(methodOn(MemberController.class).listMembers(null)).withRel("members"));
        return model;
    }
}