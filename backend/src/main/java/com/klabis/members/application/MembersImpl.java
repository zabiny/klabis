package com.klabis.members.application;

import com.klabis.members.MemberAccommodationDto;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import org.jmolecules.ddd.annotation.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class MembersImpl implements Members {

    private final MemberRepository memberRepository;

    MembersImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<MemberDto> findById(MemberId memberId) {
        return memberRepository.findById(memberId).map(this::fromMember);
    }

    @Override
    public Map<MemberId, MemberDto> findByIds(Collection<MemberId> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllByIds(memberIds).stream()
                .collect(Collectors.toMap(
                        member -> new MemberId(member.getId().uuid()),
                        this::fromMember));
    }

    @Override
    public Map<MemberId, MemberAccommodationDto> findAccommodationDataByIds(Collection<MemberId> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllByIds(memberIds).stream()
                .collect(Collectors.toMap(
                        member -> new MemberId(member.getId().uuid()),
                        this::fromMemberToAccommodationDto));
    }

    @Override
    public Optional<MemberDto> findByRegistrationNumber(String registrationNumber) {
        if (RegistrationNumber.isRegistrationNumber(registrationNumber)) {
            return memberRepository.findByRegistrationNumber(RegistrationNumber.of(registrationNumber))
                    .map(this::fromMember);
        } else {
            return Optional.empty();
        }
    }

    private MemberDto fromMember(Member member) {
        return new MemberDto(member.getId().uuid(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail() != null ? member.getEmail().value() : null,
                member.getRegistrationNumber() != null ? member.getRegistrationNumber().getValue() : null,
                LocalDateTime.ofInstant(member.getLastModifiedAt(), ZoneId.of("Europe/Prague")));
    }

    private MemberAccommodationDto fromMemberToAccommodationDto(Member member) {
        String identityCardNumber = member.getIdentityCard() != null ? member.getIdentityCard().cardNumber() : null;
        java.time.LocalDate identityCardValidityDate = member.getIdentityCard() != null ? member.getIdentityCard().validityDate() : null;
        String addressStreet = member.getAddress() != null ? member.getAddress().street() : null;
        String addressCity = member.getAddress() != null ? member.getAddress().city() : null;
        String addressPostalCode = member.getAddress() != null ? member.getAddress().postalCode() : null;
        String addressCountry = member.getAddress() != null ? member.getAddress().country() : null;
        return new MemberAccommodationDto(
                member.getFirstName(),
                member.getLastName(),
                identityCardNumber,
                identityCardValidityDate,
                member.getDateOfBirth(),
                addressStreet,
                addressCity,
                addressPostalCode,
                addressCountry
        );
    }
}
