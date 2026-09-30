package com.klabis.members.legalguardian.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class LegalGuardianService implements LegalGuardianPort {

    private final LegalGuardianRepository legalGuardianRepository;
    private final MemberRepository memberRepository;
    private final UserService userService;
    private final LoginNumberSequence loginNumberSequence;

    LegalGuardianService(LegalGuardianRepository legalGuardianRepository,
                         MemberRepository memberRepository,
                         UserService userService,
                         LoginNumberSequence loginNumberSequence) {
        this.legalGuardianRepository = legalGuardianRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.loginNumberSequence = loginNumberSequence;
    }

    @Transactional
    @Override
    public LegalGuardianProfile register(NewLegalGuardian command) {
        String email = EmailAddress.of(command.email()).value();
        requireEmailNotInUse(email, null);

        String loginName = loginNumberSequence.next();
        UserId userId = userService.createUser(loginName, Set.of(Authority.MEMBERS_READ));

        LegalGuardian guardian = legalGuardianRepository.save(LegalGuardian.create(
                new LegalGuardian.CreateLegalGuardian(userId, command.firstName(), command.lastName(),
                        email, command.phone())));
        return new LegalGuardianProfile(guardian, loginName);
    }

    @Transactional
    @Override
    public Set<UserId> resolveGuardians(List<GuardianInput> inputs) {
        return inputs.stream()
                .map(input -> input.userId() != null ? input.userId() : register(input.newGuardian()).guardian().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    @Override
    public LegalGuardianProfile get(UserId id) {
        return profileOf(load(id));
    }

    @Transactional
    @Override
    public LegalGuardianProfile update(UserId id, LegalGuardian.UpdateLegalGuardian command) {
        LegalGuardian guardian = load(id);
        if (command.email() != null && !guardian.getEmail().equals(EmailAddress.of(command.email()))) {
            requireEmailNotInUse(EmailAddress.of(command.email()).value(), id);
        }
        guardian.update(command);
        return profileOf(legalGuardianRepository.save(guardian));
    }

    private void requireEmailNotInUse(String email, UserId self) {
        boolean usedByGuardian = legalGuardianRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(self))
                .isPresent();
        boolean usedByAdultMember = memberRepository.findAllByEmail(email).stream()
                .anyMatch(member -> !member.getPersonalInformation().isMinor());
        if (usedByGuardian || usedByAdultMember) {
            throw new LegalGuardianEmailAlreadyInUseException(email);
        }
    }

    private LegalGuardian load(UserId id) {
        return legalGuardianRepository.findById(id).orElseThrow(() -> new LegalGuardianNotFoundException(id));
    }

    private LegalGuardianProfile profileOf(LegalGuardian guardian) {
        String loginName = userService.findUserById(guardian.getId())
                .map(user -> user.getUsername())
                .orElseThrow(() -> new LegalGuardianNotFoundException(guardian.getId()));
        return new LegalGuardianProfile(guardian, loginName);
    }
}
