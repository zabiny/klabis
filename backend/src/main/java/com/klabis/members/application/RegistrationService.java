package com.klabis.members.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.application.PermissionService;
import com.klabis.members.MemberId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.GuardiansNotAllowedException;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.members.domain.RegistrationNumberAlreadyInUseException;
import com.klabis.members.domain.RegistrationNumberGenerator;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Set;

/**
 * Service for member registration operations.
 *
 * <p>Handles the complete member registration process, including:
 * <ul>
 *   <li>Generating registration numbers</li>
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
public class RegistrationService implements RegistrationPort {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final RegistrationNumberGenerator registrationNumberGenerator;
    private final LegalGuardianPort legalGuardianPort;
    private final LegalGuardianGroupPort legalGuardianGroupPort;
    private final MemberCompletenessPort memberCompletenessPort;
    private final PermissionService permissionService;

    /**
     * Constructs a new RegistrationPort.
     *
     * @param memberRepository            the member repository for persisting members
     * @param userService                 the user service for creating users and permissions
     * @param registrationNumberGenerator the generator for registration numbers
     * @param legalGuardianPort           creates new non-member legal guardians and releases a taken-over one
     * @param legalGuardianGroupPort      assigns the legal guardians to the registered minor
     * @param memberCompletenessPort      tells what the legal guardians offer towards the minor's contacts
     * @param permissionService           grants a taken-over guardian the authorities of a member
     */
    public RegistrationService(
            MemberRepository memberRepository,
            UserService userService,
            RegistrationNumberGenerator registrationNumberGenerator,
            LegalGuardianPort legalGuardianPort,
            LegalGuardianGroupPort legalGuardianGroupPort,
            MemberCompletenessPort memberCompletenessPort,
            PermissionService permissionService) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.registrationNumberGenerator = registrationNumberGenerator;
        this.legalGuardianPort = legalGuardianPort;
        this.legalGuardianGroupPort = legalGuardianGroupPort;
        this.memberCompletenessPort = memberCompletenessPort;
        this.permissionService = permissionService;
    }

    @Transactional
    @Override
    public Member registerMember(RegisterNewMember command) {
        Assert.notNull(command.personalInformation(), "Personal information must not be null");
        Assert.notNull(command.personalInformation().getDateOfBirth(), "Date of birth must not be null");

        LocalDate dateOfBirth = command.personalInformation().getDateOfBirth();
        boolean minor = command.personalInformation().isMinor();

        if (minor && command.takenOverLegalGuardian() != null) {
            throw GuardiansNotAllowedException.takeOverByMinor();
        }
        if (!minor && !command.legalGuardians().isEmpty()) {
            throw GuardiansNotAllowedException.adultWithGuardians();
        }

        Set<UserId> guardians = command.legalGuardians().isEmpty()
                                ? Set.of()
                                : legalGuardianPort.resolveGuardians(command.legalGuardians());
        GuardianContacts guardianContacts = guardians.isEmpty()
                                            ? GuardianContacts.NONE
                                            : memberCompletenessPort.contactsOfChosenGuardians(guardians);

        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(dateOfBirth);
        log.debug("Generated registration number: {} for date of birth: {}",
                registrationNumber.getValue(), dateOfBirth);

        Member member = register(command, registrationNumber, guardianContacts, false);

        if (!guardians.isEmpty()) {
            legalGuardianGroupPort.setGuardiansOf(member.getId(), guardians);
        }
        return member;
    }

    @Transactional
    @Override
    public Member importMember(ImportMember command) {
        Assert.notNull(command.registrationNumber(), "Registration number must not be null");

        return register(command.details(), command.registrationNumber(), GuardianContacts.NONE, true);
    }

    /**
     * Single path both entry points converge onto once the registration number is decided -
     * generated for a hand registration, adopted from ORIS for an import. Everything below this
     * point (user creation and the shared-id invariant) is identical for both; only the domain
     * factory differs — {@code importing} routes to {@link Member#importFromOris}, which tolerates
     * incomplete data, instead of {@link Member#register}, which does not (design.md D5).
     */
    private Member register(RegisterNewMember command, RegistrationNumber registrationNumber,
                            GuardianContacts guardianContacts, boolean importing) {
        try {
            UserId sharedUserId = command.takenOverLegalGuardian() != null
                                  ? takeOverLegalGuardian(command.takenOverLegalGuardian())
                                  : userService.createUser(
                    registrationNumber.getValue(),
                    Authority.getStandardUserAuthorities()
            );

            log.debug("User created with shared ID: {} for username: {}",
                    sharedUserId, registrationNumber.getValue());

            Member.RegisterMember domainCommand = new Member.RegisterMember(
                    MemberId.fromUserId(sharedUserId),
                    registrationNumber,
                    command.personalInformation(),
                    command.address(),
                    command.email(),
                    command.phone(),
                    command.birthNumber(),
                    command.bankAccountNumber(),
                    command.registeredBy()
            );

            Member member = importing ? Member.importFromOris(domainCommand)
                                                   : Member.register(domainCommand, guardianContacts);

            Member savedMember = memberRepository.save(member);

            log.debug("Member created with shared ID: {}", savedMember.getId());

            return savedMember;
        } catch (DataIntegrityViolationException e) {
            // The UNIQUE constraint on members.registration_number (and, since the registration
            // number doubles as the username, on common.users.user_name) is what enforces
            // "already in use" - no existence check is made beforehand, since that would just be
            // a race between the check and this save. See design.md D4.
            throw new RegistrationNumberAlreadyInUseException(registrationNumber, e);
        }
    }

    /**
     * The guardian's user becomes the member's user, so the person keeps their login number and their
     * legal guardian groups; only the guardian profile is replaced by the member profile.
     */
    private UserId takeOverLegalGuardian(UserId guardianId) {
        legalGuardianPort.releaseForMembership(guardianId);
        permissionService.updateUserPermissions(guardianId, Authority.getStandardUserAuthorities());
        return guardianId;
    }
}
