package com.klabis.members.infrastructure;

import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.GuardianInformation;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.domain.RegistrationNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberActivationContactVerifier tests")
class MemberActivationContactVerifierTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberActivationContactVerifier verifier;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        verifier = new MemberActivationContactVerifier(memberRepository);
    }

    @Test
    @DisplayName("matches the member's own e-mail")
    void matchesMembersOwnEmail() {
        Member member = MemberTestDataBuilder.aMember()
                .withRegistrationNumber("ZBM0101")
                .withEmail("member@example.com")
                .build();
        when(memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM0101")))
                .thenReturn(Optional.of(member));

        boolean result = verifier.isActivationContact("ZBM0101", "member@example.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("matches the guardian's e-mail")
    void matchesGuardianEmail() {
        GuardianInformation guardian = new GuardianInformation("Petr", "Novák", "Father",
                EmailAddress.of("guardian@example.com"),
                PhoneNumber.of("+420 987 654 321"));
        Member member = MemberTestDataBuilder.aMember()
                .withRegistrationNumber("ZBM0102")
                .withGuardian(guardian)
                .build();
        when(memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM0102")))
                .thenReturn(Optional.of(member));

        boolean result = verifier.isActivationContact("ZBM0102", "guardian@example.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("rejects any other address")
    void rejectsOtherAddress() {
        Member member = MemberTestDataBuilder.aMember()
                .withRegistrationNumber("ZBM0103")
                .withEmail("member@example.com")
                .build();
        when(memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM0103")))
                .thenReturn(Optional.of(member));

        boolean result = verifier.isActivationContact("ZBM0103", "attacker@example.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("returns false for an unknown registration number")
    void returnsFalseForUnknownRegistrationNumber() {
        when(memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM9999")))
                .thenReturn(Optional.empty());

        boolean result = verifier.isActivationContact("ZBM9999", "someone@example.com");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("matches trimmed and case-insensitively")
    void matchesTrimmedAndCaseInsensitively() {
        Member member = MemberTestDataBuilder.aMember()
                .withRegistrationNumber("ZBM0104")
                .withEmail("member@example.com")
                .build();
        when(memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM0104")))
                .thenReturn(Optional.of(member));

        boolean result = verifier.isActivationContact("ZBM0104", "  Member@Example.com  ");

        assertThat(result).isTrue();
    }
}
