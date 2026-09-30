package com.klabis.members.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberActivationContactVerifier tests")
class MemberActivationContactVerifierTest {

    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1990, 1, 1);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LegalGuardianRepository legalGuardianRepository;

    private MemberActivationContactVerifier verifier;

    private final UserId userId = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        verifier = new MemberActivationContactVerifier(memberRepository, legalGuardianRepository);
    }

    private void givenMember(Member member) {
        when(memberRepository.findById(MemberId.fromUserId(userId))).thenReturn(Optional.of(member));
    }

    private Member adultMember(String email) {
        return MemberTestDataBuilder.aMemberWithId(userId.uuid())
                .withDateOfBirth(ADULT_BIRTH_DATE)
                .withEmail(email)
                .build();
    }

    @Test
    @DisplayName("matches an adult member's own e-mail")
    void matchesAdultMembersOwnEmail() {
        givenMember(adultMember("member@example.com"));

        assertThat(verifier.isActivationContact(userId, "member@example.com")).isTrue();
    }

    @Test
    @DisplayName("matches trimmed and case-insensitively")
    void matchesTrimmedAndCaseInsensitively() {
        givenMember(adultMember("member@example.com"));

        assertThat(verifier.isActivationContact(userId, "  Member@Example.com  ")).isTrue();
    }

    @Test
    @DisplayName("rejects any other address of an adult member")
    void rejectsOtherAddress() {
        givenMember(adultMember("member@example.com"));

        assertThat(verifier.isActivationContact(userId, "attacker@example.com")).isFalse();
    }

    @Test
    @DisplayName("never accepts a minor, not even with their own e-mail")
    void rejectsMinor() {
        Member minor = MemberTestDataBuilder.aMemberWithId(userId.uuid())
                .withDateOfBirth(LocalDate.now().minusYears(12))
                .withEmail("child@example.com")
                .build();
        givenMember(minor);

        assertThat(verifier.isActivationContact(userId, "child@example.com")).isFalse();
    }

    @Test
    @DisplayName("matches a non-member legal guardian's e-mail")
    void matchesLegalGuardiansEmail() {
        when(memberRepository.findById(MemberId.fromUserId(userId))).thenReturn(Optional.empty());
        when(legalGuardianRepository.findById(userId)).thenReturn(Optional.of(LegalGuardian.create(
                new LegalGuardian.CreateLegalGuardian(userId, "Jan", "Novák", "jan@example.com", "+420 777 123 456"))));

        assertThat(verifier.isActivationContact(userId, "Jan@example.com")).isTrue();
    }

    @Test
    @DisplayName("rejects a foreign address of a legal guardian")
    void rejectsForeignAddressOfLegalGuardian() {
        when(memberRepository.findById(MemberId.fromUserId(userId))).thenReturn(Optional.empty());
        when(legalGuardianRepository.findById(userId)).thenReturn(Optional.of(LegalGuardian.create(
                new LegalGuardian.CreateLegalGuardian(userId, "Jan", "Novák", "jan@example.com", "+420 777 123 456"))));

        assertThat(verifier.isActivationContact(userId, "other@example.com")).isFalse();
    }

    @Test
    @DisplayName("returns false for a user who is neither a member nor a legal guardian")
    void returnsFalseForUnknownUser() {
        when(memberRepository.findById(MemberId.fromUserId(userId))).thenReturn(Optional.empty());
        when(legalGuardianRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(verifier.isActivationContact(userId, "someone@example.com")).isFalse();
    }

    @Test
    @DisplayName("returns false for a null e-mail")
    void returnsFalseForNullEmail() {
        assertThat(verifier.isActivationContact(userId, null)).isFalse();
    }
}
