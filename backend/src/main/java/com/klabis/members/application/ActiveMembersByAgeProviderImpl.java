package com.klabis.members.application;

import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
class ActiveMembersByAgeProviderImpl implements ActiveMembersByAgeProvider {

    private final MemberRepository memberRepository;

    ActiveMembersByAgeProviderImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public List<MemberId> findActiveMemberIdsByAgeRange(int minAge, int maxAge) {
        LocalDate today = LocalDate.now();
        // TODO: push age range filter to DB query when roster size warrants it
        return memberRepository.findAll(MemberFilter.activeOnly()).stream()
                .filter(member -> member.getDateOfBirth() != null)
                .filter(member -> {
                    int age = Period.between(member.getDateOfBirth(), today).getYears();
                    return age >= minAge && age <= maxAge;
                })
                .map(Member::getId)
                .toList();
    }
}
