package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.application.GuardianResolver;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SecondaryAdapter
@Component
class MemberGuardianResolver implements GuardianResolver {

    private static final int ADULT_AGE = 18;

    private final MemberRepository memberRepository;

    MemberGuardianResolver(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Set<Guardian> resolve(Set<UserId> userIds) {
        List<MemberId> memberIds = userIds.stream().map(MemberId::fromUserId).toList();
        Map<MemberId, Member> members = memberRepository.findAllByIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        LocalDate today = LocalDate.now();

        return userIds.stream().map(userId -> {
            Member member = members.get(MemberId.fromUserId(userId));
            if (member == null || Period.between(member.getDateOfBirth(), today).getYears() < ADULT_AGE) {
                throw new GuardianNotFoundException(userId);
            }
            return new Guardian(userId, member.getLastName());
        }).collect(Collectors.toUnmodifiableSet());
    }
}
