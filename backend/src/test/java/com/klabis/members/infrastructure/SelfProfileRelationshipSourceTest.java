package com.klabis.members.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
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
@DisplayName("SelfProfileRelationshipSource")
class SelfProfileRelationshipSourceTest {

    @Mock
    private MemberRepository memberRepository;

    private SelfProfileRelationshipSource source() {
        return new SelfProfileRelationshipSource(memberRepository);
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

        assertThat(grants).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE);
        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(TargetRef.member(userId.uuid()));
    }

    @Test
    @DisplayName("grants nothing to a minor")
    void shouldGrantNothingToMinor() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(15));

        assertThat(source().grantsOf(userId)).isEmpty();
    }

    @Test
    @DisplayName("grants a member turning 18 today profile editing")
    void shouldGrantMemberTurningEighteenToday() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(18));

        assertThat(source().grantsOf(userId)).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("grants nothing to a member turning 18 tomorrow")
    void shouldGrantNothingToMemberTurningEighteenTomorrow() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(18).plusDays(1));

        assertThat(source().grantsOf(userId)).isEmpty();
    }

    @Test
    @DisplayName("grants an adult profile editing even when their membership is suspended")
    void shouldGrantSuspendedAdult() {
        UserId userId = givenMemberBornOn(LocalDate.now().minusYears(30));

        assertThat(source().grantsOf(userId)).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("grants nothing to a user who is not a member")
    void shouldGrantNothingToNonMember() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findDateOfBirth(new MemberId(id))).thenReturn(Optional.empty());

        assertThat(source().grantsOf(new UserId(id))).isEmpty();
    }
}
