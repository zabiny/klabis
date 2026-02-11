package com.klabis.members.management;

import com.klabis.members.*;
import com.klabis.members.persistence.MemberRepository;
import com.klabis.users.Authority;
import com.klabis.users.UserCreationParams;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service for member registration operations.
 *
 * <p>Handles the complete member registration process, including:
 * <ul>
 *   <li>Generating unique registration numbers</li>
 *   <li>Creating both Member and User aggregates in an atomic transaction</li>
 *   <li>Ensuring Member ID = User ID for all members</li>
 * </ul>
 *
 * <p><b>Transaction Boundary:</b> The registration process creates both User and Member
 * aggregates in a single transaction. The User is created FIRST to obtain the shared UserId,
 * then the Member is created using that same ID. This ensures referential integrity.
 *
 * <p><b>Event Publishing:</b> After transaction commit, a MemberCreatedEvent will be published,
 * triggering the password setup email to be sent asynchronously by the members module.
 */
@Service
@PrimaryPort
class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final RegistrationNumberGenerator registrationNumberGenerator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new RegistrationService.
     *
     * @param memberRepository the member repository for persisting members
     * @param userService      the user service for creating users and permissions
     * @param passwordEncoder  the password encoder for hashing passwords
     * @param clubCode         the club code used for registration number generation
     */
    public RegistrationService(
            MemberRepository memberRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            Members members,
            @Value("${klabis.club.code}") String clubCode) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.registrationNumberGenerator = new RegistrationNumberGenerator(clubCode, members);
    }

    /**
     * Registers a new member.
     * <p>
     * Creates both Member and User aggregates in an atomic transaction.
     * <b>Critical:</b> User is created FIRST to obtain the shared UserId,
     * then Member is created using that same ID. This ensures Member ID = User ID.
     * <p>
     * The MemberCreatedEvent will be published after commit, triggering
     * the password setup email to be sent asynchronously.
     *
     * @param request the registration request containing member details
     * @return the shared ID of the newly created member and user (same ID)
     * @throws IllegalArgumentException if any required field is invalid
     * @throws IllegalStateException    if Member ID != User ID after creation (invariant violation)
     */
    @Transactional
    public UUID registerMember(RegisterMemberRequest request) {
        // Generate unique registration number
        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(request.dateOfBirth());

        // Create Address value object
        Address address = createAddress(request.address());

        // Create EmailAddress and PhoneNumber value objects
        EmailAddress email = createEmailAddress(request.email());
        PhoneNumber phone = createPhoneNumber(request.phone());

        // Convert guardian DTO to domain object if present
        GuardianInformation guardian = null;
        if (request.guardian() != null) {
            GuardianDTO dto = request.guardian();

            // Create EmailAddress and PhoneNumber value objects for guardian
            EmailAddress guardianEmail = createEmailAddress(dto.email());
            PhoneNumber guardianPhone = createPhoneNumber(dto.phone());

            guardian = new GuardianInformation(
                    dto.firstName(),
                    dto.lastName(),
                    dto.relationship(),
                    guardianEmail,
                    guardianPhone
            );
        }

        // Create PersonalInformation value object
        PersonalInformation personalInformation = PersonalInformation.of(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.nationality(),
                request.gender()
        );

        // CRITICAL: Create User FIRST to obtain the shared UserId
        // This ensures Member ID = User ID for all members
        String placeholderPassword = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(placeholderPassword);

        // Create user with pending activation status and default authority
        // Pass email to UserCreationParams for cross-module password setup coordination
        // UserService handles User + UserPermissions creation in a single transaction
        UserCreationParams params = UserCreationParams.builder()
                .username(registrationNumber.getValue())
                .passwordHash(passwordHash)
                .authorities(Set.of(Authority.MEMBERS_READ))  // Default authority for new members
                .email(email.value())  // PII from Member request for password setup
                .build();

        UserId sharedId = userService.createUserPendingActivation(params);

        // Critical assertion: User ID must never be null after creation
        assert sharedId != null : "User ID must not be null after creation";

        log.debug("User created with shared ID: {} for username: {}",
                sharedId, registrationNumber.getValue());
        log.debug("UserPermissions created for user: {} with authority: MEMBERS_READ",
                sharedId);

        // Create Member using the SAME UserId (shared ID)
        Member member = Member.createWithId(
                sharedId,  // Pass the shared ID from User
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                guardian
        );

        // Persist Member
        Member savedMember = memberRepository.save(member);

        log.debug("Member created with shared ID: {}", savedMember.getId());

        // Verify invariant: Member ID must equal User ID
        if (!savedMember.getId().equals(sharedId)) {
            String errorMsg = String.format(
                    "Critical invariant violation: Member ID (%s) != User ID (%s)",
                    savedMember.getId(), sharedId
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // MemberCreatedEvent will be published after transaction commit
        // MemberCreatedEventHandler will send password setup email asynchronously

        return sharedId.uuid();  // Return the shared ID
    }

    // ========== Helper Methods ==========

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
}
