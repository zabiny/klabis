package com.klabis.members.application;

import com.klabis.common.users.UserService;
import com.klabis.common.users.application.PasswordSetupService;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.GeneratedTokenResult;
import com.klabis.common.users.domain.User;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MemberAccountActivationService implements MemberAccountActivationPort {

    private final MemberRepository memberRepository;
    private final UserService userService;
    private final PasswordSetupService passwordSetupService;

    MemberAccountActivationService(MemberRepository memberRepository, UserService userService,
                                   PasswordSetupService passwordSetupService) {
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.passwordSetupService = passwordSetupService;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isAvailableFor(Member member) {
        return member.getPersonalInformation().isMinor()
               && member.getEmail() != null
               && awaitingActivation(member);
    }

    @Transactional
    @Override
    public void sendAccountActivation(MemberId memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!isAvailableFor(member)) {
            throw new AccountActivationNotAvailableException(memberId);
        }

        User user = userService.findUserById(member.getUserId()).orElseThrow();
        GeneratedTokenResult token = passwordSetupService.generateToken(user);
        passwordSetupService.sendPasswordSetupEmail(member.getFirstName(), member.getEmail().value(), token.plainToken());
    }

    private boolean awaitingActivation(Member member) {
        return userService.findUserById(member.getUserId())
                .map(user -> user.getAccountStatus() == AccountStatus.PENDING_ACTIVATION)
                .orElse(false);
    }
}
