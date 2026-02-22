package com.klabis.members.management;

import com.klabis.members.domain.*;
import com.klabis.members.infrastructure.restapi.AddressRequest;
import com.klabis.members.infrastructure.restapi.RegisterMemberRequest;
import com.klabis.users.Authority;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

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
class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final RegistrationNumberGenerator registrationNumberGenerator;

    /**
     * Constructs a new RegistrationService.
     *
     * @param memberRepository the member repository for persisting members
     * @param userService      the user service for creating users and permissions
     * @param clubCode         the club code used for registration number generation
     */
    public RegistrationServiceImpl(
            MemberRepository memberRepository,
            UserService userService,
            Members members,
            @Value("${klabis.club.code}") String clubCode) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.registrationNumberGenerator = new RegistrationNumberGenerator(clubCode, members);
    }

    @Transactional
    @Override
    public UUID registerMember(RegisterMemberRequest request) {
        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(request.dateOfBirth());

        Address address = createAddress(request.address());

        EmailAddress email = createEmailAddress(request.email());
        PhoneNumber phone = createPhoneNumber(request.phone());

        GuardianInformation guardian = null;
        if (request.guardian() != null) {
            GuardianDTO dto = request.guardian();
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

        PersonalInformation personalInformation = PersonalInformation.of(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.nationality(),
                request.gender()
        );

        BirthNumber birthNumber = null;
        if (request.birthNumber() != null && !request.birthNumber().isBlank()) {
            birthNumber = BirthNumber.of(request.birthNumber());
        }

        BankAccountNumber bankAccountNumber = null;
        if (request.bankAccountNumber() != null && !request.bankAccountNumber().isBlank()) {
            bankAccountNumber = BankAccountNumber.of(request.bankAccountNumber());
        }

        UserId sharedId = userService.createUser(
                registrationNumber.getValue(),
                email.value(),
                Authority.getStandardUserAuthorities()
        );

        log.debug("User created with shared ID: {} for username: {}",
                sharedId, registrationNumber.getValue());

        Member.RegisterMember command = new Member.RegisterMember(
                sharedId,
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                guardian,
                birthNumber,
                bankAccountNumber
        );
        Member member = Member.register(command);

        Member savedMember = memberRepository.save(member);

        log.debug("Member created with shared ID: {}", savedMember.getId());

        return sharedId.uuid();
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
