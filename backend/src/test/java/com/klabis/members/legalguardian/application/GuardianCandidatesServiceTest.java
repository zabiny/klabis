package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GuardianCandidatesService")
@ExtendWith(MockitoExtension.class)
class GuardianCandidatesServiceTest {

    private static final UUID ADULT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UserId GUARDIAN_ID = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Mock
    private LegalGuardianRepository legalGuardianRepository;
    @Mock
    private MemberRepository memberRepository;

    private GuardianCandidatesService service;

    @BeforeEach
    void setUp() {
        service = new GuardianCandidatesService(legalGuardianRepository, memberRepository);
    }

    private static Member adultMember(UUID id, String first, String last) {
        return aMemberWithId(id).withName(first, last).withEmail("member@example.com")
                .withDateOfBirth(LocalDate.now().minusYears(40)).build();
    }

    private static LegalGuardian nonMember(UserId id, String first, String last, String email) {
        return LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(id, first, last, email, "+420 601 000 000"));
    }

    @Test
    @DisplayName("offers non-member guardians and adult active members, each with what tells namesakes apart")
    void offersBoth() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of(nonMember(GUARDIAN_ID, "Eva", "Svobodová", "eva@example.com")));
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of(adultMember(ADULT_UUID, "Jan", "Novák")));

        List<GuardianCandidate> candidates = service.findCandidates(null, null);

        assertThat(candidates).extracting(GuardianCandidate::displayName).containsExactly("Eva Svobodová", "Jan Novák");
        GuardianCandidate member = candidates.get(1);
        assertThat(member.kind()).isEqualTo(GuardianKind.MEMBER);
        assertThat(member.registrationNumber()).isNotBlank();
        GuardianCandidate guardian = candidates.get(0);
        assertThat(guardian.kind()).isEqualTo(GuardianKind.LEGAL_GUARDIAN);
        assertThat(guardian.email()).isEqualTo("eva@example.com");
        assertThat(guardian.registrationNumber()).isNull();
    }

    @Test
    @DisplayName("asks only for active members")
    void asksOnlyForActiveMembers() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of());
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of());

        service.findCandidates("nov", null);

        ArgumentCaptor<MemberFilter> filter = ArgumentCaptor.forClass(MemberFilter.class);
        verify(memberRepository).findAll(filter.capture());
        assertThat(filter.getValue().status()).isEqualTo(MemberFilter.StatusFilter.ACTIVE);
        assertThat(filter.getValue().fulltextQuery()).isEqualTo("nov");
    }

    @Test
    @DisplayName("leaves out members younger than 18")
    void leavesOutMinors() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of());
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of(
                aMemberWithId(ADULT_UUID).withDateOfBirth(LocalDate.now().minusYears(12)).build()));

        assertThat(service.findCandidates(null, null)).isEmpty();
    }

    @Test
    @DisplayName("filters non-member guardians by name ignoring case and diacritics")
    void filtersGuardiansByName() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of(
                nonMember(GUARDIAN_ID, "Eva", "Svobodová", "eva@example.com"),
                nonMember(new UserId(UUID.randomUUID()), "Petr", "Dvořák", "petr@example.com")));
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of());

        assertThat(service.findCandidates("dvorak", null)).extracting(GuardianCandidate::displayName)
                .containsExactly("Petr Dvořák");
        assertThat(service.findCandidates("eva svob", null)).extracting(GuardianCandidate::displayName)
                .containsExactly("Eva Svobodová");
    }

    @Test
    @DisplayName("offers a person only once when they are both member and guardian")
    void dedupesByUserId() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of(
                nonMember(new UserId(ADULT_UUID), "Jan", "Novák", "jan@example.com")));
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of(adultMember(ADULT_UUID, "Jan", "Novák")));

        assertThat(service.findCandidates(null, null)).singleElement()
                .extracting(GuardianCandidate::kind).isEqualTo(GuardianKind.MEMBER);
    }

    @Test
    @DisplayName("offers only non-member guardians for kind LEGAL_GUARDIAN without asking for members")
    void filtersByLegalGuardianKind() {
        when(legalGuardianRepository.findAll()).thenReturn(List.of(nonMember(GUARDIAN_ID, "Eva", "Svobodová", "eva@example.com")));
        assertThat(service.findCandidates(null, GuardianKind.LEGAL_GUARDIAN)).extracting(GuardianCandidate::displayName)
                .containsExactly("Eva Svobodová");
        verify(memberRepository, org.mockito.Mockito.never()).findAll(any(MemberFilter.class));
    }

    @Test
    @DisplayName("offers only adult members for kind MEMBER")
    void filtersByMemberKind() {
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of(adultMember(ADULT_UUID, "Jan", "Novák")));

        assertThat(service.findCandidates(null, GuardianKind.MEMBER)).extracting(GuardianCandidate::displayName)
                .containsExactly("Jan Novák");
        verify(legalGuardianRepository, org.mockito.Mockito.never()).findAll();
    }
}
