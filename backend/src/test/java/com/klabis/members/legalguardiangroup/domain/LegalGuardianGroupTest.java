package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.groups.domain.MemberAlreadyInGroupException;
import com.klabis.common.groups.domain.MemberNotInGroupException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import static com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LegalGuardianGroup domain unit tests")
class LegalGuardianGroupTest {

    private static final Guardian NOVAK = guardian("11111111-1111-1111-1111-111111111111", "Novák");
    private static final Guardian SVOBODOVA = guardian("22222222-2222-2222-2222-222222222222", "Svobodová");
    private static final Guardian DVORAK = guardian("33333333-3333-3333-3333-333333333333", "Dvořák");
    private static final Minor MINOR_A = minor("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Minor MINOR_B = minor("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static Guardian guardian(String uuid, String lastName) {
        return new Guardian(new UserId(UUID.fromString(uuid)), lastName);
    }

    private static Minor minor(String uuid) {
        return new Minor(new MemberId(UUID.fromString(uuid)), LocalDate.now().minusYears(10));
    }

    private static LegalGuardianGroup groupWith(Set<Guardian> guardians, Minor... minors) {
        LegalGuardianGroup group = LegalGuardianGroup.create(guardians, minors[0]);
        for (int i = 1; i < minors.length; i++) {
            group.addMinor(minors[i]);
        }
        return group;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("guardians become owners and the minor becomes the only member")
        void guardiansAreOwnersAndMinorIsMember() {
            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(NOVAK, SVOBODOVA), MINOR_A);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getGuardians()).containsExactlyInAnyOrder(NOVAK.userId(), SVOBODOVA.userId());
            assertThat(group.getMinors()).extracting(m -> m.memberId()).containsExactly(MINOR_A.id());
            assertThat(group.hasMember(NOVAK.userId())).isFalse();
        }

        @Test
        @DisplayName("name is generated from guardians' surnames")
        void nameIsGenerated() {
            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(SVOBODOVA, NOVAK), MINOR_A);

