package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.domain.*;

/**
 * Service for member registration operations.
 */
@org.jmolecules.architecture.hexagonal.PrimaryPort
public interface RegistrationPort {

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
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            UserId registeredBy
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
     *   <li>user ID - using {@link UserService#createUser}</li>
     * </ul>
     * <p>
     * The MemberCreatedEvent will be published after commit, triggering
     * the password setup email to be sent asynchronously.
     *
     * @param command the registration command containing member details (without ID and registration number)
     * @return the newly created Member aggregate
     * @throws IllegalArgumentException              if any required field is invalid
     * @throws IllegalStateException                 if Member ID != User ID after creation (invariant violation)
     * @throws RegistrationNumberAlreadyInUseException if the generated number collides with one already in use
     */
    @org.springframework.transaction.annotation.Transactional
    Member registerMember(RegisterNewMember command);

    /**
     * Command for importing a member whose registration number was issued elsewhere (e.g. by ORIS).
     * <p>
     * Composes {@link RegisterNewMember} rather than adding an optional field to it, so the two
     * cases - a number issued by the club versus one adopted from elsewhere - stay explicit at
     * the type level.
     *
     * @param details            the ordinary registration details, exactly as for {@link #registerMember}
     * @param registrationNumber the registration number to adopt instead of generating one
     */
    record ImportMember(RegisterNewMember details, RegistrationNumber registrationNumber) {}

    /**
     * Imports a member whose registration number was issued elsewhere, adopting it instead of
     * generating one from the club's own sequence.
     * <p>
     * Converges onto the same registration path as {@link #registerMember} immediately after the
     * registration number is decided, so every consequence of a normal registration - user
     * creation, the shared-id invariant, validation and every published event - follows here too.
     *
     * @param command the import command, carrying both the registration details and the number to adopt
     * @return the newly created Member aggregate
     * @throws IllegalArgumentException              if any required field is invalid
     * @throws IllegalStateException                 if Member ID != User ID after creation (invariant violation)
     * @throws RegistrationNumberAlreadyInUseException if the given number is already carried by another member
     */
    @org.springframework.transaction.annotation.Transactional
    Member importMember(ImportMember command);
}
