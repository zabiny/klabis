package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.infrastructure.restapi.DelegatedAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegatedAuthorities wire mapping")
class DelegatedAuthoritiesTest {

    @Test
    @DisplayName("the API enum offers exactly the authorities a founder may delegate")
    void apiEnumMatchesDelegatableAuthorities() {
        Set<String> wireValues = Arrays.stream(DelegatedAuthority.values())
                .map(DelegatedAuthority::getValue)
                .collect(Collectors.toSet());
        Set<String> domainValues = FreeGroup.delegatableAuthorities().stream()
                .map(Authority::getValue)
                .collect(Collectors.toSet());

        assertThat(wireValues).isEqualTo(domainValues);
    }

    @Test
    @DisplayName("administrator authorities are never delegatable")
    void administratorAuthoritiesAreNotDelegatable() {
        assertThat(FreeGroup.delegatableAuthorities())
                .doesNotContain(Authority.MEMBERS_MANAGE, Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_READ)
                .containsExactly(Authority.MEMBERS_EDIT_PROFILE);
    }

    @Test
    @DisplayName("an absent request property delegates nothing")
    void absentMeansNothingDelegated() {
        assertThat(DelegatedAuthorities.toDomain(null)).isEmpty();
    }

    @Test
    @DisplayName("maps between the API and the domain representation")
    void roundTrips() {
        Set<Authority> domain = DelegatedAuthorities.toDomain(Set.of(DelegatedAuthority.MEMBERS_EDIT_PROFILE));

        assertThat(domain).containsExactly(Authority.MEMBERS_EDIT_PROFILE);
        assertThat(DelegatedAuthorities.toWire(domain)).containsExactly(DelegatedAuthority.MEMBERS_EDIT_PROFILE);
    }
}