            assertThat(group.getName()).isEqualTo("Novák a Svobodová");
        }

        @Test
        @DisplayName("rejects empty set of guardians")
        void rejectsEmptyGuardians() {
            assertThatThrownBy(() -> LegalGuardianGroup.create(Set.of(), MINOR_A))
                    .isInstanceOf(LegalGuardianGroupWithoutGuardianException.class);
        }

        @Test
        @DisplayName("rejects a guardian who is the minor of the same group")
        void rejectsGuardianBeingTheMinor() {
            Guardian sameAsMinor = new Guardian(MINOR_A.id().toUserId(), "Novák");

            assertThatThrownBy(() -> LegalGuardianGroup.create(Set.of(sameAsMinor), MINOR_A))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }
    }

    @Nested
    @DisplayName("Minor")
    class MinorValue {

        @Test
        @DisplayName("rejects a member who is 18 or older")
        void rejectsAdult() {
            MemberId adult = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> new Minor(adult, LocalDate.now().minusYears(18)))
                    .isInstanceOf(OnlyMinorsAllowedException.class);
        }

        @Test
        @DisplayName("accepts a member who turns 18 tomorrow")
        void acceptsAlmostAdult() {
            new Minor(new MemberId(UUID.randomUUID()), LocalDate.now().minusYears(18).plusDays(1));
        }
    }

    @Nested
    @DisplayName("generated name")
    class GeneratedName {

        @Test
        @DisplayName("is ordered alphabetically using Czech collation")
        void usesCzechCollation() {
            Guardian capek = guardian("44444444-4444-4444-4444-444444444444", "Čapek");

            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(DVORAK, capek), MINOR_A);

            assertThat(group.getName()).isEqualTo("Čapek a Dvořák");
        }

        @Test
        @DisplayName("does not repeat a shared surname")
        void deduplicatesSurnames() {
            Guardian otherNovak = guardian("55555555-5555-5555-5555-555555555555", "Novák");

            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(NOVAK, otherNovak), MINOR_A);

            assertThat(group.getName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("joins three surnames with 'a'")
        void joinsAll() {
            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(SVOBODOVA, NOVAK, DVORAK), MINOR_A);

            assertThat(group.getName()).isEqualTo("Dvořák a Novák a Svobodová");
        }
    }

    @Nested
    @DisplayName("changeGuardians()")
    class ChangeGuardians {

        @Test
        @DisplayName("replaces the guardians and regenerates the name")
        void replacesGuardians() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            group.changeGuardians(Set.of(SVOBODOVA, DVORAK));

            assertThat(group.getGuardians()).containsExactlyInAnyOrder(SVOBODOVA.userId(), DVORAK.userId());
            assertThat(group.getName()).isEqualTo("Dvořák a Svobodová");
        }

        @Test
        @DisplayName("keeps a guardian that stays in the set")
        void keepsRetainedGuardian() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            group.changeGuardians(Set.of(NOVAK, SVOBODOVA));

            assertThat(group.getGuardians()).containsExactlyInAnyOrder(NOVAK.userId(), SVOBODOVA.userId());
        }

        @Test
        @DisplayName("rejects empty set and leaves the group unchanged")
        void rejectsEmpty() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            assertThatThrownBy(() -> group.changeGuardians(Set.of()))
                    .isInstanceOf(LegalGuardianGroupWithoutGuardianException.class);
            assertThat(group.getGuardians()).containsExactly(NOVAK.userId());
        }

        @Test
        @DisplayName("rejects a guardian who is a minor of the group")
        void rejectsMinorAsGuardian() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);
            Guardian minorAsGuardian = new Guardian(MINOR_A.id().toUserId(), "Novák");

            assertThatThrownBy(() -> group.changeGuardians(Set.of(minorAsGuardian)))
                    .isInstanceOf(MemberAlreadyInGroupException.class);
        }
    }

    @Nested
    @DisplayName("minors")
    class Minors {

        @Test
        @DisplayName("addMinor adds another child")
        void addsMinor() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            group.addMinor(MINOR_B);

            assertThat(group.getMinors()).extracting(m -> m.memberId())
                    .containsExactlyInAnyOrder(MINOR_A.id(), MINOR_B.id());
        }

        @Test
        @DisplayName("addMinor rejects a child that is already in the group")
        void rejectsDuplicateMinor() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            assertThatThrownBy(() -> group.addMinor(MINOR_A)).isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("addMinor rejects a guardian of the group")
        void rejectsGuardianAsMinor() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);
            Minor guardianAsMinor = new Minor(new MemberId(NOVAK.userId().uuid()), LocalDate.now().minusYears(5));

            assertThatThrownBy(() -> group.addMinor(guardianAsMinor)).isInstanceOf(MemberAlreadyInGroupException.class);
        }

        @Test
        @DisplayName("removeMinor removes the child")
        void removesMinor() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A, MINOR_B);

            group.removeMinor(MINOR_A.id());

            assertThat(group.getMinors()).extracting(m -> m.memberId()).containsExactly(MINOR_B.id());
            assertThat(group.hasMinors()).isTrue();
        }

        @Test
        @DisplayName("removeMinor rejects a child that is not in the group")
        void rejectsRemovingUnknownMinor() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            assertThatThrownBy(() -> group.removeMinor(MINOR_B.id())).isInstanceOf(MemberNotInGroupException.class);
        }

        @Test
        @DisplayName("hasMinors is false after the last child is removed")
        void hasNoMinorsAfterRemovingLast() {
            LegalGuardianGroup group = groupWith(Set.of(NOVAK), MINOR_A);

            group.removeMinor(MINOR_A.id());

            assertThat(group.hasMinors()).isFalse();
        }

        @Test
        @DisplayName("takeMinorsFrom moves all children of another group")
        void takesMinorsFromOtherGroup() {
            LegalGuardianGroup target = groupWith(Set.of(NOVAK, SVOBODOVA), MINOR_A);
            LegalGuardianGroup source = groupWith(Set.of(NOVAK), MINOR_B);

            target.takeMinorsFrom(source);

            assertThat(target.getMinors()).extracting(m -> m.memberId())
                    .containsExactlyInAnyOrder(MINOR_A.id(), MINOR_B.id());
            assertThat(source.hasMinors()).isFalse();
        }
    }

    @Nested
    @DisplayName("isLastGuardian()")
    class IsLastGuardian {

        @Test
        @DisplayName("is true only for the sole guardian")
        void soleGuardian() {
            LegalGuardianGroup single = groupWith(Set.of(NOVAK), MINOR_A);
            LegalGuardianGroup pair = groupWith(Set.of(NOVAK, SVOBODOVA), MINOR_A);

            assertThat(single.isLastGuardian(NOVAK.userId())).isTrue();
            assertThat(pair.isLastGuardian(NOVAK.userId())).isFalse();
        }
    }

    @Test
    @DisplayName("type discriminator is LEGAL_GUARDIAN")
    void discriminator() {
        assertThat(LegalGuardianGroup.TYPE_DISCRIMINATOR).isEqualTo("LEGAL_GUARDIAN");
    }
}
