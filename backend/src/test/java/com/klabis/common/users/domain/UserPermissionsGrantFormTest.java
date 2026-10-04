package com.klabis.common.users.domain;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPermissions holds only authorities grantable over everything")
class UserPermissionsGrantFormTest {

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Test
    @DisplayName("an authority not grantable over everything is rejected with the authorities named")
    void shouldRejectAuthorityNotGrantableOverAll() {
        Set<Authority> grantable = EnumSet.complementOf(EnumSet.of(Authority.CALENDAR_MANAGE));

        assertThatThrownBy(() -> UserPermissions.requireGrantableOverAll(
                Set.of(Authority.MEMBERS_READ, Authority.CALENDAR_MANAGE), grantable))
                .isInstanceOf(AuthorityNotGrantableOverAllException.class)
                .hasMessageContaining("CALENDAR:MANAGE")
                .hasMessageNotContaining("MEMBERS:READ");
    }

    @Test
    @DisplayName("authorities grantable over everything are accepted")
    void shouldAcceptAuthoritiesGrantableOverAll() {
        assertThatCode(() -> UserPermissions.requireGrantableOverAll(
                Set.of(Authority.MEMBERS_MANAGE), EnumSet.of(Authority.MEMBERS_MANAGE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create, grant and replace accept every authority grantable over everything")
    void shouldAcceptAllGrantableAuthoritiesOnEveryWritePath() {
        UserPermissions permissions = UserPermissions.create(USER_ID, Authority.grantableOverAll());
        permissions.replaceAuthorities(Authority.grantableOverAll());
        Authority.grantableOverAll().forEach(permissions::grantAuthority);

        assertThat(permissions.getDirectAuthorities()).isEqualTo(Authority.grantableOverAll());
    }
}
