package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@DisplayName("MemberAndLegalGuardianContactResolver")
@ExtendWith(MockitoExtension.class)
class GuardianContactResolverTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MINOR_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UserId GUARDIAN_ID = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final UserId STRANGER = new UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private LegalGuardianRepository legalGuardianRepository;

    private GuardianContactResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MemberAndLegalGuardianContactResolver(memberRepository, legalGuardianRepository);
        lenient().when(memberRepository.findAllByIds(any())).thenAnswer(inv -> {
            Collection<MemberId> ids = inv.getArgument(0);
            return List.of(
                            aMemberWithId(MEMBER_UUID).withName("Jan", "Novák").withEmail("jan@example.com")
                                    .withPhone("+420 777 000 111").withDateOfBirth(LocalDate.now().minusYears(40)).build(),
                            aMemberWithId(MINOR_UUID).withName("Kuba", "Novák")
                                    .withDateOfBirth(LocalDate.now().minusYears(10)).build())
                    .stream().filter(m -> ids.contains(m.getId())).toList();
        });
        lenient().when(legalGuardianRepository.findAllByIds(any())).thenAnswer(inv -> {
            Collection<UserId> ids = inv.getArgument(0);
            return List.of(LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(GUARDIAN_ID, "Eva", "Svobodová",
                    "eva@example.com", "+420 601 000 000"))).stream().filter(g -> ids.contains(g.getId())).toList();
        });
    }

    @Test
    @DisplayName("takes a member's contact from the member")
    void memberContactComesFromMember() {
        List<GuardianContact> contacts = resolver.resolve(List.of(new UserId(MEMBER_UUID)));

        assertThat(contacts).singleElement().satisfies(contact -> {
            assertThat(contact.kind()).isEqualTo(GuardianKind.MEMBER);
            assertThat(contact.lastName()).isEqualTo("Novák");
            assertThat(contact.email()).isEqualTo("jan@example.com");
            assertThat(contact.phone()).isEqualTo("+420 777 000 111");
        });
    }

    @Test
    @DisplayName("takes a non-member's contact from the legal guardian")
    void nonMemberContactComesFromLegalGuardian() {
        List<GuardianContact> contacts = resolver.resolve(List.of(GUARDIAN_ID));

        assertThat(contacts).singleElement().satisfies(contact -> {
            assertThat(contact.kind()).isEqualTo(GuardianKind.LEGAL_GUARDIAN);
            assertThat(contact.lastName()).isEqualTo("Svobodová");
            assertThat(contact.email()).isEqualTo("eva@example.com");
        });
    }

    @Test
    @DisplayName("omits minors and unknown users")
    void omitsMinorsAndUnknownUsers() {
        List<GuardianContact> contacts = resolver.resolve(List.of(new UserId(MINOR_UUID), STRANGER, GUARDIAN_ID));

        assertThat(contacts).extracting(GuardianContact::userId).containsExactly(GUARDIAN_ID);
    }

    @Test
    @DisplayName("resolves a mix of a member and a non-member")
    void resolvesMix() {
        List<GuardianContact> contacts = resolver.resolve(List.of(new UserId(MEMBER_UUID), GUARDIAN_ID));

        assertThat(contacts).extracting(GuardianContact::kind)
                .containsExactlyInAnyOrder(GuardianKind.MEMBER, GuardianKind.LEGAL_GUARDIAN);
    }
}
