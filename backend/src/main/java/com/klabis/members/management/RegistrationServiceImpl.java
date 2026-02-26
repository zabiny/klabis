package com.klabis.members.management;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final RegistrationNumberGenerator registrationNumberGenerator;

    /**
     * Constructs a new RegistrationService.
     *
     * @param memberRepository            the member repository for persisting members
     * @param userService                 the user service for creating users and permissions
     * @param registrationNumberGenerator the generator for registration numbers
     */
    public RegistrationServiceImpl(
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

        UserId sharedId = userService.createUser(
                registrationNumber.getValue(),
                command.email().value(),
                Authority.getStandardUserAuthorities()
        );

        log.debug("User created with shared ID: {} for username: {}",
                sharedId, registrationNumber.getValue());

        Member.RegisterMember domainCommand = new Member.RegisterMember(
                sharedId,
                registrationNumber,
                command.personalInformation(),
                command.address(),
                command.email(),
                command.phone(),
                command.guardian(),
                command.birthNumber(),
                command.bankAccountNumber()
        );

        Member member = Member.register(domainCommand);

        Member savedMember = memberRepository.save(member);

        log.debug("Member created with shared ID: {}", savedMember.getId());

        return savedMember;
    }
}
