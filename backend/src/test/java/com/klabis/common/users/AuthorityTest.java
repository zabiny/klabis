package com.klabis.common.users;

import com.klabis.common.authorization.GrantForm;
import com.klabis.common.authorization.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authority target type and grant forms")
class AuthorityTest {

    static Stream<Arguments> expectedMetadata() {
        return Stream.of(
                Arguments.of(Authority.MEMBERS_MANAGE, TargetType.MEMBER, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.MEMBERS_READ, TargetType.MEMBER, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.MEMBERS_PERMISSIONS, TargetType.MEMBER, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.MEMBERS_EDIT_PROFILE, TargetType.MEMBER, EnumSet.of(GrantForm.SPECIFIC)),
                Arguments.of(Authority.EVENTS_REGISTRATIONS, TargetType.MEMBER, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.EVENTS_READ, TargetType.EVENT, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.EVENTS_MANAGE, TargetType.EVENT, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.CALENDAR_MANAGE, TargetType.NONE, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.GROUPS_TRAINING, TargetType.NONE, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.FINANCE_MANAGE, TargetType.NONE, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.SYNC_MANAGE, TargetType.NONE, EnumSet.of(GrantForm.ALL)),
                Arguments.of(Authority.DEVELOPER, TargetType.NONE, EnumSet.of(GrantForm.ALL))
        );
    }

    @ParameterizedTest
    @MethodSource("expectedMetadata")
    @DisplayName("every constant declares target type and grant forms")
    void shouldDeclareTargetTypeAndGrantForms(Authority authority, TargetType targetType, EnumSet<GrantForm> forms) {
        assertThat(authority.getTargetType()).isEqualTo(targetType);
        assertThat(authority.getGrantForms()).isEqualTo(forms);
    }

    @Test
    @DisplayName("expected metadata covers every constant")
    void shouldCoverEveryConstant() {
        var covered = expectedMetadata().map(a -> (Authority) a.get()[0]).toList();

        assertThat(covered).containsExactlyInAnyOrder(Authority.values());
    }

    @Test
    @DisplayName("assignable authorities are those holdable over everything, without standard ones and DEVELOPER")
    void shouldDeriveAssignableAuthoritiesFromGrantForms() {
        var expected = EnumSet.noneOf(Authority.class);
        for (Authority authority : Authority.values()) {
            if (authority.getGrantForms().contains(GrantForm.ALL)) {
                expected.add(authority);
            }
        }
        expected.removeAll(Authority.getStandardUserAuthorities());
        expected.remove(Authority.DEVELOPER);

        assertThat(Authority.assignableAuthorities()).isEqualTo(expected);
    }

    @Test
    @DisplayName("authorities grantable over everything are exactly those holdable over everything")
    void shouldDeriveGrantableOverAllFromGrantForms() {
        for (Authority authority : Authority.values()) {
            assertThat(Authority.grantableOverAll().contains(authority))
                    .as(authority.getValue())
                    .isEqualTo(authority.getGrantForms().contains(GrantForm.ALL));
        }
    }

    @Test
    @DisplayName("profile editing is the only delegatable authority")
    void shouldHaveProfileEditingAsOnlyDelegatableAuthority() {
        assertThat(Authority.delegatable()).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("profile editing has the wire value MEMBERS:EDIT_PROFILE")
    void shouldExposeProfileEditingWireValue() {
        assertThat(Authority.MEMBERS_EDIT_PROFILE.getValue()).isEqualTo("MEMBERS:EDIT_PROFILE");
        assertThat(Authority.fromString("MEMBERS:EDIT_PROFILE")).isEqualTo(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("profile editing is neither assignable in the permissions dialog nor grantable over everything")
    void shouldNotOfferProfileEditingForDirectGrant() {
        assertThat(Authority.assignableAuthorities()).doesNotContain(Authority.MEMBERS_EDIT_PROFILE);
        assertThat(Authority.grantableOverAll()).doesNotContain(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("delegatable authorities are those holdable over specific targets")
    void shouldDeriveDelegatableFromGrantForms() {
        for (Authority authority : Authority.delegatable()) {
            assertThat(authority.getGrantForms()).contains(GrantForm.SPECIFIC);
        }
    }
}
