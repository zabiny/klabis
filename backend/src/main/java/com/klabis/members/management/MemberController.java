package com.klabis.members.management;

import com.klabis.common.ui.RootModel;
import com.klabis.members.*;
import com.klabis.users.Authority;
import com.klabis.users.UserId;
import com.klabis.users.authorization.HasAuthority;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
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
@Tag(name = "Members ", description = "Member registration and management API")
@ExposesResourceFor(Member.class)
@SecurityRequirement(name = "OAuth2")
class MemberController {

    private final ManagementService managementService;
    private final Members memberRepository;
    private final PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler;

    public MemberController(
            ManagementService managementService,
            Members memberRepository,
            PagedResourcesAssembler<MemberSummaryResponse> pagedResourcesAssembler) {
        this.managementService = managementService;
        this.memberRepository = memberRepository;
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
                          "Returns HATEOAS links for resource navigation."
    )
    @ApiResponse(responseCode = "200", description = "Member updated successfully")
    public ResponseEntity<EntityModel<MemberDetailsResponse>> updateMember(
            @Parameter(description = "Member UUID") @PathVariable UUID id,
            @Parameter(description = "Partial update request - only include fields to update")
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication auth) {

        // Call service for update (authorization and field filtering done in service)
        UUID updatedMemberId = managementService.updateMember(id, request);

        // Load updated member directly from repository
        Member updatedMember = memberRepository.findById(new UserId(updatedMemberId))
                .orElseThrow(() -> new MemberNotFoundException(updatedMemberId));

        // Map to response
        MemberDetailsResponse response = mapToDetailsResponse(updatedMember);

        // Create entity model with HATEOAS links
        EntityModel<MemberDetailsResponse> entityModel = EntityModel.of(response);

        // Add self link
        entityModel.add(
                linkTo(methodOn(MemberController.class).getMember(updatedMemberId)).withSelfRel()
                        // Add edit link (indicates the edit capability is available)
                        .andAffordance(afford(methodOn(MemberController.class).updateMember(updatedMemberId,
                                null,
                                null)))
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
    @Transactional(readOnly = true)
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(
            summary = "List members with pagination and sorting",
            description = "Retrieves a paginated list of registered members with summary information (firstName, lastName, registrationNumber). " +
                          "Supports pagination (page, size) and sorting (sort=field,direction). " +
                          "Default: page=0, size=10, sort=lastName,asc. " +
                          "Allowed sort fields: firstName, lastName, registrationNumber. " +
                          "Returns HATEOAS links for navigation including pagination links (first, last, next, prev)."
    )
    @ApiResponse(responseCode = "200",description = "Paginated list of members retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<MemberSummaryResponse>>> listMembers(
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) @ParameterObject Pageable pageable) {

        // Validate sort fields
        validateSortFields(pageable.getSort());

        // Fetch page from repository directly
        Page<Member> memberPage = memberRepository.findAll(pageable);

        // Map to DTOs and convert to PagedModel using assembler
        PagedModel<EntityModel<MemberSummaryResponse>> pagedModel = pagedResourcesAssembler.toModel(
                memberPage.map(this::toSummaryResponse),
                response -> {
                    EntityModel<MemberSummaryResponse> model = EntityModel.of(response);
                    // Add self link to individual member
                    model.add(linkTo(methodOn(MemberController.class).getMember(response.id())).withSelfRel());
                    return model;
                }
        );

        pagedModel.mapLink(IanaLinkRelations.SELF,
                oldLink -> linkTo(methodOn(MemberController.class).listMembers(pageable)).withSelfRel()
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

        // Load member directly from repository
        Member member = memberRepository.findById(new UserId(id))
                .orElseThrow(() -> new MemberNotFoundException(id));

        // Map to response
        MemberDetailsResponse response = mapToDetailsResponse(member);

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
     * Maps Member domain object to MemberDetailsResponse.
     *
     * @param member the member domain object
     * @return the member details response
     */
    private MemberDetailsResponse mapToDetailsResponse(Member member) {
        // Map address, guardian, email and phone using null-safe methods
        AddressResponse addressResponse = AddressResponse.from(member.getAddress());
        GuardianDTO guardianDTO = GuardianDTO.from(member.getGuardian());
        String email = member.getEmail().value();
        String phone = member.getPhone().value();

        // Map optional fields
        String chipNumber = member.getChipNumber();
        IdentityCardDto identityCardDto = mapToIdentityCardDto(member.getIdentityCard());
        MedicalCourseDto medicalCourseDto = mapToMedicalCourseDto(member.getMedicalCourse());
        TrainerLicenseDto trainerLicenseDto = mapToTrainerLicenseDto(member.getTrainerLicense());
        DrivingLicenseGroup drivingLicenseGroup = member.getDrivingLicenseGroup();
        String dietaryRestrictions = member.getDietaryRestrictions();

        return new MemberDetailsResponse(
                member.getId().uuid(),
                member.getRegistrationNumber().getValue(),
                member.getFirstName(),
                member.getLastName(),
                member.getDateOfBirth(),
                member.getNationality(),
                member.getGender(),
                email,
                phone,
                addressResponse,
                guardianDTO,
                member.isActive(),
                chipNumber,
                identityCardDto,
                medicalCourseDto,
                trainerLicenseDto,
                drivingLicenseGroup,
                dietaryRestrictions
        );
    }

    /**
     * Maps Member domain object to MemberSummaryResponse.
     *
     * @param member the member domain object
     * @return the member summary response
     */
    private MemberSummaryResponse toSummaryResponse(Member member) {
        return new MemberSummaryResponse(
                member.getId().uuid(),
                member.getFirstName(),
                member.getLastName(),
                member.getRegistrationNumber().getValue()
        );
    }

    /**
     * Maps IdentityCard domain object to IdentityCardDto.
     *
     * @param identityCard the identity card to map
     * @return the identity card DTO, or null if identityCard is null
     */
    private IdentityCardDto mapToIdentityCardDto(IdentityCard identityCard) {
        if (identityCard == null) {
            return null;
        }
        return new IdentityCardDto(
                identityCard.cardNumber(),
                identityCard.validityDate()
        );
    }

    /**
     * Maps MedicalCourse domain object to MedicalCourseDto.
     *
     * @param medicalCourse the medical course to map
     * @return the medical course DTO, or null if medicalCourse is null
     */
    private MedicalCourseDto mapToMedicalCourseDto(MedicalCourse medicalCourse) {
        if (medicalCourse == null) {
            return null;
        }
        return new MedicalCourseDto(
                medicalCourse.completionDate(),
                medicalCourse.validityDate()
        );
    }

    /**
     * Maps TrainerLicense domain object to TrainerLicenseDto.
     *
     * @param trainerLicense the trainer license to map
     * @return the trainer license DTO, or null if trainerLicense is null
     */
    private TrainerLicenseDto mapToTrainerLicenseDto(TrainerLicense trainerLicense) {
        if (trainerLicense == null) {
            return null;
        }
        return new TrainerLicenseDto(
                trainerLicense.licenseNumber(),
                trainerLicense.validityDate()
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