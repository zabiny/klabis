package com.klabis.members.management;

import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.users.UserId;
import org.jmolecules.ddd.annotation.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
class MembersImpl implements Members {

    private final MemberRepository memberRepository;

    MembersImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<MemberDto> findByUserId(UserId memberId) {
        return memberRepository.findById(memberId).map(this::fromMember);
    }

    @Override
    public Optional<MemberDto> findByRegistrationNumber(String registrationNumber) {
        if (RegistrationNumber.isRegistrationNumber(registrationNumber)) {
            return memberRepository.findByRegistrationNumber(RegistrationNumber.of(registrationNumber)).map(this::fromMember);
        } else {
            return Optional.empty();
        }
    }

    private MemberDto fromMember(Member member) {
        return new MemberDto(member.getFirstName(), member.getLastName(), member.getEmail().value(), LocalDateTime.ofInstant(member.getLastModifiedAt(), ZoneId.of("Europe/Prague")));
    }
}
