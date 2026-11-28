package com.klabis.members.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SelfRelationshipSource")
class SelfRelationshipSourceTest {

    @Mock
    private MemberRepository memberRepository;

    private SelfRelationshipSource source() {
        return new SelfRelationshipSource(memberRepository);
    }

    private UserId givenMemberBornOn(LocalDate dateOfBirth) {
        UUID id = UUID.randomUUID();
        when(memberRepository.findDateOfBirth(new MemberId(id))).thenReturn(Optional.of(dateOfBirth));
        return new UserId(id);
    }

    @Test
    @DisplayName("grants an adult profile editing over themself")
    void shouldGrantAdultEditProfileOverSelf() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(30));

        var grants = source().grantsOf(userId);

        assertThat(grants).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE, Authority.MEMBERS_VIEW_PROFILE,
                Authority.EVENTS_REGISTER_MEMBER, Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE);
        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(TargetRef.member(userId.uuid()));
    }

    @Test
    @DisplayName("grants a minor only profile viewing and event registering over themself")
    void shouldGrantMinorOnlyViewAndRegister() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(15));

        assertThat(source().grantsOf(userId)).containsOnlyKeys(Authority.EVENTS_REGISTER_MEMBER,
                Authority.MEMBERS_VIEW_PROFILE);
    }

    @ParameterizedTest(name = "{0}-year-old member: {1} over themself is granted = {2}")
    @CsvSource({
            "30, MEMBERS_EDIT_PROFILE, true",
            "30, MEMBERS_VIEW_PROFILE, true",
            "30, EVENTS_REGISTER_MEMBER, true",
            "30, MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, true",
            "15, MEMBERS_EDIT_PROFILE, false",
            "15, MEMBERS_VIEW_PROFILE, true",
            "15, EVENTS_REGISTER_MEMBER, true",
            "15, MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, false"
    })
    void shouldGrantMemberAuthorityOverSelfByAge(int age, Authority authority, boolean granted) {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(age));

        var grants = source().grantsOf(userId);

        if (granted) {
            assertThat(grants.get(authority)).containsExactly(TargetRef.member(userId.uuid()));
        } else {
            assertThat(grants).doesNotContainKey(authority);
        }
    }

    @Test
    @DisplayName("grants a member turning 18 today profile editing")
    void shouldGrantMemberTurningEighteenToday() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(18));

        assertThat(source().grantsOf(userId)).containsKey(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("grants no profile editing to a member turning 18 tomorrow")
    void shouldGrantNoEditToMemberTurningEighteenTomorrow() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(18).plusDays(1));

        assertThat(source().grantsOf(userId)).doesNotContainKey(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("grants a user who is not a member profile viewing and editing over themself, but no event registering")
    void shouldGrantNonMemberProfileViewAndEdit() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findDateOfBirth(new MemberId(id))).thenReturn(Optional.empty());

        var grants = source().grantsOf(new UserId(id));

        assertThat(grants).containsOnlyKeys(Authority.MEMBERS_VIEW_PROFILE, Authority.MEMBERS_EDIT_PROFILE);
        assertThat(grants.get(Authority.MEMBERS_VIEW_PROFILE)).containsExactly(TargetRef.member(id));
        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(TargetRef.member(id));
    }

    @Test
    @DisplayName("grants an adult member exactly what TestSnapshots emulates")
    void shouldMatchTestSnapshotsForAdultMember() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(30));

        assertThat(source().grantsOf(userId)).isEqualTo(TestSnapshots.selfGrants(userId.uuid(), true, false));
    }

    @Test
    @DisplayName("grants a minor member exactly what TestSnapshots emulates")
    void shouldMatchTestSnapshotsForMinorMember() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(15));

        assertThat(source().grantsOf(userId)).isEqualTo(TestSnapshots.selfGrants(userId.uuid(), true, true));
    }

    @Test
    @DisplayName("grants a non-member exactly what TestSnapshots emulates")
    void shouldMatchTestSnapshotsForNonMember() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findDateOfBirth(new MemberId(id))).thenReturn(Optional.empty());

        assertThat(source().grantsOf(new UserId(id))).isEqualTo(TestSnapshots.selfGrants(id, false, false));
    }
}
