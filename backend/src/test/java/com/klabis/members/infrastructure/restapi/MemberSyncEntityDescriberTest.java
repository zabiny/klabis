package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MemberSyncEntityDescriber")
class MemberSyncEntityDescriberTest {

    private final Members members = mock(Members.class);
    private final MemberSyncEntityDescriber describer = new MemberSyncEntityDescriber(members);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a", java.util.List.of(new SimpleGrantedAuthority(Authority.MEMBERS_READ.getValue()))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("describes the member by name and registration number and links to the member")
    void describesMember() {
        UUID id = UUID.randomUUID();
        when(members.findById(new MemberId(id)))
                .thenReturn(Optional.of(new MemberDto(id, "Jan", "Novák", "jan@example.com", "ZBM9500", java.time.LocalDateTime.now())));

        var description = describer.describe(id.toString());

        assertThat(description).isPresent();
        assertThat(description.get().label()).isEqualTo("Jan Novák (ZBM9500)");
        assertThat(description.get().entityLink().getHref()).endsWith("/api/members/" + id);
    }

    @Test
    @DisplayName("is empty when the member no longer exists")
    void emptyWhenMissing() {
        when(members.findById(any())).thenReturn(Optional.empty());

        assertThat(describer.describe(UUID.randomUUID().toString())).isEmpty();
    }
}
