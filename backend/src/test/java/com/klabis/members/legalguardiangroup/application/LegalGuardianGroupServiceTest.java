package com.klabis.members.legalguardiangroup.application;

import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardian.application.GuardianKind;
import com.klabis.members.MemberId;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import com.klabis.members.legalguardiangroup.domain.OnlyMinorsAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.klabis.members.MemberTestDataBuilder.aMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@DisplayName("LegalGuardianGroupService")
@ExtendWith(MockitoExtension.class)
class LegalGuardianGroupServiceTest {

    private static final Guardian NOVAK = guardian("11111111-1111-1111-1111-111111111111", "Novák");
    private static final Guardian SVOBODOVA = guardian("22222222-2222-2222-2222-222222222222", "Svobodová");
    private static final Guardian DVORAK = guardian("33333333-3333-3333-3333-333333333333", "Dvořák");
    private static final MemberId CHILD_A = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId CHILD_B = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final MemberId ADULT = new MemberId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    private final InMemoryLegalGuardianGroupRepository groups = new InMemoryLegalGuardianGroupRepository();

    private final GuardianContactResolver guardianContactResolver = userIds -> Stream.of(NOVAK, SVOBODOVA, DVORAK)
            .filter(g -> userIds.contains(g.userId()))
            .map(g -> new GuardianContact(g.userId(), "Jan", g.lastName(), null, null, GuardianKind.MEMBER))
            .toList();

    @Mock
    private MemberRepository memberRepository;

    private LegalGuardianGroupService service;

    @BeforeEach
    void setUp() {
        service = new LegalGuardianGroupService(groups, guardianContactResolver, memberRepository);
        lenient().when(memberRepository.findById(CHILD_A))
                .thenReturn(Optional.of(aMemberWithId(CHILD_A.uuid()).withDateOfBirth(tenYearsAgo()).build()));
        lenient().when(memberRepository.findById(CHILD_B))
                .thenReturn(Optional.of(aMemberWithId(CHILD_B.uuid()).withDateOfBirth(tenYearsAgo()).build()));
        lenient().when(memberRepository.findById(ADULT))
                .thenReturn(Optional.of(aMemberWithId(ADULT.uuid()).withDateOfBirth(LocalDate.now().minusYears(30)).build()));
    }

    private static Guardian guardian(String uuid, String lastName) {
        return new Guardian(new UserId(UUID.fromString(uuid)), lastName);
    }

    private static Set<UserId> ids(Guardian... guardians) {
        return Set.of(guardians).stream().map(Guardian::userId).collect(Collectors.toSet());
    }

    private static LocalDate tenYearsAgo() {
        return LocalDate.now().minusYears(10);
    }

    private LegalGuardianGroup groupOf(Set<Guardian> guardians, MemberId... minors) {
        LegalGuardianGroup group = LegalGuardianGroup.create(guardians, minorOf(minors[0]));
        for (int i = 1; i < minors.length; i++) {
            group.addMinor(minorOf(minors[i]));
        }
        return groups.save(group);
    }

    private static Minor minorOf(MemberId id) {
        return new Minor(id, tenYearsAgo());
    }

    private Optional<LegalGuardianGroup> groupOfMinor(MemberId minor) {
        return groups.findOne(LegalGuardianGroupFilter.all().withMinorIs(minor.toUserId()));
    }

    @Test
    @DisplayName("rejects a guardian that is neither a legal guardian nor an adult member")
    void rejectsUnknownGuardian() {
        UserId stranger = new UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

        assertThatThrownBy(() -> service.setGuardiansOf(CHILD_A, Set.of(stranger)))
                .isInstanceOf(GuardianNotFoundException.class);
    }

    @Nested
    @DisplayName("changeGroupGuardians()")
    class ChangeGroupGuardians {

