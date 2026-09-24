package com.klabis.members.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.members.domain.RegistrationNumberAlreadyInUseException;
import com.klabis.members.domain.RegistrationNumberGenerator;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;

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

    /**
     * Constructs a new RegistrationPort.
     *
     * @param memberRepository            the member repository for persisting members
     * @param userService                 the user service for creating users and permissions
     * @param registrationNumberGenerator the generator for registration numbers
     */
    public RegistrationService(
            MemberRepository memberRepository,
            UserService userService,
            RegistrationNumberGenerator registrationNumberGenerator) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.registrationNumberGenerator = registrationNumberGenerator;
    }

    @Transactional
    @Override
    public Member registerMember(RegisterNewMember command) {
        Assert.notNull(command.personalInformation(), "Personal information must not be null");
        Assert.notNull(command.personalInformation().getDateOfBirth(), "Date of birth must not be null");

        LocalDate dateOfBirth = command.personalInformation().getDateOfBirth();

        RegistrationNumber registrationNumber = registrationNumberGenerator.generate(dateOfBirth);
        log.debug("Generated registration number: {} for date of birth: {}",
                registrationNumber.getValue(), dateOfBirth);

        return register(command, registrationNumber, false);
    }

    @Transactional
    @Override
    public Member importMember(ImportMember command) {
        Assert.notNull(command.registrationNumber(), "Registration number must not be null");

        return register(command.details(), command.registrationNumber(), true);
    }

    /**
     * Single path both entry points converge onto once the registration number is decided -
     * generated for a hand registration, adopted from ORIS for an import. Everything below this
     * point (user creation and the shared-id invariant) is identical for both; only the domain
     * factory differs — {@code importing} routes to {@link Member#importFromOris}, which tolerates
     * incomplete data, instead of {@link Member#register}, which does not (design.md D5).
     */
    private Member register(RegisterNewMember command, RegistrationNumber registrationNumber, boolean importing) {
        try {
            UserId sharedUserId = userService.createUser(
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
                    command.guardian(),
                    command.birthNumber(),
                    command.bankAccountNumber(),
                    command.registeredBy()
            );

            Member member = importing ? Member.importFromOris(domainCommand) : Member.register(domainCommand);

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
}
