package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthorityToDtoConverter")
class AuthorityToDtoConverterTest {

    private static final Set<String> WIRE_NAMES = Arrays.stream(com.klabis.common.users.infrastructure.restapi.Authority.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private final AuthorityToDtoConverter converter = new AuthorityToDtoConverterImpl();

    @Test
    @DisplayName("converts every authority present in the wire schema and rejects every other one")
    void shouldCoverEveryDomainAuthority() {
        for (Authority authority : Authority.values()) {
            if (WIRE_NAMES.contains(authority.name())) {
                assertThat(converter.convert(authority).name()).isEqualTo(authority.name());
            } else {
                assertThatThrownBy(() -> converter.convert(authority)).as(authority.name())
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