        @Test
        @DisplayName("changes the guardians in place and regenerates the name")
        void changesInPlace() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK), CHILD_A, CHILD_B);

            service.changeGroupGuardians(group.getId(), ids(NOVAK, SVOBODOVA));

            LegalGuardianGroup saved = groups.findById(group.getId()).orElseThrow();
            assertThat(saved.getGuardians()).isEqualTo(ids(NOVAK, SVOBODOVA));
            assertThat(saved.getName()).isEqualTo("Novák a Svobodová");
            assertThat(saved.getMinors()).hasSize(2);
        }

        @Test
        @DisplayName("merges into the group that already has the target guardians and removes the emptied group")
        void mergesWithExistingGroup() {
            LegalGuardianGroup source = groupOf(Set.of(NOVAK), CHILD_A);
            LegalGuardianGroup existing = groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_B);

            service.changeGroupGuardians(source.getId(), ids(NOVAK, SVOBODOVA));

            assertThat(groups.findById(source.getId())).isEmpty();
            LegalGuardianGroup merged = groups.findById(existing.getId()).orElseThrow();
            assertThat(merged.getMinors()).extracting(m -> m.memberId()).containsExactlyInAnyOrder(CHILD_A, CHILD_B);
        }

        @Test
        @DisplayName("does nothing harmful when the guardians stay the same")
        void sameGuardians() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A);

            service.changeGroupGuardians(group.getId(), ids(NOVAK, SVOBODOVA));

            assertThat(groups.findById(group.getId())).isPresent();
            assertThat(groups.findAll(LegalGuardianGroupFilter.all())).hasSize(1);
        }

        @Test
        @DisplayName("rejects an empty set of guardians")
        void rejectsEmpty() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK), CHILD_A);

            assertThatThrownBy(() -> service.changeGroupGuardians(group.getId(), Set.of()))
                    .isInstanceOf(LegalGuardianGroupWithoutGuardianException.class);
        }

        @Test
        @DisplayName("fails for an unknown group")
        void unknownGroup() {
            assertThatThrownBy(() -> service.changeGroupGuardians(
                    new LegalGuardianGroupId(UUID.randomUUID()), ids(NOVAK)))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setGuardiansOf()")
    class SetGuardiansOf {

        @Test
        @DisplayName("creates a new group for a minor without a group")
        void createsNewGroup() {
            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA));

            LegalGuardianGroup group = groupOfMinor(CHILD_A).orElseThrow();
            assertThat(group.getGuardians()).isEqualTo(ids(NOVAK, SVOBODOVA));
            assertThat(group.getName()).isEqualTo("Novák a Svobodová");
        }

        @Test
        @DisplayName("sibling with the same guardians joins the existing group")
        void siblingJoinsExistingGroup() {
            groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A);

            service.setGuardiansOf(CHILD_B, ids(NOVAK, SVOBODOVA));

            assertThat(groups.findAll(LegalGuardianGroupFilter.all())).hasSize(1);
            assertThat(groupOfMinor(CHILD_B)).isEqualTo(groupOfMinor(CHILD_A));
        }

        @Test
        @DisplayName("half-sibling gets a separate group")
        void halfSiblingGetsOwnGroup() {
            groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A);

            service.setGuardiansOf(CHILD_B, ids(NOVAK));

            assertThat(groups.findAll(LegalGuardianGroupFilter.all())).hasSize(2);
            assertThat(groupOfMinor(CHILD_B).orElseThrow().getGuardians()).isEqualTo(ids(NOVAK));
        }

        @Test
        @DisplayName("changing guardians of one sibling leaves the other sibling untouched")
        void siblingUnaffected() {
            groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A, CHILD_B);

            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA, DVORAK));

            assertThat(groupOfMinor(CHILD_A).orElseThrow().getGuardians()).isEqualTo(ids(NOVAK, SVOBODOVA, DVORAK));
            assertThat(groupOfMinor(CHILD_B).orElseThrow().getGuardians()).isEqualTo(ids(NOVAK, SVOBODOVA));
        }

        @Test
        @DisplayName("changes the group in place when the minor is its only child")
        void changesInPlaceForOnlyChild() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK), CHILD_A);

            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA));

            assertThat(groups.findAll(LegalGuardianGroupFilter.all())).hasSize(1);
            assertThat(groups.findById(group.getId()).orElseThrow().getGuardians()).isEqualTo(ids(NOVAK, SVOBODOVA));
        }

        @Test
        @DisplayName("moves the minor to the existing group and removes the emptied previous group")
        void movesToExistingGroup() {
            LegalGuardianGroup previous = groupOf(Set.of(NOVAK), CHILD_A);
            LegalGuardianGroup target = groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_B);

            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA));

            assertThat(groups.findById(previous.getId())).isEmpty();
            assertThat(groups.findById(target.getId()).orElseThrow().getMinors()).hasSize(2);
        }

        @Test
        @DisplayName("keeps the previous group when other minors remain in it")
        void keepsPreviousGroupWithOtherMinors() {
            LegalGuardianGroup previous = groupOf(Set.of(NOVAK), CHILD_A, CHILD_B);
            groupOf(Set.of(NOVAK, SVOBODOVA), new MemberId(UUID.randomUUID()));

            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA));

            assertThat(groups.findById(previous.getId()).orElseThrow().getMinors()).hasSize(1);
        }

        @Test
        @DisplayName("is a no-op when the minor already has exactly these guardians")
        void noOpWhenUnchanged() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A, CHILD_B);

            service.setGuardiansOf(CHILD_A, ids(NOVAK, SVOBODOVA));

            assertThat(groups.findById(group.getId()).orElseThrow().getMinors()).hasSize(2);
        }

        @Test
        @DisplayName("rejects an empty set of guardians")
        void rejectsEmpty() {
            assertThatThrownBy(() -> service.setGuardiansOf(CHILD_A, Set.of()))
                    .isInstanceOf(LegalGuardianGroupWithoutGuardianException.class);
        }

        @Test
        @DisplayName("rejects an adult")
        void rejectsAdult() {
            assertThatThrownBy(() -> service.setGuardiansOf(ADULT, ids(NOVAK)))
                    .isInstanceOf(OnlyMinorsAllowedException.class);
        }

        @Test
        @DisplayName("fails for an unknown member")
        void unknownMember() {
            MemberId unknown = new MemberId(UUID.randomUUID());
            lenient().when(memberRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setGuardiansOf(unknown, ids(NOVAK)))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("guardiansOf()")
    class GuardiansOf {

        @Test
        @DisplayName("returns guardians of the minor's group")
        void returnsGuardians() {
            groupOf(Set.of(NOVAK, SVOBODOVA), CHILD_A);

            assertThat(service.guardiansOf(CHILD_A)).isEqualTo(ids(NOVAK, SVOBODOVA));
        }

        @Test
        @DisplayName("returns an empty set for a minor without a group")
        void emptyWithoutGroup() {
            assertThat(service.guardiansOf(CHILD_A)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getGroup() / listGroups()")
    class Queries {

        @Test
        @DisplayName("lists all groups")
        void lists() {
            groupOf(Set.of(NOVAK), CHILD_A);
            groupOf(Set.of(SVOBODOVA), CHILD_B);

            assertThat(service.listGroups()).hasSize(2);
        }

        @Test
        @DisplayName("gets a group by id")
        void gets() {
            LegalGuardianGroup group = groupOf(Set.of(NOVAK), CHILD_A);

            assertThat(service.getGroup(group.getId())).isSameAs(group);
        }

        @Test
        @DisplayName("fails for an unknown group")
        void unknown() {
            assertThatThrownBy(() -> service.getGroup(new LegalGuardianGroupId(UUID.randomUUID())))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }
}
