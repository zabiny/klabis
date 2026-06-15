package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
class AllMembersService implements AllMembersPort {

    private final MemberRepository memberRepository;

    AllMembersService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Set<MemberId> findAll() {
        return memberRepository.findAll().stream()
                .map(com.klabis.members.domain.Member::getId)
                .collect(Collectors.toSet());
    }
}
