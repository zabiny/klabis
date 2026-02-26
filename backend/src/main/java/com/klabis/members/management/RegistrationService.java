package com.klabis.members.management;

import com.klabis.members.domain.*;

import java.time.LocalDate;

/**
 * Service for member registration operations.
 */
@org.jmolecules.architecture.hexagonal.PrimaryPort
public interface RegistrationService {

    /**
     * Service-level command for registering a new member.
     * <p>
     * This command contains only the information that comes from the controller/API layer.
     * The service layer is responsible for generating the registration number and user ID.
     * <p>
     * The generated values (userId, registrationNumber) are added internally when creating
     * the domain-level {@link Member.RegisterMember} command.
     */
    record RegisterNewMember(
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber
    ) {}

    /**
     * Registers a new member.
     * <p>
     * Creates both Member and User aggregates in an atomic transaction.
     * <b>Critical:</b> User is created FIRST to obtain the shared UserId,
     * then Member is created using that same ID. This ensures Member ID = User ID.
     * <p>
     * The service layer generates:
     * <ul>
     *   <li>registration number - using {@link com.klabis.members.domain.RegistrationNumberGenerator}</li>
     *   <li>user ID - using {@link com.klabis.common.users.UserService#createUser}</li>
     * </ul>
     * <p>
     * The MemberCreatedEvent will be published after commit, triggering
     * the password setup email to be sent asynchronously.
     *
     * @param command the registration command containing member details (without ID and registration number)
     * @return the newly created Member aggregate
     * @throws IllegalArgumentException if any required field is invalid
     * @throws IllegalStateException    if Member ID != User ID after creation (invariant violation)
     */
    @org.springframework.transaction.annotation.Transactional
    Member registerMember(RegisterNewMember command);
}
