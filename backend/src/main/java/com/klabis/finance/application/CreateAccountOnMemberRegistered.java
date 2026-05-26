package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.members.MemberCreatedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@PrimaryAdapter
@Component
public class CreateAccountOnMemberRegistered {

    private final MemberAccountRepository memberAccountRepository;

    public CreateAccountOnMemberRegistered(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @ApplicationModuleListener
    public void on(MemberCreatedEvent event) {
        MemberAccount account = MemberAccount.openFor(event.memberId());
        memberAccountRepository.save(account);
    }
}
