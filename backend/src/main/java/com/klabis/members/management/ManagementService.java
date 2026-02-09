package com.klabis.members.management;

import com.klabis.members.*;
import com.klabis.members.persistence.MemberRepository;
import com.klabis.users.Authority;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for member management operations.
 *
 * <p>Handles member queries and updates, including:
 * <ul>
 *   <li>Retrieving individual member details</li>
 *   <li>Listing members with pagination</li>
 *   <li>Updating member information with role-based access control</li>
 * </ul>
 *
 * <p><b>Authorization Model:</b> Supports dual authorization:
 * <ul>
 *   <li><b>Self-edit:</b> Member editing their own information (registration number matches OAuth2 subject)</li>
 *   <li><b>Admin edit:</b> User with MEMBERS:UPDATE authority editing any member</li>
 * </ul>
 *
 * <p><b>Field Access Control:</b> Non-admin users can only update contact information
 * (email, phone, address). Admin-only fields include personal details and documents.
 */
@Service
@PrimaryPort
class ManagementService {

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);
    private static final String MEMBERS_UPDATE_AUTHORITY = Authority.MEMBERS_UPDATE.getValue();

    private final MemberRepository memberRepository;

    /**
     * Constructs a new ManagementService.
     *
     * @param memberRepository the member repository for querying and persisting members
     */
    public ManagementService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Retrieves a member by their ID.
     *
     * @param memberId the member ID
     * @return DTO containing complete member details
     * @throws MemberNotFoundException if no member exists with the given ID
     */
    @Transactional(readOnly = true)
    public MemberDetailsDTO getMember(UUID memberId) {
        Optional<Member> member = memberRepository.findById(new UserId(memberId));

        if (member.isEmpty()) {
            throw new MemberNotFoundException(memberId);
        }

        return mapToDTO(member.get());
    }

    /**
     * Lists all members with pagination and sorting.
     *
     * @param pageable pagination and sorting parameters
     * @return page of member summaries with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<MemberSummaryDTO> listMembers(Pageable pageable) {
        // Fetch page from repository
        Page<Member> memberPage = memberRepository.findAll(pageable);

        // Map to DTOs
        return memberPage.map(this::toSummaryDTO);
    }

    /**
     * Updates a member's information.
     * <p>
     * Implements dual authorization:
     * <ol>
     *   <li>Determine if user is admin (has MEMBERS:UPDATE)</li>
     *   <li>If not admin, verify self-edit (authenticated registration number matches member)</li>
     *   <li>Filter fields based on role (non-admins cannot update admin-only fields)</li>
     *   <li>Apply updates using Member's update methods</li>
     *   <li>Save updated member to repository</li>
     * </ol>
     *
     * @param memberId the ID of the member to update
     * @param request  the update request
     * @return the ID of the updated member
     * @throws SelfEditNotAllowedException if non-admin tries to edit another member
     * @throws AdminFieldAccessException   if non-admin tries to edit admin-only fields
     * @throws UserIdentificationException if user cannot be identified from authentication
     * @throws InvalidUpdateException      if validation fails
     */
    @Transactional
    public UUID updateMember(UUID memberId, UpdateMemberRequest request) {
        // Validate request has at least one field to update
        if (isEmptyUpdate(request)) {
            throw new InvalidUpdateException("Update request must contain at least one field to update");
        }

        // Load existing member
        Member existingMember = memberRepository.findById(new UserId(memberId))
                .orElseThrow(() -> new InvalidUpdateException(
                        "Member not found with ID: " + memberId
                ));

        // Get authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidUpdateException("User must be authenticated to update member information");
        }

        // Determine user role
        boolean isAdmin = hasAdminPermission(authentication);
        final RegistrationNumber authUserRegistrationId = getAuthenticatedRegistrationNumber(authentication).orElse(null);

        log.debug("Processing member update: memberId={}, isAdmin={}, userId={}",
                memberId, isAdmin, authUserRegistrationId);

        // If not admin, verify self-edit
        if (!isAdmin) {
            if (!canSelfEdit(existingMember, authUserRegistrationId)) {
                if (authUserRegistrationId != null) {
                    throw new SelfEditNotAllowedException(authUserRegistrationId,
                            existingMember.getRegistrationNumber());
                } else {
                    throw new UserIdentificationException("Cannot update member without authenticated user");
                }
            }
            log.debug("Self-edit allowed: updating member {}", existingMember.getRegistrationNumber());
            verifyNoAdminFields(request);
        }

        // Apply updates
        Member updatedMember = applyUpdates(existingMember, request);

        // Save to repository
        Member savedMember = memberRepository.save(updatedMember);

        log.info("Member updated: memberId={}, updatedBy={}",
                savedMember.getId(), authUserRegistrationId);

        return savedMember.getId().uuid();  // Extract UUID from UserId
    }

    // ========== Helper Methods ==========

    /**
     * Checks if the authenticated user has MEMBERS:UPDATE authority.
     *
     * @param authentication the authentication object
     * @return true if user has admin permission, false otherwise
     */
    private boolean hasAdminPermission(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(MEMBERS_UPDATE_AUTHORITY::equals);
    }

    /**
     * Extracts the registration number from the authentication context.
     *
     * @param authentication the authentication object
     * @return Optional containing the registration number if present
     */
    private Optional<RegistrationNumber> getAuthenticatedRegistrationNumber(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!StringUtils.hasText(authentication.getName())) {
            return Optional.empty();
        }

        if (RegistrationNumber.isRegistrationNumber(authentication.getName())) {
            return Optional.of(new RegistrationNumber(authentication.getName()));
        } else {
            // application users without Member have user names which doesn't follow registration number pattern (for example `admin`)
            return Optional.empty();
        }
    }

    /**
     * Verifies that the authenticated user is editing their own information.
     *
     * @param member the member being edited
     * @param auth   the authenticated user's registration number
     * @return true if user can edit themselves, false otherwise
     */
    private boolean canSelfEdit(Member member, RegistrationNumber auth) {
        if (auth == null) {
            return false;
        }
        return auth.equals(member.getRegistrationNumber());
    }

    /**
     * Verifies that the request does not contain any admin-only fields.
     *
     * @param request the update request
     * @throws AdminFieldAccessException if request contains admin-only fields
     */
    private void verifyNoAdminFields(UpdateMemberRequest request) {
        if (request.firstName().isPresent()) {
            throw new AdminFieldAccessException("firstName");
        }
        if (request.lastName().isPresent()) {
            throw new AdminFieldAccessException("lastName");
        }
        if (request.dateOfBirth().isPresent()) {
            throw new AdminFieldAccessException("dateOfBirth");
        }
        if (request.gender().isPresent()) {
            throw new AdminFieldAccessException("gender");
        }
        if (request.chipNumber().isPresent()) {
            throw new AdminFieldAccessException("chipNumber");
        }
        if (request.identityCard().isPresent()) {
            throw new AdminFieldAccessException("identityCard");
        }
        if (request.medicalCourse().isPresent()) {
            throw new AdminFieldAccessException("medicalCourse");
        }
        if (request.trainerLicense().isPresent()) {
            throw new AdminFieldAccessException("trainerLicense");
        }
        if (request.drivingLicenseGroup().isPresent()) {
            throw new AdminFieldAccessException("drivingLicenseGroup");
        }

        log.debug("No admin-only fields in update request");
    }

    /**
     * Checks if update request has no fields to update.
     *
     * @param request the update request
     * @return true if request is empty, false otherwise
     */
    private boolean isEmptyUpdate(UpdateMemberRequest request) {
        return request.email().isEmpty()
               && request.phone().isEmpty()
               && request.address().isEmpty()
               && request.firstName().isEmpty()
               && request.lastName().isEmpty()
               && request.dateOfBirth().isEmpty()
               && request.gender().isEmpty()
               && request.chipNumber().isEmpty()
               && request.identityCard().isEmpty()
               && request.medicalCourse().isEmpty()
               && request.trainerLicense().isEmpty()
               && request.drivingLicenseGroup().isEmpty()
               && request.dietaryRestrictions().isEmpty();
    }

    /**
     * Applies updates to the member using the appropriate update methods.
     *
     * @param existingMember the existing member to update
     * @param request        the update request
     * @return the updated member
     */
    private Member applyUpdates(Member existingMember, UpdateMemberRequest request) {
        Member updatedMember = existingMember;

        // Update contact information (member-editable)
        boolean hasContactUpdates = request.email().isPresent()
                                    || request.phone().isPresent()
                                    || request.address().isPresent();

        if (hasContactUpdates) {
            EmailAddress email = request.email()
                    .map(this::createEmailAddress)
                    .orElse(null);
            PhoneNumber phone = request.phone()
                    .map(this::createPhoneNumber)
                    .orElse(null);
            Address address = request.address()
                    .map(this::createAddress)
                    .orElse(null);

            updatedMember.updateContactInformation(email, phone, address);
            log.debug("Contact information updated");
        }

        // Update documents (admin-only)
        boolean hasDocumentUpdates = request.identityCard().isPresent()
                                     || request.medicalCourse().isPresent()
                                     || request.trainerLicense().isPresent();

        if (hasDocumentUpdates) {
            IdentityCard identityCard = request.identityCard()
                    .map(dto -> IdentityCard.of(dto.cardNumber(), dto.validityDate()))
                    .orElse(null);
            MedicalCourse medicalCourse = request.medicalCourse()
                    .map(dto -> MedicalCourse.of(
                            dto.completionDate(),
                            dto.validityDate()
                    ))
                    .orElse(null);
            TrainerLicense trainerLicense = request.trainerLicense()
                    .map(dto -> TrainerLicense.of(dto.licenseNumber(), dto.validityDate()))
                    .orElse(null);

            updatedMember.updateDocuments(identityCard, medicalCourse, trainerLicense);
            log.debug("Documents updated");
        }

        // Update personal details (admin-only fields + dietary restrictions)
        boolean hasPersonalInformationUpdates = request.firstName().isPresent()
                                                || request.lastName().isPresent()
                                                || request.dateOfBirth().isPresent()
                                                || request.gender().isPresent()
                                                || request.chipNumber().isPresent()
                                                || request.drivingLicenseGroup().isPresent()
                                                || request.dietaryRestrictions().isPresent();

        if (hasPersonalInformationUpdates) {
            // Build new PersonalInformation if any name/DOB/gender fields are provided
            PersonalInformation personalInformation = null;
            if (request.firstName().isPresent() || request.lastName().isPresent()
                || request.dateOfBirth().isPresent() || request.gender().isPresent()) {
                String firstName = request.firstName().orElse(existingMember.getFirstName());
                String lastName = request.lastName().orElse(existingMember.getLastName());
                LocalDate dateOfBirth = request.dateOfBirth().orElse(existingMember.getDateOfBirth());
                String nationalityCode = existingMember.getNationality();
                Gender gender = request.gender().orElse(existingMember.getGender());

                personalInformation = PersonalInformation.of(firstName, lastName, dateOfBirth, nationalityCode, gender);
                log.debug("Personal information updated: firstName={}, lastName={}, dateOfBirth={}, gender={}",
                        firstName, lastName, dateOfBirth, gender);
            }

            updatedMember.updateMemberDetails(
                    personalInformation,
                    null, // address - already handled in contact updates
                    null, // email - already handled in contact updates
                    null, // phone - already handled in contact updates
                    null, // guardian - not supported in current API
                    request.chipNumber().orElse(null),
                    request.drivingLicenseGroup().orElse(null),
                    request.dietaryRestrictions().orElse(null),
                    null // gender - already handled in personalInformation
            );
            log.debug("Personal details updated");
        }

        return updatedMember;
    }

    /**
     * Creates an Address value object from the request data.
     *
     * @param addressRequest the address request data
     * @return the Address value object
     * @throws IllegalArgumentException if address data is invalid
     */
    private Address createAddress(AddressRequest addressRequest) {
        try {
            return Address.of(
                    addressRequest.street(),
                    addressRequest.city(),
                    addressRequest.postalCode(),
                    addressRequest.country()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid address data: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid address: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an EmailAddress value object from the string.
     *
     * @param email the email address string
     * @return the EmailAddress value object
     * @throws IllegalArgumentException if email is invalid
     */
    private EmailAddress createEmailAddress(String email) {
        try {
            return EmailAddress.of(email);
        } catch (IllegalArgumentException e) {
            log.error("Invalid email address: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid email: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a PhoneNumber value object from the string.
     *
     * @param phone the phone number string
     * @return the PhoneNumber value object
     * @throws IllegalArgumentException if phone number is invalid
     */
    private PhoneNumber createPhoneNumber(String phone) {
        try {
            return PhoneNumber.of(phone);
        } catch (IllegalArgumentException e) {
            log.error("Invalid phone number: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid phone: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a Member domain object to MemberDetailsDTO.
     *
     * @param member the member to map
     * @return the member details DTO
     */
    private MemberDetailsDTO mapToDTO(Member member) {
        // Map address, guardian, email and phone using null-safe methods
        AddressResponse addressResponse = AddressResponse.from(member.getAddress());
        GuardianDTO guardianDTO = GuardianDTO.from(member.getGuardian());
        String email = member.getEmail().value();
        String phone = member.getPhone().value();

        // Map optional fields using null-safe methods from MapperHelpers
        String chipNumber = member.getChipNumber();
        IdentityCardDto identityCardDto = mapToIdentityCardDto(member.getIdentityCard());
        MedicalCourseDto medicalCourseDto = mapToMedicalCourseDto(member.getMedicalCourse());
        TrainerLicenseDto trainerLicenseDto = mapToTrainerLicenseDto(member.getTrainerLicense());
        DrivingLicenseGroup drivingLicenseGroup = member.getDrivingLicenseGroup();
        String dietaryRestrictions = member.getDietaryRestrictions();

        return new MemberDetailsDTO(
                member.getId().uuid(),  // Extract UUID from UserId
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
                medicalCourse.validityDate() // Returns Optional<LocalDate>
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

    /**
     * Maps a Member domain object to MemberSummaryDTO.
     *
     * @param member the member to map
     * @return the member summary DTO
     */
    private MemberSummaryDTO toSummaryDTO(Member member) {
        return new MemberSummaryDTO(
                member.getId().uuid(),  // Extract UUID from UserId
                member.getFirstName(),
                member.getLastName(),
                member.getRegistrationNumber().getValue()
        );
    }
}
